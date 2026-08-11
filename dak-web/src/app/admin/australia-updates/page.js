'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  AlertTriangle, Check, Archive, Pencil, ExternalLink,
  Sparkles, ChevronDown, Undo2, RefreshCw,
} from 'lucide-react';
import {
  fetchDraftUpdates,
  fetchPublishedUpdates,
  fetchArchivedUpdates,
  updateUpdateStatus,
  updateUpdateMetadata,
  triggerRssPoll,
} from '@/api/admin';
import { fetchUpdateCategories } from '@/api/updates';
import { useAuth } from '@/context/AuthContext';
import { timeAgo } from '@/utils/date';
import PageShell from '@/components/PageShell';
import Pagination from '@/components/Pagination';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import {
  primaryBtn, secondaryBtn, selectClass, fieldClass,
  cardClass, emptyStateClass,
} from '@/components/admin/adminStyles';

// Mirrors the ck_australia_updates_scope database constraint.
const SCOPES = [
  { value: 'ADELAIDE', label: 'Adelaide' },
  { value: 'SOUTH_AUSTRALIA', label: 'South Australia' },
  { value: 'AUSTRALIA', label: 'Australia' },
  { value: 'COUNCIL_AREA', label: 'Council area' },
  { value: 'SUBURB', label: 'Suburb' },
];

/**
 * Australia Updates queue.
 *
 * Split out of the single admin page: every queue there loaded together, so
 * reading the update list meant waiting on rentals, events and guides as well,
 * and the sections buried each other.
 */
