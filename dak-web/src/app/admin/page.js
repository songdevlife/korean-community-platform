'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Check, X, ChevronRight, Newspaper, BookOpen,
  CalendarDays, Home, Users,
} from 'lucide-react';
import {
  fetchPendingBusinesses,
  updateBusinessStatus,
  fetchDraftUpdates,
  fetchPublishedUpdates,
  fetchDraftGuides,
  fetchPublishedGuides,
  fetchEventsByStatus,
  fetchRentalsByStatus,
  fetchUsers,
} from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import Pagination from '@/components/Pagination';
import AdminGuard from '@/components/admin/AdminGuard';
import {
  primaryBtn, secondaryBtn, cardClass, emptyStateClass,
} from '@/components/admin/adminStyles';

/**
 * Admin dashboard.
 *
 * Each content type has its own screen; this only says how much is waiting in
 * each and links to it. Everything used to live on this page, which meant
 * reading the update queue required loading rentals, events and guides as
 * well, and the sections buried one another.
 *
 * Businesses stay here rather than getting a screen of their own: the queue is
 * usually empty, and the whole interaction is two buttons.
 */
export default function AdminPage() {
  const { user, loading: authLoading } = useAuth();

  const [counts, setCounts] = useState(null);

  const [businesses, setBusinesses] = useState([]);
  const [businessPage, setBusinessPage] = useState(0);
  const [businessPages, setBusinessPages] = useState(0);
  const [businessTotal, setBusinessTotal] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  async function loadBusinesses() {
    const data = await fetchPendingBusinesses(businessPage);
    setBusinesses(data?.content ?? []);
    setBusinessPages(data?.totalPages ?? 0);
    setBusinessTotal(data?.totalElements ?? 0);
  }

  async function loadData() {
    setLoading(true);
    try {
      // Only the totals are wanted from most of these, but the queue endpoints
      // have no count-only form. The first page is small enough that asking for
      // it and reading totalElements costs little.
      const [
        draftUpdates, publishedUpdates,
        draftGuides, publishedGuides,
        draftEvents, publishedEvents,
        draftRentals, publishedRentals,
        users,
      ] = await Promise.all([
        fetchDraftUpdates(0),
        fetchPublishedUpdates(0),
        fetchDraftGuides(0),
        fetchPublishedGuides(0),
        fetchEventsByStatus('DRAFT'),
        fetchEventsByStatus('PUBLISHED'),
        fetchRentalsByStatus('DRAFT'),
        fetchRentalsByStatus('PUBLISHED'),
        fetchUsers(0),
      ]);

      setCounts({
        updates: {
          draft: draftUpdates?.totalElements ?? 0,
          published: publishedUpdates?.totalElements ?? 0,
        },
        guides: {
          draft: draftGuides?.totalElements ?? 0,
          published: publishedGuides?.totalElements ?? 0,
        },
        events: {
          draft: draftEvents?.totalElements ?? 0,
          published: publishedEvents?.totalElements ?? 0,
        },
        rentals: {
          draft: draftRentals?.totalElements ?? 0,
          published: publishedRentals?.totalElements ?? 0,
        },
        users: users?.totalElements ?? 0,
      });

      await loadBusinesses();

      setError('');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not load admin data.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (authLoading || !user) return;
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, authLoading, businessPage]);

  async function handleBusinessAction(businessId, status) {
    setActionError('');
    try {
      await updateBusinessStatus(businessId, status);
      await loadBusinesses();
    } catch (err) {
      setActionError(err.response?.data?.error?.message || 'That action failed.');
    }
  }

  const sections = counts
    ? [
        {
          href: '/admin/australia-updates',
          label: 'Australia Updates',
          icon: Newspaper,
          ...counts.updates,
        },
        {
          href: '/admin/guides',
          label: 'Guides',
          icon: BookOpen,
          ...counts.guides,
        },
        {
          href: '/admin/events',
          label: 'Events',
          icon: CalendarDays,
          ...counts.events,
        },
        {
          href: '/admin/rentals',
          label: 'Rentals',
          icon: Home,
          ...counts.rentals,
        },
      ]
    : [];

  return (
    <AdminGuard error={error}>
      <PageShell>
        <h1 className="text-xl font-bold text-snow mb-5">Admin</h1>

        {actionError && <p className="text-adelaide-red text-[13px] mb-4">{actionError}</p>}

        {loading ? (
          <div className="grid gap-2.5">
            {[0, 1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : (
          <>
            <div className="grid gap-2.5 mb-8">
              {sections.map((section) => {
                const Icon = section.icon;

                return (
                  <Link
                    key={section.href}
                    href={section.href}
                    className={`${cardClass} flex items-center justify-between gap-4
                                hover:border-faint transition-colors`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <Icon size={18} strokeWidth={1.75} className="text-muted shrink-0" />
                      <div className="min-w-0">
                        <p className="font-medium text-snow">{section.label}</p>
                        <span className="text-[12px] text-faint">
                          {/* Drafts are what needs attention, so they are named
                              even at zero; published is context. */}
                          드래프트 {section.draft} · 발행 {section.published}
                        </span>
                      </div>
                    </div>
                    <ChevronRight
                      size={16}
                      strokeWidth={2}
                      className="text-faint shrink-0"
                    />
                  </Link>
                );
              })}

              <Link
                href="/admin/users"
                className={`${cardClass} flex items-center justify-between gap-4
                            hover:border-faint transition-colors`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <Users size={18} strokeWidth={1.75} className="text-muted shrink-0" />
                  <div className="min-w-0">
                    <p className="font-medium text-snow">Accounts</p>
                    <span className="text-[12px] text-faint">
                      {counts.users}
                    </span>
                  </div>
                </div>
                <ChevronRight size={16} strokeWidth={2} className="text-faint shrink-0" />
              </Link>
            </div>

            <section>
              <h2 className="text-lg font-semibold text-snow mb-3">
                Pending businesses{' '}
                <span className="text-muted font-normal">({businessTotal})</span>
              </h2>

              {businesses.length === 0 ? (
                <div className={emptyStateClass}>
                  <p className="text-muted text-sm">Nothing awaiting review.</p>
                </div>
              ) : (
                <>
                  <div className="grid gap-2.5">
                    {businesses.map((business) => (
                      <div
                        key={business.id}
                        className={`${cardClass} flex items-center justify-between gap-4`}
                      >
                        <span className="font-medium text-snow truncate min-w-0">
                          {business.name}
                        </span>
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

                  <Pagination
                    page={businessPage}
                    totalPages={businessPages}
                    onChange={setBusinessPage}
                  />
                </>
              )}
            </section>
          </>
        )}
      </PageShell>
    </AdminGuard>
  );
}