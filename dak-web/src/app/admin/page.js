'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft, AlertTriangle, Check, X, Archive,
  Pencil, ExternalLink, Sparkles, ChevronDown, Undo2, Plus, RefreshCw,
} from 'lucide-react';
import {
  fetchPendingBusinesses,
  updateBusinessStatus,
  fetchDraftUpdates,
  fetchPublishedUpdates,
  fetchArchivedUpdates,
  updateUpdateStatus,
  updateUpdateMetadata,
  triggerRssPoll,
  fetchDraftGuides,
  fetchPublishedGuides,
  fetchArchivedGuides,
  fetchUsers,
  updateGuideStatus,
  fetchEventsByStatus,
  updateEvent,
} from '@/api/admin';
import { fetchUpdateCategories } from '@/api/updates';
import { useAuth } from '@/context/AuthContext';
import { timeAgo, eventDateTime, eventDate } from '@/utils/date';
import PageShell from '@/components/PageShell';
import Pagination from '@/components/Pagination';

// Mirrors the ck_australia_updates_scope database constraint.
const SCOPES = [
  { value: 'ADELAIDE', label: 'Adelaide' },
  { value: 'SOUTH_AUSTRALIA', label: 'South Australia' },
  { value: 'AUSTRALIA', label: 'Australia' },
  { value: 'COUNCIL_AREA', label: 'Council area' },
  { value: 'SUBURB', label: 'Suburb' },
];

/**
 * Admin review queues. A client component in full — every list here is
 * authenticated, and the page is not something a crawler should reach.
 *
 * The server-side guard is SecurityConfig's /api/v1/admin/** rule; this only
 * controls what is displayed.
 */