export default function AdminUpdatesPage() {
  const { user, loading: authLoading } = useAuth();

  const [updates, setUpdates] = useState([]);
  const [draftPage, setDraftPage] = useState(0);
  const [draftPages, setDraftPages] = useState(0);
  const [draftTotal, setDraftTotal] = useState(0);

  const [published, setPublished] = useState([]);
  const [publishedPage, setPublishedPage] = useState(0);
  const [publishedPages, setPublishedPages] = useState(0);
  const [publishedTotal, setPublishedTotal] = useState(0);

  // Archived items are consulted occasionally rather than worked through, so
  // the section stays collapsed and is only fetched when opened.
  const [archived, setArchived] = useState([]);
  const [archivedPage, setArchivedPage] = useState(0);
  const [archivedPages, setArchivedPages] = useState(0);
  const [archivedTotal, setArchivedTotal] = useState(0);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [archiveLoading, setArchiveLoading] = useState(false);

  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  // Id of the row currently open for editing, plus its working copy. Only one
  // row edits at a time — a review queue is worked through sequentially.
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState({ title: '', slug: '', koreanSummary: '' });
  const [saving, setSaving] = useState(false);

  // A poll runs for minutes and reports no progress, so the only honest
  // feedback is that it is still going, then that the queue reloaded.
  const [polling, setPolling] = useState(false);
  const [pollNotice, setPollNotice] = useState('');

  function cancelEditing() {
    setEditingId(null);
    setDraft({ title: '', slug: '', koreanSummary: '' });
  }

  async function loadData() {
    setLoading(true);
    try {
      const [draftData, publishedData] = await Promise.all([
        fetchDraftUpdates(draftPage),
        fetchPublishedUpdates(publishedPage),
      ]);

      setUpdates(draftData?.content ?? []);
      setDraftPages(draftData?.totalPages ?? 0);
      setDraftTotal(draftData?.totalElements ?? 0);

      setPublished(publishedData?.content ?? []);
      setPublishedPages(publishedData?.totalPages ?? 0);
      setPublishedTotal(publishedData?.totalElements ?? 0);

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load updates.');
    } finally {
      setLoading(false);
    }
  }

  // Any open edit is cancelled first: a form left open against a row that has
  // scrolled off would save against something the admin can no longer see.
  useEffect(() => {
    if (authLoading || !user) return;
    cancelEditing();
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading, draftPage, publishedPage]);

  useEffect(() => {
    // Categories populate the assignment dropdown and don't change during a
    // session, so they're fetched once rather than with every reload.
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
      slug: update.slug ?? '',
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
        slug: draft.slug,
        koreanSummary: draft.koreanSummary,
      });
      setUpdates((rows) =>
        rows.map((row) =>
          row.id !== updateId
            ? row
            : { ...row, title: draft.title, slug: draft.slug, koreanSummary: draft.koreanSummary }
        )
      );
      cancelEditing();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminGuard error={error}>
      <PageShell>
        <AdminBackButton href="/admin" label="Admin" />

        <h1 className="text-xl font-bold text-snow mb-5">Australia Updates</h1>

        {actionError && <p className="text-adelaide-red text-[13px] mb-4">{actionError}</p>}

        {loading ? (
          <div className="grid gap-2.5">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : (
          <>
            <section className="mb-8">
              {/* The poll control sits with the queue it fills, so the thing it
                  changes is directly below it. */}
              <div className="flex items-center justify-between gap-4 mb-3">
                <h2 className="text-lg font-semibold text-snow">
                  Drafts <span className="text-muted font-normal">({draftTotal})</span>
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
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing awaiting review.</p>
                </div>
              ) : (
                <>
                  <div className="grid gap-2.5">
                    {updates.map((update) => {
                      // Publishing requires a category, a source, a scope and a
                      // summary written by a person. An import arrives with none
                      // of them. The summary matters most: without it, publishing
                      // would put the publisher's own article text on the site.
                      const missing = [
                        !update.hasCategory && 'category',
                        !update.hasGeographicScope && 'scope',
                        !update.hasSource && 'source',
                        !update.koreanSummary?.trim() && 'summary',
                      ].filter(Boolean);
                      const incomplete = missing.length > 0;
                      const isEditing = editingId === update.id;

                      return (
                        <div key={update.id} className={cardClass}>
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

                              {/* Editable here and nowhere else: a draft has no
                                  links to break, and a published one has every
                                  link already shared under it — the server
                                  refuses the field once the status moves. */}
                              <div>
                                <label className="block text-[12px] font-medium text-muted mb-1.5">
                                  Slug — the address this will publish at
                                </label>
                                <input
                                  type="text"
                                  value={draft.slug}
                                  onChange={(e) => setDraft({ ...draft, slug: e.target.value })}
                                  maxLength={200}
                                  className={fieldClass}
                                />
                              </div>

                              {/* Source text, read-only. This is the publisher's
                                  own article and is never published — it is here
                                  to write the summary from. */}
                              {update.extractedText && (
                                <div>
                                  <label className="block text-[12px] font-medium text-muted mb-1.5">
                                    Source text — reference only, not published
                                  </label>
                                  <div className="max-h-48 overflow-y-auto rounded-lg border
                                                  border-border-dark bg-surface p-3 text-[13px]
                                                  text-muted leading-relaxed whitespace-pre-line">
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
                                  onChange={(e) =>
                                    setDraft({ ...draft, koreanSummary: e.target.value })
                                  }
                                  rows={8}
                                  className={`${fieldClass} resize-y leading-relaxed`}
                                />
                              </div>

                              <div className="flex gap-2">
                                {/* An empty summary saves fine — a half-written
                                    draft has to be resumable. Publication is
                                    where it is refused. */}
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
                                <span className="font-medium text-snow min-w-0">
                                  {update.title}
                                </span>
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

                              {/* Shows the admin's own summary once written, and
                                  the source text until then — either way a
                                  reviewer can see what the item is about well
                                  enough to categorise it. */}
                              {(update.koreanSummary || update.extractedText) && (
                                <p className="text-[13px] text-muted leading-relaxed
                                              whitespace-pre-line line-clamp-3 mb-3">
                                  {update.koreanSummary || update.extractedText}
                                </p>
                              )}

                              <div className="flex flex-wrap items-center gap-2">
                                <select
                                  value={update.categoryId ?? ''}
                                  onChange={(e) =>
                                    handleMetadataChange(update.id, {
                                      categoryId: e.target.value || null,
                                    })
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

                                <button
                                  onClick={() => startEditing(update)}
                                  className={secondaryBtn}
                                >
                                  <Pencil size={13} strokeWidth={2} />
                                  Write summary
                                </button>

                                {/* Needs a summary: the card is written from it. */}
                                <Link
                                  href={`/admin/australia-updates/${update.id}/card`}
                                  className={secondaryBtn}
                                >
                                  <Sparkles size={13} strokeWidth={2} />
                                  Card
                                </Link>

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
                                  <span className="flex items-center gap-1 text-[12px]
                                                   text-muted ml-auto">
                                    <Sparkles size={12} strokeWidth={2} />
                                    Machine-drafted
                                  </span>
                                )}
                              </div>

                              {incomplete && (
                                <p className="flex items-center gap-1.5 text-xs
                                              text-adelaide-red mt-2.5">
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

            {/* Editing happens on the article itself — an admin reading a live
                page is where problems are actually noticed — so this section
                carries retirement and the card only. */}
            <section className="mb-8">
              <h2 className="text-lg font-semibold text-snow mb-3">
                Published <span className="text-muted font-normal">({publishedTotal})</span>
              </h2>

              {published.length === 0 ? (
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing published yet.</p>
                </div>
              ) : (
                <>
                  <div className="grid gap-2.5">
                    {published.map((update) => (
                      <div
                        key={update.id}
                        className={`${cardClass} flex items-center justify-between gap-4`}
                      >
                        <div className="min-w-0">
                          <Link
                            href={`/australia-updates/${update.slug}`}
                            className="font-medium text-snow hover:text-white transition-colors
                                       block truncate"
                          >
                            {update.title}
                          </Link>
                          <span className="text-[12px] text-faint">
                            {timeAgo(update.publishedAt ?? update.createdAt)}
                          </span>
                        </div>
                        <div className="flex gap-2 shrink-0">
                          <Link
                            href={`/admin/australia-updates/${update.id}/card`}
                            className={secondaryBtn}
                          >
                            <Sparkles size={13} strokeWidth={2} />
                            Card
                          </Link>
                          <button
                            onClick={() => handleUpdateAction(update.id, 'ARCHIVED')}
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
                    page={publishedPage}
                    totalPages={publishedPages}
                    onChange={setPublishedPage}
                  />
                </>
              )}
            </section>

            {/* Collapsed by default: retired items are consulted occasionally,
                not worked through. Last on the page because an archive is where
                work ends rather than begins. */}
            <section>
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
                Archived
                {archiveOpen && archivedTotal > 0 && (
                  <span className="text-muted font-normal">({archivedTotal})</span>
                )}
              </button>

              {archiveOpen && (
                archiveLoading ? (
                  <div className="grid gap-2.5">
                    {[0, 1, 2].map((i) => (
                      <div
                        key={i}
                        className="h-14 rounded-xl bg-night border border-border-dark animate-pulse"
                      />
                    ))}
                  </div>
                ) : archived.length === 0 ? (
                  <div className={emptyStateClass}>
                    <p className="text-muted text-sm">Nothing archived.</p>
                  </div>
                ) : (
                  <>
                    <div className="grid gap-2.5">
                      {archived.map((update) => (
                        <div
                          key={update.id}
                          className={`${cardClass} flex items-center justify-between gap-4`}
                        >
                          <div className="min-w-0">
                            <span className="text-sm text-muted block truncate">
                              {update.title}
                            </span>
                            <span className="text-[12px] text-faint">
                              {timeAgo(update.createdAt)}
                            </span>
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
          </>
        )}
      </PageShell>
    </AdminGuard>
  );
}