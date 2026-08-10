'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Plus } from 'lucide-react';
import { fetchRentalsByStatus, updateRental, extendRental } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import { secondaryBtn, cardClass, emptyStateClass } from '@/components/admin/adminStyles';

/** Whole days until a listing lapses. Negative values never reach this — the
    row shows "만료" instead. */
function daysLeft(expiresAt) {
  const ms = new Date(expiresAt) - new Date();
  return Math.max(0, Math.ceil(ms / 86400000));
}

/**
 * Rentals queue.
 *
 * Same shape as events, with one addition: a listing lapses on its own after
 * three weeks, so the queue shows how long each has left and offers an
 * extension rather than only a publish and an archive. An expired listing is
 * the one thing here that goes wrong by itself.
 */
export default function AdminRentalsPage() {
  const { user, loading: authLoading } = useAuth();

  const [draftRentals, setDraftRentals] = useState([]);
  const [draftTotal, setDraftTotal] = useState(0);
  const [publishedRentals, setPublishedRentals] = useState([]);
  const [publishedTotal, setPublishedTotal] = useState(0);
  const [archivedRentals, setArchivedRentals] = useState([]);
  const [archivedTotal, setArchivedTotal] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  async function loadData() {
    setLoading(true);
    try {
      const [draftData, publishedData, archivedData] = await Promise.all([
        fetchRentalsByStatus('DRAFT'),
        fetchRentalsByStatus('PUBLISHED'),
        fetchRentalsByStatus('ARCHIVED'),
      ]);

      setDraftRentals(draftData?.content ?? []);
      setDraftTotal(draftData?.totalElements ?? 0);
      setPublishedRentals(publishedData?.content ?? []);
      setPublishedTotal(publishedData?.totalElements ?? 0);
      setArchivedRentals(archivedData?.content ?? []);
      setArchivedTotal(archivedData?.totalElements ?? 0);

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load rentals.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (authLoading || !user) return;
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading]);

  // Publishing is what starts the twenty-one day clock, which the server sets
  // rather than the form — a draft that sat for a week still gets its full
  // period once it goes up.
  async function handleRentalAction(rentalId, status) {
    setActionError('');
    try {
      await updateRental(rentalId, { status });
      loadData();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  // Another twenty-one days, for an advertiser who replied to say they are
  // still looking. Separate from an edit because nothing about the listing
  // changed.
  async function handleExtend(rentalId) {
    setActionError('');
    try {
      await extendRental(rentalId);
      loadData();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'Could not extend that listing.');
    }
  }

  return (
    <AdminGuard error={error}>
      <PageShell>
        <AdminBackButton href="/admin" label="Admin" />

        <div className="flex items-center justify-between gap-4 mb-5">
          <h1 className="text-xl font-bold text-snow">Rentals</h1>
          <Link href="/admin/rentals/new" className={`${secondaryBtn} shrink-0`}>
            <Plus size={14} strokeWidth={2} />
            New rental
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
                  {draftRentals.map((rental) => (
                    <div
                      key={rental.id}
                      className={`${cardClass} flex items-center justify-between gap-4`}
                    >
                      <div className="min-w-0">
                        <p className="font-medium text-snow truncate">{rental.title}</p>
                        <span className="text-[12px] text-faint">
                          {rental.suburb} · ${rental.rentMin}/주 · {rental.consentStatus}
                          {rental.hasContact && ' · 연락처'}
                          {rental.imageCount > 0 && ` · 사진 ${rental.imageCount}`}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <Link href={`/admin/rentals/${rental.id}/edit`} className={secondaryBtn}>
                          Edit
                        </Link>
                        <button
                          onClick={() => handleRentalAction(rental.id, 'PUBLISHED')}
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

              {publishedRentals.length === 0 ? (
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing published yet.</p>
                </div>
              ) : (
                <div className="grid gap-2.5">
                  {publishedRentals.map((rental) => (
                    <div
                      key={rental.id}
                      className={`${cardClass} flex items-center justify-between gap-4 ${
                        rental.hasExpired ? 'opacity-60' : ''
                      }`}
                    >
                      <div className="min-w-0">
                        <Link
                          href={`/rentals/${rental.slug}`}
                          className="font-medium text-snow hover:text-white transition-colors
                                     block truncate"
                        >
                          {rental.title}
                        </Link>
                        <span className="text-[12px] text-faint">
                          {rental.suburb} · ${rental.rentMin}/주
                          {rental.consentStatus !== 'FULL' && ' · 외부'}
                          {rental.hasExpired
                            ? ' · 게시 기간 만료'
                            : rental.expiresAt && ` · ${daysLeft(rental.expiresAt)}일 남음`}
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <button onClick={() => handleExtend(rental.id)} className={secondaryBtn}>
                          +21일
                        </button>
                        <Link href={`/admin/rentals/${rental.id}/edit`} className={secondaryBtn}>
                          Edit
                        </Link>
                        <button
                          onClick={() => handleRentalAction(rental.id, 'ARCHIVED')}
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

            {archivedTotal > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-snow mb-3">
                  Archived <span className="text-muted font-normal">({archivedTotal})</span>
                </h2>

                <div className="grid gap-2.5">
                  {archivedRentals.map((rental) => (
                    <div
                      key={rental.id}
                      className={`${cardClass} flex items-center justify-between gap-4 opacity-60`}
                    >
                      <div className="min-w-0">
                        <p className="font-medium text-snow truncate">{rental.title}</p>
                        <span className="text-[12px] text-faint">
                          {rental.suburb} · ${rental.rentMin}/주
                        </span>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <Link href={`/admin/rentals/${rental.id}/edit`} className={secondaryBtn}>
                          Edit
                        </Link>
                        {/* Restore grants a fresh period as well as
                            republishing: a listing brought back has usually
                            been re-advertised. */}
                        <button
                          onClick={() =>
                            handleExtend(rental.id).then(() =>
                              handleRentalAction(rental.id, 'PUBLISHED')
                            )
                          }
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