export default function AdminPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();

  // Each queue paginates independently: an admin working through drafts should
  // not have their position reset because a different section changed.
  const [businesses, setBusinesses] = useState([]);
  const [businessPage, setBusinessPage] = useState(0);
  const [businessPages, setBusinessPages] = useState(0);
  const [businessTotal, setBusinessTotal] = useState(0);

  const [updates, setUpdates] = useState([]);
  const [draftPage, setDraftPage] = useState(0);
  const [draftPages, setDraftPages] = useState(0);
  const [draftTotal, setDraftTotal] = useState(0);

  const [published, setPublished] = useState([]);
  const [publishedPage, setPublishedPage] = useState(0);
  const [publishedPages, setPublishedPages] = useState(0);
  const [publishedTotal, setPublishedTotal] = useState(0);

  // Archived items are rarely needed, so the section stays collapsed and its
  // data is only fetched when opened.
  const [archived, setArchived] = useState([]);
  const [archivedPage, setArchivedPage] = useState(0);
  const [archivedPages, setArchivedPages] = useState(0);
  const [archivedTotal, setArchivedTotal] = useState(0);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [archiveLoading, setArchiveLoading] = useState(false);

  // Guides run the same draft → published lifecycle as updates, but without
  // the completeness gate: a guide is written by hand, so there is nothing
  // missing to check for before publishing.
  const [draftGuides, setDraftGuides] = useState([]);
  const [draftGuidePage, setDraftGuidePage] = useState(0);
  const [draftGuidePages, setDraftGuidePages] = useState(0);
  const [draftGuideTotal, setDraftGuideTotal] = useState(0);

  const [publishedGuides, setPublishedGuides] = useState([]);
  const [publishedGuidePage, setPublishedGuidePage] = useState(0);
  const [publishedGuidePages, setPublishedGuidePages] = useState(0);
  const [publishedGuideTotal, setPublishedGuideTotal] = useState(0);

  // Collapsed and loaded on first open, as the update archive is. Guides
  // accumulate rather than expire, so an always-open archive would grow to
  // dominate the page. Prefixed because archiveOpen and its neighbours above
  // belong to updates — the names do not say so, and a second set that also
  // did not would be worse.
  const [archivedGuides, setArchivedGuides] = useState([]);
  const [archivedGuidePage, setArchivedGuidePage] = useState(0);
  const [archivedGuidePages, setArchivedGuidePages] = useState(0);
  const [archivedGuideTotal, setArchivedGuideTotal] = useState(0);
  const [guideArchiveOpen, setGuideArchiveOpen] = useState(false);
  const [guideArchiveLoading, setGuideArchiveLoading] = useState(false);

  // Events run the guide lifecycle rather than the update one: written by
  // hand, published when finished, no review queue in between. What they add
  // is a date that expires, which makes this the only place a past event can
  // be seen - the public listing has already dropped it by then.
  const [draftEvents, setDraftEvents] = useState([]);
  const [draftEventTotal, setDraftEventTotal] = useState(0);
  const [publishedEvents, setPublishedEvents] = useState([]);
  const [publishedEventTotal, setPublishedEventTotal] = useState(0);
  const [archivedEvents, setArchivedEvents] = useState([]);
  const [archivedEventTotal, setArchivedEventTotal] = useState(0);

  // Read-only, collapsed, loaded on first open. This section exists to remove
  // the only routine reason to open a shell against the production database —
  // the credential in Entries 12 and 36 was pasted while answering exactly the
  // question this answers.
  const [users, setUsers] = useState([]);
  const [userPage, setUserPage] = useState(0);
  const [userPages, setUserPages] = useState(0);
  const [userTotal, setUserTotal] = useState(0);
  const [usersOpen, setUsersOpen] = useState(false);
  const [usersLoading, setUsersLoading] = useState(false);

  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  // Id of the row currently open for editing, plus its working copy. Only one
  // row edits at a time — a review queue is worked through sequentially.
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState({ title: '', koreanSummary: '' });
  const [saving, setSaving] = useState(false);

  // A poll runs for minutes and reports no progress, so the only honest
  // feedback is that it is still going, then that the queue reloaded.
  const [polling, setPolling] = useState(false);
  const [pollNotice, setPollNotice] = useState('');

  function cancelEditing() {
    setEditingId(null);
    setDraft({ title: '', koreanSummary: '' });
  }

  async function loadData() {
    setLoading(true);
    try {
      const [businessesData, updatesData, publishedData, draftGuideData, publishedGuideData,
        draftEventData, publishedEventData, archivedEventData] =
   await Promise.all([
     fetchPendingBusinesses(businessPage),
     fetchDraftUpdates(draftPage),
     fetchPublishedUpdates(publishedPage),
     fetchDraftGuides(draftGuidePage),
     fetchPublishedGuides(publishedGuidePage),
     fetchEventsByStatus('DRAFT'),
     fetchEventsByStatus('PUBLISHED'),
     fetchEventsByStatus('ARCHIVED'),
   ]);
      setBusinesses(businessesData?.content ?? []);
      setBusinessPages(businessesData?.totalPages ?? 0);
      setBusinessTotal(businessesData?.totalElements ?? 0);

      setUpdates(updatesData?.content ?? []);
      setDraftPages(updatesData?.totalPages ?? 0);
      setDraftTotal(updatesData?.totalElements ?? 0);

      setPublished(publishedData?.content ?? []);
      setPublishedPages(publishedData?.totalPages ?? 0);
      setPublishedTotal(publishedData?.totalElements ?? 0);

      setDraftGuides(draftGuideData?.content ?? []);
      setDraftGuidePages(draftGuideData?.totalPages ?? 0);
      setDraftGuideTotal(draftGuideData?.totalElements ?? 0);

      setPublishedGuides(publishedGuideData?.content ?? []);
      setPublishedGuidePages(publishedGuideData?.totalPages ?? 0);
      setPublishedGuideTotal(publishedGuideData?.totalElements ?? 0);

      setDraftEvents(draftEventData?.content ?? []);
      setDraftEventTotal(draftEventData?.totalElements ?? 0);
      setPublishedEvents(publishedEventData?.content ?? []);
      setPublishedEventTotal(publishedEventData?.totalElements ?? 0);
      setArchivedEvents(archivedEventData?.content ?? []);
      setArchivedEventTotal(archivedEventData?.totalElements ?? 0);

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load admin data.');
    } finally {
      setLoading(false);
    }
  }

  // Refetch whenever any queue changes page. Any open edit is cancelled first:
  // a form left open against a row that has scrolled off would save against
  // something the admin can no longer see.
  useEffect(() => {
    if (authLoading || !user) return;
    cancelEditing();
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading, businessPage, draftPage, publishedPage, draftGuidePage, publishedGuidePage]);

  useEffect(() => {
    // Categories populate the assignment dropdown and don't change during
    // a session, so they're fetched once rather than with every reload.
    if (authLoading || !user) return;
    fetchUpdateCategories()
      .then((data) => setCategories(data ?? []))
      .catch((err) => console.error('Failed to load update categories:', err));
  }, [user, authLoading]);

  async function loadArchived(page) {
    setArchiveLoading(true);
    try {
      const data = await fetchArchivedUpdates(page);
      setArchived(data?.content ?? []);
      setArchivedPages(data?.totalPages ?? 0);
      setArchivedTotal(data?.totalElements ?? 0);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not load archived updates.');
    } finally {
      setArchiveLoading(false);
    }
  }

  async function toggleArchive() {
    const opening = !archiveOpen;
    setArchiveOpen(opening);

    // Fetch on first open only; reopening reuses what was loaded.
    if (opening && archived.length === 0) {
      await loadArchived(archivedPage);
    }
  }

  function changeArchivedPage(page) {
    setArchivedPage(page);
    loadArchived(page);
  }

  async function loadArchivedGuides(page) {
    setGuideArchiveLoading(true);
    try {
      const data = await fetchArchivedGuides(page);
      setArchivedGuides(data?.content ?? []);
      setArchivedGuidePages(data?.totalPages ?? 0);
      setArchivedGuideTotal(data?.totalElements ?? 0);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not load archived guides.');
    } finally {
      setGuideArchiveLoading(false);
    }
  }

  async function toggleGuideArchive() {
    const opening = !guideArchiveOpen;
    setGuideArchiveOpen(opening);

    // Fetch on first open only; reopening reuses what was loaded.
    if (opening && archivedGuides.length === 0) {
      await loadArchivedGuides(archivedGuidePage);
    }
  }

  function changeArchivedGuidePage(page) {
    setArchivedGuidePage(page);
    loadArchivedGuides(page);
  }

  async function loadUsers(page) {
    setUsersLoading(true);
    try {
      const data = await fetchUsers(page);
      setUsers(data?.content ?? []);
      setUserPages(data?.totalPages ?? 0);
      setUserTotal(data?.totalElements ?? 0);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not load users.');
    } finally {
      setUsersLoading(false);
    }
  }

  async function toggleUsers() {
    const opening = !usersOpen;
    setUsersOpen(opening);

    if (opening && users.length === 0) {
      await loadUsers(userPage);
    }
  }

  function changeUserPage(page) {
    setUserPage(page);
    loadUsers(page);
  }
  // Reloads the draft queue from page one afterwards: new imports are the
  // newest rows, so an admin sitting on page three would see nothing change.
  // The archive is invalidated too, because rejected articles are archived
  // rather than discarded.
  async function handlePoll() {
    if (polling) return;

    setActionError('');
    setPollNotice('');
    setPolling(true);

    try {
      await triggerRssPoll();

      setArchived([]);
      setArchivedPage(0);
      setArchiveOpen(false);

      if (draftPage !== 0) {
        setDraftPage(0);
      } else {
        await loadData();
      }

      setPollNotice('Poll finished. Anything new is at the top of the draft queue.');
    } catch (err) {
      setActionError(
        err.response?.data?.error?.message ||
          'The poll failed or timed out. It may still be running on the server, so reload this page in a few minutes before running it again.'
      );
    } finally {
      setPolling(false);
    }
  }

  async function handleBusinessAction(businessId, status) {
    setActionError('');
    try {
      await updateBusinessStatus(businessId, status);
      loadData();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  async function handleUpdateAction(updateId, status) {
    setActionError('');
    try {
      await updateUpdateStatus(updateId, status);
      loadData();
      // An item moving into or out of the archive invalidates what was loaded
      // there; drop it so the next open refetches.
      setArchived([]);
      setArchivedPage(0);
      setArchiveOpen(false);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  async function handleGuideAction(guideId, status) {
    setActionError('');
    try {
      await updateGuideStatus(guideId, status);
      loadData();

      // loadData refetches drafts and published only, so anything already
      // loaded into the archive is now stale in both directions — a guide
      // just archived is missing from it, and one just restored is still in
      // it. Dropping the cache makes the next open refetch, as the update
      // archive does for the same reason.
      setArchivedGuides([]);
      setArchivedGuidePage(0);
      setGuideArchiveOpen(false);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  // Status travels through the general edit endpoint rather than one of its
  // own: publishing an event is usually the last edit rather than a separate
  // act, and a category is required for it, which the server refuses without.
  async function handleEventAction(eventId, status) {
    setActionError('');
    try {
      await updateEvent(eventId, { status });
      loadData();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  async function restoreUpdate(updateId) {
    setActionError('');
    try {
      await updateUpdateStatus(updateId, 'DRAFT');
      // Remove locally rather than refetching the archive, so the admin's
      // position in a long list is preserved.
      setArchived((rows) => rows.filter((r) => r.id !== updateId));
      setArchivedTotal((n) => Math.max(0, n - 1));
      loadData();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not restore that update.');
    }
  }

  async function handleMetadataChange(updateId, changes) {
    setActionError('');

    // Update the row in place rather than refetching, so the other dropdown
    // doesn't reset while the admin is still filling the form.
    setUpdates((rows) =>
      rows.map((row) =>
        row.id !== updateId
          ? row
          : {
              ...row,
              ...(changes.categoryId !== undefined && {
                categoryId: changes.categoryId,
                hasCategory: Boolean(changes.categoryId),
              }),
              ...(changes.geographicScope !== undefined && {
                geographicScope: changes.geographicScope,
                hasGeographicScope: Boolean(changes.geographicScope),
              }),
            }
      )
    );

    try {
      await updateUpdateMetadata(updateId, changes);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not save that change.');
      loadData(); // Reload to discard the optimistic edit.
    }
  }

  function startEditing(update) {
    setEditingId(update.id);
    setDraft({
      title: update.title ?? '',
      koreanSummary: update.koreanSummary ?? '',
    });
    setActionError('');
  }

  async function saveEdits(updateId) {
    setActionError('');
    setSaving(true);
    try {
      await updateUpdateMetadata(updateId, {
        title: draft.title,
        koreanSummary: draft.koreanSummary,
      });
      setUpdates((rows) =>
        rows.map((row) =>
          row.id !== updateId
            ? row
            : { ...row, title: draft.title, koreanSummary: draft.koreanSummary }
        )
      );
      cancelEditing();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setSaving(false);
    }
  }

  const primaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium ' +
    'bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-korea-blue';

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg ' +
    'border border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:text-muted ' +
    'disabled:hover:border-border-dark';

  const selectClass =
    'text-[13px] px-2.5 py-1.5 rounded-lg bg-surface border border-border-dark ' +
    'text-snow outline-none focus:border-faint transition-colors [color-scheme:dark]';

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-surface border border-border-dark text-snow ' +
    'text-[14px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  const backButton = (
    <button
      onClick={() => router.back()}
      aria-label="Back"
      className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
    >
      <ArrowLeft size={18} strokeWidth={1.75} />
      <span className="hidden sm:inline">Back</span>
    </button>
  );

  if (authLoading || loading) {
    return (
      <PageShell>
        {backButton}
        <div className="h-7 w-48 rounded bg-night animate-pulse mb-6" />
        <div className="grid gap-2.5">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-16 rounded-xl bg-night border border-border-dark animate-pulse" />
          ))}
        </div>
      </PageShell>
    );
  }

  if (!user || error) {
    return (
      <PageShell>
        {backButton}
        <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
          <AlertTriangle size={22} strokeWidth={1.5} className="text-adelaide-red mx-auto mb-2" />
          <p className="text-snow text-sm">{error || 'You need to be signed in.'}</p>
          <p className="text-muted text-[13px] mt-1.5">
            Check that you are logged in with an account that has admin privileges.
          </p>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      {backButton}

      <h1 className="text-xl font-bold text-snow mb-5">Admin</h1>

      {actionError && <p className="text-adelaide-red text-[13px] mb-4">{actionError}</p>}

      <section className="mb-8">
                <h2 className="text-lg font-semibold text-snow mb-3">
          Pending businesses <span className="text-muted font-normal">({businessTotal})</span>
        </h2>

        {businesses.length === 0 ? (

          <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
            <p className="text-muted text-sm">Nothing awaiting review.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-2.5">
              {businesses.map((business) => (
                <div
                  key={business.id}
                  className="rounded-xl border border-border-dark bg-night p-3.5
                             flex items-center justify-between gap-4"
                >
                  <span className="font-medium text-snow truncate min-w-0">{business.name}</span>
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => handleBusinessAction(business.id, 'PUBLISHED')}
                      className={primaryBtn}
                    >
                      <Check size={14} strokeWidth={2.5} />
                      Approve
                    </button>
                    <button
                      onClick={() => handleBusinessAction(business.id, 'REJECTED')}
                      className={secondaryBtn}
                    >
                      <X size={14} strokeWidth={2.5} />
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <Pagination page={businessPage} totalPages={businessPages} onChange={setBusinessPage} />
          </>
        )}
      </section>

      <section className="mb-8">
{/* The poll control sits with the queue it fills, so the thing it
            changes is directly below it. */}
        <div className="flex items-center justify-between gap-4 mb-3">
          <h2 className="text-lg font-semibold text-snow">
            Draft updates <span className="text-muted font-normal">({draftTotal})</span>
          </h2>
          <button
            onClick={handlePoll}
            disabled={polling}
            className={`${secondaryBtn} shrink-0`}
          >
            <RefreshCw
              size={14}
              strokeWidth={2}
              className={polling ? 'animate-spin' : undefined}
            />
            {polling ? 'Polling...' : 'Poll feeds'}
          </button>
        </div>

        {polling && (
          <p className="text-[13px] text-muted mb-3">
            Reading the feeds and drafting summaries. This takes a few minutes.
            Leave the page open and do not press the button again.
          </p>
        )}

        {!polling && pollNotice && (
          <p className="text-[13px] text-muted mb-3">{pollNotice}</p>
        )}

        {updates.length === 0 ? (
          <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
            <p className="text-muted text-sm">Nothing awaiting review.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-2.5">
              {updates.map((update) => {
                // Publishing requires a category, a source, a scope and a
                // summary written by a person. An import arrives with none of
                // them. The summary matters most: without it, publishing would
                // put the publisher's own article text on the site.
                const missing = [
                  !update.hasCategory && 'category',
                  !update.hasGeographicScope && 'scope',
                  !update.hasSource && 'source',
                  !update.koreanSummary?.trim() && 'summary',
                ].filter(Boolean);
                const incomplete = missing.length > 0;
                const isEditing = editingId === update.id;

                return (
                  <div key={update.id} className="rounded-xl border border-border-dark bg-night p-3.5">
                    {isEditing ? (
                      <div className="flex flex-col gap-3">
                        <div>
                          <label className="block text-[12px] font-medium text-muted mb-1.5">
                            Title
                          </label>
                          <input
                            type="text"
                            value={draft.title}
                            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                            maxLength={300}
                            className={fieldClass}
                          />
                        </div>

                        {/* Source text, read-only. This is the publisher's own
                            article and is never published — it is here to write
                            the summary from. Kept in a scroll box so a long
                            article does not bury the field below it. */}
                        {update.extractedText && (
                          <div>
                            <label className="block text-[12px] font-medium text-muted mb-1.5">
                              Source text — reference only, not published
                            </label>
                            <div className="max-h-48 overflow-y-auto rounded-lg border border-border-dark
                                            bg-surface p-3 text-[13px] text-muted leading-relaxed
                                            whitespace-pre-line">
                              {update.extractedText}
                            </div>
                          </div>
                        )}

                        <div>
                        <label className="block text-[12px] font-medium text-muted mb-1.5">
                            Korean summary — review the draft; do not paste the source
                          </label>
                          <textarea
                            value={draft.koreanSummary}
                            onChange={(e) => setDraft({ ...draft, koreanSummary: e.target.value })}
                            rows={8}
                            className={`${fieldClass} resize-y leading-relaxed`}
                          />
                        </div>

                        <div className="flex gap-2">
                          {/* An empty summary saves fine — a half-written draft
                              has to be resumable. Publication is where it is
                              refused. */}
                          <button
                            onClick={() => saveEdits(update.id)}
                            disabled={saving || !draft.title.trim()}
                            className={primaryBtn}
                          >
                            <Check size={14} strokeWidth={2.5} />
                            {saving ? 'Saving…' : 'Save'}
                          </button>
                          <button onClick={cancelEditing} className={secondaryBtn}>
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div className="flex items-start justify-between gap-4 mb-2">
                          <span className="font-medium text-snow min-w-0">{update.title}</span>
                          <div className="flex gap-2 shrink-0">
                            <button
                              onClick={() => handleUpdateAction(update.id, 'PUBLISHED')}
                              disabled={incomplete}
                              className={primaryBtn}
                            >
                              <Check size={14} strokeWidth={2.5} />
                              Publish
                            </button>
                            <button
                              onClick={() => handleUpdateAction(update.id, 'ARCHIVED')}
                              className={secondaryBtn}
                            >
                              <Archive size={14} strokeWidth={2} />
                              Archive
                            </button>
                          </div>
                        </div>

                        {/* Shows the admin's own summary once written, and the
                            source text until then — either way a reviewer can
                            see what the item is about well enough to
                            categorise it. */}
                        {(update.koreanSummary || update.extractedText) && (
                          <p className="text-[13px] text-muted leading-relaxed whitespace-pre-line
                                        line-clamp-3 mb-3">
                            {update.koreanSummary || update.extractedText}
                          </p>
                        )}

                        <div className="flex flex-wrap items-center gap-2">
                          <select
                            value={update.categoryId ?? ''}
                            onChange={(e) =>
                              handleMetadataChange(update.id, { categoryId: e.target.value || null })
                            }
                            aria-label="Category"
                            className={selectClass}
                          >
                            <option value="">Category —</option>
                            {categories.map((category) => (
                              <option key={category.id} value={category.id}>
                                {category.name}
                              </option>
                            ))}
                          </select>

                          <select
                            value={update.geographicScope ?? ''}
                            onChange={(e) =>
                              handleMetadataChange(update.id, {
                                geographicScope: e.target.value || null,
                              })
                            }
                            aria-label="Geographic scope"
                            className={selectClass}
                          >
                            <option value="">Scope —</option>
                            {SCOPES.map((scope) => (
                              <option key={scope.value} value={scope.value}>
                                {scope.label}
                              </option>
                            ))}
                          </select>

                          <button onClick={() => startEditing(update)} className={secondaryBtn}>
                            <Pencil size={13} strokeWidth={2} />
                            Write summary
                          </button>

                          {update.sourceUrl && (
                            <a
                              href={update.sourceUrl}
                              target="_blank"
                              rel="noreferrer"
                              className={secondaryBtn}
                            >
                              <ExternalLink size={13} strokeWidth={2} />
                              Source
                            </a>
                          )}

                          {update.aiGenerated && (
                            <span className="flex items-center gap-1 text-[12px] text-muted ml-auto">
                              <Sparkles size={12} strokeWidth={2} />
                              Machine-drafted
                            </span>
                          )}
                        </div>

                        {incomplete && (
                          <p className="flex items-center gap-1.5 text-xs text-adelaide-red mt-2.5">
                            <AlertTriangle size={13} strokeWidth={2} className="shrink-0" />
                            Missing {missing.join(', ')} — required before publishing.
                          </p>
                        )}
                      </>
                    )}
                  </div>
                );
              })}
            </div>

            <Pagination page={draftPage} totalPages={draftPages} onChange={setDraftPage} />
          </>
        )}
      </section>

      {/* Published updates. Editing happens on the article itself — an admin
          reading a live page is where problems are actually noticed — so this
          section carries retirement only. */}
      <section>
        <h2 className="text-lg font-semibold text-snow mb-3">
          Published updates <span className="text-muted font-normal">({publishedTotal})</span>
        </h2>

        {published.length === 0 ? (
          <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
            <p className="text-muted text-sm">Nothing published yet.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-2.5">
              {published.map((update) => (
                <div
                  key={update.id}
                  className="rounded-xl border border-border-dark bg-night p-3.5
                             flex items-center justify-between gap-4"
                >
                  <div className="min-w-0">
                    <Link
                      href={`/australia-updates/${update.id}`}
                      className="font-medium text-snow hover:text-white transition-colors block truncate"
                    >
                      {update.title}
                    </Link>
                    <span className="text-[12px] text-faint">{timeAgo(update.createdAt)}</span>
                  </div>
                  <button
                    onClick={() => handleUpdateAction(update.id, 'ARCHIVED')}
                    className={`${secondaryBtn} shrink-0`}
                  >
                    <Archive size={14} strokeWidth={2} />
                    Archive
                  </button>
                </div>
              ))}
            </div>

            <Pagination page={publishedPage} totalPages={publishedPages} onChange={setPublishedPage} />
          </>
        )}
      </section>

      {/* Guides. Draft and published in one section: the volume is far lower
          than updates, and a guide moves straight from writing to publishing
          with no review queue in between. */}
      <section className="mt-8">
        <div className="flex items-center justify-between gap-4 mb-3">
          <h2 className="text-lg font-semibold text-snow">
            Guides{' '}
            <span className="text-muted font-normal">
              ({draftGuideTotal + publishedGuideTotal})
            </span>
          </h2>
          <Link href="/admin/guides/new" className={`${secondaryBtn} shrink-0`}>
            <Plus size={14} strokeWidth={2} />
            New guide
          </Link>
        </div>

        {draftGuideTotal > 0 && (
          <>
            <h3 className="text-[13px] font-medium text-muted mb-2">
              Drafts ({draftGuideTotal})
            </h3>
            <div className="grid gap-2.5 mb-4">
              {draftGuides.map((guide) => (
                <div
                  key={guide.id}
                  className="rounded-xl border border-border-dark bg-night p-3.5
                             flex items-center justify-between gap-4"
                >
                  <div className="min-w-0">
                    <span className="font-medium text-snow block truncate">{guide.title}</span>
                    <span className="text-[12px] text-faint">
                      {guide.slug || 'No slug — set one before publishing'}
                    </span>
                  </div>
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => handleGuideAction(guide.id, 'PUBLISHED')}
                      className={primaryBtn}
                    >
                      <Check size={14} strokeWidth={2.5} />
                      Publish
                    </button>
                    <button
                      onClick={() => handleGuideAction(guide.id, 'ARCHIVED')}
                      className={secondaryBtn}
                    >
                      <Archive size={14} strokeWidth={2} />
                      Archive
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <Pagination
              page={draftGuidePage}
              totalPages={draftGuidePages}
              onChange={setDraftGuidePage}
            />
          </>
        )}

        <h3 className="text-[13px] font-medium text-muted mb-2">
          Published ({publishedGuideTotal})
        </h3>

        {publishedGuides.length === 0 ? (
          <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
            <p className="text-muted text-sm">Nothing published yet.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-2.5">
              {publishedGuides.map((guide) => (
                <div
                  key={guide.id}
                  className="rounded-xl border border-border-dark bg-night p-3.5
                             flex items-center justify-between gap-4"
                >
                  <div className="min-w-0">
                    <Link
                      href={`/guides/${guide.slug}`}
                      className="font-medium text-snow hover:text-white transition-colors block truncate"
                    >
                      {guide.title}
                    </Link>
                    <span className="text-[12px] text-faint">
                      {timeAgo(guide.publishedAt ?? guide.createdAt)}
                    </span>
                  </div>
                  <button
                    onClick={() => handleGuideAction(guide.id, 'ARCHIVED')}
                    className={`${secondaryBtn} shrink-0`}
                  >
                    <Archive size={14} strokeWidth={2} />
                    Archive
                  </button>
                </div>
              ))}
            </div>

            <Pagination
              page={publishedGuidePage}
              totalPages={publishedGuidePages}
              onChange={setPublishedGuidePage}
            />
          </>
        )}

        {/* Without this an archived guide left the admin screen entirely: it
            was absent from both lists, the only route back was the database,
            and Archive was in practice a delete that left the row behind.
            Found when a guide saved twice ended up archived and could not be
            located from the interface. */}
        <button
          type="button"
          onClick={toggleGuideArchive}
          aria-expanded={guideArchiveOpen}
          className="flex items-center gap-2 text-[13px] font-medium text-muted
                     hover:text-snow transition-colors mt-6 mb-2"
        >
          <ChevronDown
            size={14}
            strokeWidth={2}
            className={`transition-transform duration-200 ${guideArchiveOpen ? '' : '-rotate-90'}`}
          />
          Archived
          {guideArchiveOpen && archivedGuideTotal > 0 && (
            <span className="font-normal">({archivedGuideTotal})</span>
          )}
        </button>

        {guideArchiveOpen && (
          guideArchiveLoading ? (
            <div className="grid gap-2.5">
              {[0, 1, 2].map((i) => (
                <div
                  key={i}
                  className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
                />
              ))}
            </div>
          ) : archivedGuides.length === 0 ? (
            <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
              <p className="text-muted text-sm">Nothing archived.</p>
            </div>
          ) : (
            <>
              <div className="grid gap-2.5">
                {archivedGuides.map((guide) => (
                  <div
                    key={guide.id}
                    className="rounded-xl border border-border-dark bg-night p-3.5
                               flex items-center justify-between gap-4 opacity-60"
                  >
                    <div className="min-w-0">
                      <p className="font-medium text-snow truncate">{guide.title}</p>
                      <span className="text-[12px] text-faint truncate block">{guide.slug}</span>
                    </div>
                    {/* Restore rather than Publish: this puts a guide back
                        where it was, and Publish reads as sending something
                        out for the first time. */}
                    <button
                      onClick={() => handleGuideAction(guide.id, 'PUBLISHED')}
                      className={`${secondaryBtn} shrink-0`}
                    >
                      Restore
                    </button>
                  </div>
                ))}
              </div>

              <Pagination
                page={archivedGuidePage}
                totalPages={archivedGuidePages}
                onChange={changeArchivedGuidePage}
              />
            </>
          )
        )}
      </section>

{/* Events. Draft and published together, like guides - low volume, no
    review queue. The published list here does not hide what has passed,
    unlike the public one: an event that has finished is exactly what
    needs archiving, so it has to be visible to be dealt with. */}
<section className="mt-8">
  <div className="flex items-center justify-between gap-4 mb-3">
    <h2 className="text-lg font-semibold text-snow">
      Events{' '}
      <span className="text-muted font-normal">
      ({draftEventTotal + publishedEventTotal + archivedEventTotal})
      </span>
    </h2>
    <Link href="/admin/events/new" className={`${secondaryBtn} shrink-0`}>
      <Plus size={14} strokeWidth={2} />
      New event
    </Link>
  </div>

  {draftEventTotal > 0 && (
    <>
      <h3 className="text-[13px] font-medium text-muted mb-2">
        Drafts ({draftEventTotal})
      </h3>
      <div className="grid gap-2.5 mb-4">
        {draftEvents.map((event) => (
          <div
            key={event.id}
            className="rounded-xl border border-border-dark bg-night p-3.5
                       flex items-center justify-between gap-4"
          >
            <div className="min-w-0">
              <p className="font-medium text-snow truncate">{event.title}</p>
              <span className="text-[12px] text-faint">
                {eventDateTime(event.startsAt)}
                {/* Named here because publishing is refused without one,
                    and the refusal arrives as an error rather than as a
                    disabled button. */}
                {!event.category && ' · 분류 없음'}
              </span>
            </div>
            <div className="flex gap-2 shrink-0">
                    <Link href={`/admin/events/new?from=${event.id}`} className={secondaryBtn}>
                      Copy
                    </Link>
                    <Link href={`/admin/events/${event.id}/edit`} className={secondaryBtn}>
                      Edit
                    </Link>
                    <button
                      onClick={() => handleEventAction(event.id, 'PUBLISHED')}
                      className={secondaryBtn}
                    >
                      Publish
                    </button>
                  </div>
          </div>
        ))}
      </div>
    </>
  )}

  <h3 className="text-[13px] font-medium text-muted mb-2">
    Published ({publishedEventTotal})
  </h3>

  {publishedEvents.length === 0 ? (
    <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
      <p className="text-muted text-sm">Nothing published yet.</p>
    </div>
  ) : (
    <div className="grid gap-2.5">
      {publishedEvents.map((event) => (
        <div
          key={event.id}
          className="rounded-xl border border-border-dark bg-night p-3.5
                     flex items-center justify-between gap-4"
        >
          <div className="min-w-0">
          <Link
              href={`/events/${event.slug}`}
              className="font-medium text-snow hover:text-white transition-colors block truncate"
            >
              {event.title}
            </Link>
            <span className="text-[12px] text-faint">
              {eventDateTime(event.startsAt)}
              {/* The public list drops these silently, so without this
                  the section would look full while showing nothing. */}
              {event.hasPassed && ' · 종료됨'}
            </span>
          </div>
          <div className="flex gap-2 shrink-0">
                  {/* Next occurrence of a weekly event, without retyping it.
                      Copies everything but the date. */}
                  <Link href={`/admin/events/new?from=${event.id}`} className={secondaryBtn}>
                    Copy
                  </Link>
                  <Link href={`/admin/events/${event.id}/edit`} className={secondaryBtn}>
                    Edit
                  </Link>
                  <button
                    onClick={() => handleEventAction(event.id, 'ARCHIVED')}
                    className={secondaryBtn}
                  >
                    Archive
                  </button>
                </div>
                </div>
      ))}
    </div>
  )}

  {/* Only where there is something in it. Without this section an archived
      event vanished from the admin screen entirely — the row disappeared,
      the only route back was the database, and Archive was in practice a
      hide button that could not be undone. */}
  {archivedEventTotal > 0 && (
    <>
      <h3 className="text-[13px] font-medium text-muted mb-2 mt-4">
        Archived ({archivedEventTotal})
      </h3>
      <div className="grid gap-2.5">
        {archivedEvents.map((event) => (
          <div
            key={event.id}
            className="rounded-xl border border-border-dark bg-night p-3.5
                       flex items-center justify-between gap-4 opacity-60"
          >
            <div className="min-w-0">
              <p className="font-medium text-snow truncate">{event.title}</p>
              <span className="text-[12px] text-faint">
                {eventDateTime(event.startsAt)}
              </span>
            </div>
            <div className="flex gap-2 shrink-0">
              {/* Copy belongs here more than anywhere else: a weekly class is
                  archived once it has run, and the next occurrence is made
                  from the one that just finished. */}
              <Link href={`/admin/events/new?from=${event.id}`} className={secondaryBtn}>
                Copy
              </Link>
              <Link href={`/admin/events/${event.id}/edit`} className={secondaryBtn}>
                Edit
              </Link>
              {/* Restore rather than Publish: this puts a listing back where
                  it was, and Publish reads as sending something out for the
                  first time. */}
              <button
                onClick={() => handleEventAction(event.id, 'PUBLISHED')}
                className={secondaryBtn}
              >
                Restore
              </button>
            </div>
          </div>
        ))}
      </div>
      </>
  )}
</section>

      {/* Archived updates. Collapsed by default: retired items are consulted
          occasionally, not worked through, and there are enough of them to
          bury the queues above. Last on the page because an archive is where
          work ends rather than begins — the same order the sections above
          keep internally, drafts before published before archived. */}
      <section className="mt-8">
        <button
          type="button"
          onClick={toggleArchive}
          aria-expanded={archiveOpen}
          className="flex items-center gap-2 text-lg font-semibold text-snow
                     hover:text-white transition-colors mb-3"
        >
          <ChevronDown
            size={18}
            strokeWidth={2}
            className={`transition-transform duration-200 ${archiveOpen ? '' : '-rotate-90'}`}
          />
          Archived updates
          {archiveOpen && archivedTotal > 0 && (
            <span className="text-muted font-normal">({archivedTotal})</span>
          )}
        </button>

        {archiveOpen && (
          archiveLoading ? (
            <div className="grid gap-2.5">
              {[0, 1, 2].map((i) => (
                <div key={i} className="h-14 rounded-xl bg-night border border-border-dark animate-pulse" />
              ))}
            </div>
          ) : archived.length === 0 ? (
            <div className="rounded-xl border border-border-dark bg-night p-6 text-center">
              <p className="text-muted text-sm">Nothing archived.</p>
            </div>
          ) : (
            <>
              <div className="grid gap-2.5">
                {archived.map((update) => (
                  <div
                    key={update.id}
                    className="rounded-xl border border-border-dark bg-night p-3.5
                               flex items-center justify-between gap-4"
                  >
                    <div className="min-w-0">
                      <span className="text-sm text-muted block truncate">{update.title}</span>
                      <span className="text-[12px] text-faint">{timeAgo(update.createdAt)}</span>
                    </div>
                    <button
                      onClick={() => restoreUpdate(update.id)}
                      className={`${secondaryBtn} shrink-0`}
                    >
                      <Undo2 size={14} strokeWidth={2} />
                      Restore
                    </button>
                  </div>
                ))}
              </div>

              <Pagination
                page={archivedPage}
                totalPages={archivedPages}
                onChange={changeArchivedPage}
              />
            </>
          )
        )}
      </section>
</PageShell>
);
}