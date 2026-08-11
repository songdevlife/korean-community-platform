'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Check, Archive, ChevronDown, Plus, Sparkles } from 'lucide-react';
import {
  fetchDraftGuides,
  fetchPublishedGuides,
  fetchArchivedGuides,
  updateGuideStatus,
} from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import { timeAgo } from '@/utils/date';
import PageShell from '@/components/PageShell';
import Pagination from '@/components/Pagination';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import {
  primaryBtn, secondaryBtn, cardClass, emptyStateClass,
} from '@/components/admin/adminStyles';

/**
 * Guides queue.
 *
 * Draft and published sit together: the volume is far lower than updates, and
 * a guide moves straight from writing to publishing with no review queue in
 * between.
 */
export default function AdminGuidesPage() {
  const { user, loading: authLoading } = useAuth();

  const [draftGuides, setDraftGuides] = useState([]);
  const [draftPage, setDraftPage] = useState(0);
  const [draftPages, setDraftPages] = useState(0);
  const [draftTotal, setDraftTotal] = useState(0);

  const [publishedGuides, setPublishedGuides] = useState([]);
  const [publishedPage, setPublishedPage] = useState(0);
  const [publishedPages, setPublishedPages] = useState(0);
  const [publishedTotal, setPublishedTotal] = useState(0);

  // Guides accumulate rather than expire, so an always-open archive would grow
  // to dominate the page. Loaded on first open.
  const [archivedGuides, setArchivedGuides] = useState([]);
  const [archivedPage, setArchivedPage] = useState(0);
  const [archivedPages, setArchivedPages] = useState(0);
  const [archivedTotal, setArchivedTotal] = useState(0);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [archiveLoading, setArchiveLoading] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  async function loadData() {
    setLoading(true);
    try {
      const [draftData, publishedData] = await Promise.all([
        fetchDraftGuides(draftPage),
        fetchPublishedGuides(publishedPage),
      ]);

      setDraftGuides(draftData?.content ?? []);
      setDraftPages(draftData?.totalPages ?? 0);
      setDraftTotal(draftData?.totalElements ?? 0);

      setPublishedGuides(publishedData?.content ?? []);
      setPublishedPages(publishedData?.totalPages ?? 0);
      setPublishedTotal(publishedData?.totalElements ?? 0);

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load guides.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (authLoading || !user) return;
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading, draftPage, publishedPage]);

  async function loadArchived(page) {
    setArchiveLoading(true);
    try {
      const data = await fetchArchivedGuides(page);
      setArchivedGuides(data?.content ?? []);
      setArchivedPages(data?.totalPages ?? 0);
      setArchivedTotal(data?.totalElements ?? 0);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not load archived guides.');
    } finally {
      setArchiveLoading(false);
    }
  }

  async function toggleArchive() {
    const opening = !archiveOpen;
    setArchiveOpen(opening);

    if (opening && archivedGuides.length === 0) {
      await loadArchived(archivedPage);
    }
  }

  function changeArchivedPage(page) {
    setArchivedPage(page);
    loadArchived(page);
  }

  async function handleGuideAction(guideId, status) {
    setActionError('');
    try {
      await updateGuideStatus(guideId, status);
      loadData();

      // loadData refetches drafts and published only, so anything already
      // loaded into the archive is now stale in both directions — a guide just
      // archived is missing from it, and one just restored is still in it.
      setArchivedGuides([]);
      setArchivedPage(0);
      setArchiveOpen(false);
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  return (
    <AdminGuard error={error}>
      <PageShell>
        <AdminBackButton href="/admin" label="Admin" />

        <div className="flex items-center justify-between gap-4 mb-5">
          <h1 className="text-xl font-bold text-snow">Guides</h1>
          <Link href="/admin/guides/new" className={`${secondaryBtn} shrink-0`}>
            <Plus size={14} strokeWidth={2} />
            New guide
          </Link>
        </div>

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
            {draftTotal > 0 && (
              <section className="mb-8">
                <h2 className="text-lg font-semibold text-snow mb-3">
                  Drafts <span className="text-muted font-normal">({draftTotal})</span>
                </h2>

                <div className="grid gap-2.5">
                  {draftGuides.map((guide) => (
                    <div
                      key={guide.id}
                      className={`${cardClass} flex items-center justify-between gap-4`}
                    >
                      <div className="min-w-0">
                        <span className="font-medium text-snow block truncate">{guide.title}</span>
                        <span className="text-[12px] text-faint">
                          {guide.slug || 'No slug — set one before publishing'}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        {/* Needs body content: the card is written from it. */}
                        <Link
                          href={`/admin/guides/${guide.id}/card`}
                          className={secondaryBtn}
                        >
                          <Sparkles size={13} strokeWidth={2} />
                          Card
                        </Link>
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

                <Pagination page={draftPage} totalPages={draftPages} onChange={setDraftPage} />
              </section>
            )}

            <section className="mb-8">
              <h2 className="text-lg font-semibold text-snow mb-3">
                Published <span className="text-muted font-normal">({publishedTotal})</span>
              </h2>

              {publishedGuides.length === 0 ? (
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing published yet.</p>
                </div>
              ) : (
                <>
                  <div className="grid gap-2.5">
                    {publishedGuides.map((guide) => (
                      <div
                        key={guide.id}
                        className={`${cardClass} flex items-center justify-between gap-4`}
                      >
                        <div className="min-w-0">
                          <Link
                            href={`/guides/${guide.slug}`}
                            className="font-medium text-snow hover:text-white transition-colors
                                       block truncate"
                          >
                            {guide.title}
                          </Link>
                          <span className="text-[12px] text-faint">
                            {timeAgo(guide.publishedAt ?? guide.createdAt)}
                          </span>
                        </div>
                        <div className="flex gap-2 shrink-0">
                          <Link
                            href={`/admin/guides/${guide.id}/card`}
                            className={secondaryBtn}
                          >
                            <Sparkles size={13} strokeWidth={2} />
                            Card
                          </Link>
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
                    page={publishedPage}
                    totalPages={publishedPages}
                    onChange={setPublishedPage}
                  />
                </>
              )}
            </section>

            {/* Without this an archived guide left the admin screen entirely:
                absent from both lists, the only route back was the database,
                and Archive was in practice a delete that left the row behind. */}
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
                        className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
                      />
                    ))}
                  </div>
                ) : archivedGuides.length === 0 ? (
                  <div className={emptyStateClass}>
                    <p className="text-muted text-sm">Nothing archived.</p>
                  </div>
                ) : (
                  <>
                    <div className="grid gap-2.5">
                      {archivedGuides.map((guide) => (
                        <div
                          key={guide.id}
                          className={`${cardClass} flex items-center justify-between gap-4 opacity-60`}
                        >
                          <div className="min-w-0">
                            <p className="font-medium text-snow truncate">{guide.title}</p>
                            <span className="text-[12px] text-faint truncate block">
                              {guide.slug}
                            </span>
                          </div>
                          {/* Restore rather than Publish: this puts a guide back
                              where it was, and Publish reads as sending
                              something out for the first time. */}
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