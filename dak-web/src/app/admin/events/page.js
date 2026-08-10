'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Plus } from 'lucide-react';
import { fetchEventsByStatus, updateEvent } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import { eventDateTime } from '@/utils/date';
import PageShell from '@/components/PageShell';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import { secondaryBtn, cardClass, emptyStateClass } from '@/components/admin/adminStyles';

/**
 * Events queue.
 *
 * Runs the guide lifecycle rather than the update one: written by hand,
 * published when finished, no review queue in between. What it adds is a date
 * that expires, which makes this the only place a past event can be seen — the
 * public listing has already dropped it by then.
 */
export default function AdminEventsPage() {
  const { user, loading: authLoading } = useAuth();

  const [draftEvents, setDraftEvents] = useState([]);
  const [draftTotal, setDraftTotal] = useState(0);
  const [publishedEvents, setPublishedEvents] = useState([]);
  const [publishedTotal, setPublishedTotal] = useState(0);
  const [archivedEvents, setArchivedEvents] = useState([]);
  const [archivedTotal, setArchivedTotal] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  async function loadData() {
    setLoading(true);
    try {
      const [draftData, publishedData, archivedData] = await Promise.all([
        fetchEventsByStatus('DRAFT'),
        fetchEventsByStatus('PUBLISHED'),
        fetchEventsByStatus('ARCHIVED'),
      ]);

      setDraftEvents(draftData?.content ?? []);
      setDraftTotal(draftData?.totalElements ?? 0);
      setPublishedEvents(publishedData?.content ?? []);
      setPublishedTotal(publishedData?.totalElements ?? 0);
      setArchivedEvents(archivedData?.content ?? []);
      setArchivedTotal(archivedData?.totalElements ?? 0);

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load events.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (authLoading || !user) return;
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading]);

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

  return (
    <AdminGuard error={error}>
      <PageShell>
        <AdminBackButton href="/admin" label="Admin" />

        <div className="flex items-center justify-between gap-4 mb-5">
          <h1 className="text-xl font-bold text-snow">Events</h1>
          <Link href="/admin/events/new" className={`${secondaryBtn} shrink-0`}>
            <Plus size={14} strokeWidth={2} />
            New event
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
                  {draftEvents.map((event) => (
                    <div
                      key={event.id}
                      className={`${cardClass} flex items-center justify-between gap-4`}
                    >
                      <div className="min-w-0">
                        <p className="font-medium text-snow truncate">{event.title}</p>
                        <span className="text-[12px] text-faint">
                          {eventDateTime(event.startsAt)}
                          {/* Named here because publishing is refused without
                              one, and the refusal arrives as an error rather
                              than as a disabled button. */}
                          {!event.category && ' · 분류 없음'}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <Link
                          href={`/admin/events/new?from=${event.id}`}
                          className={secondaryBtn}
                        >
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
              </section>
            )}

            <section className="mb-8">
              <h2 className="text-lg font-semibold text-snow mb-3">
                Published <span className="text-muted font-normal">({publishedTotal})</span>
              </h2>

              {publishedEvents.length === 0 ? (
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing published yet.</p>
                </div>
              ) : (
                <div className="grid gap-2.5">
                  {publishedEvents.map((event) => (
                    <div
                      key={event.id}
                      className={`${cardClass} flex items-center justify-between gap-4`}
                    >
                      <div className="min-w-0">
                        <Link
                          href={`/events/${event.slug}`}
                          className="font-medium text-snow hover:text-white transition-colors
                                     block truncate"
                        >
                          {event.title}
                        </Link>
                        <span className="text-[12px] text-faint">
                          {eventDateTime(event.startsAt)}
                          {/* The public list drops these silently, so without
                              this the section would look full while showing
                              nothing. */}
                          {event.hasPassed && ' · 종료됨'}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        {/* Next occurrence of a weekly event, without retyping
                            it. Copies everything but the date. */}
                        <Link
                          href={`/admin/events/new?from=${event.id}`}
                          className={secondaryBtn}
                        >
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
            </section>

            {/* Only where there is something in it. Without this section an
                archived event vanished from the admin screen entirely — the row
                disappeared, the only route back was the database, and Archive
                was in practice a hide button that could not be undone. */}
            {archivedTotal > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-snow mb-3">
                  Archived <span className="text-muted font-normal">({archivedTotal})</span>
                </h2>

                <div className="grid gap-2.5">
                  {archivedEvents.map((event) => (
                    <div
                      key={event.id}
                      className={`${cardClass} flex items-center justify-between gap-4 opacity-60`}
                    >
                      <div className="min-w-0">
                        <p className="font-medium text-snow truncate">{event.title}</p>
                        <span className="text-[12px] text-faint">
                          {eventDateTime(event.startsAt)}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        {/* Copy belongs here more than anywhere else: a weekly
                            class is archived once it has run, and the next
                            occurrence is made from the one that just
                            finished. */}
                        <Link
                          href={`/admin/events/new?from=${event.id}`}
                          className={secondaryBtn}
                        >
                          Copy
                        </Link>
                        <Link href={`/admin/events/${event.id}/edit`} className={secondaryBtn}>
                          Edit
                        </Link>
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
              </section>
            )}
          </>
        )}
      </PageShell>
    </AdminGuard>
  );
}