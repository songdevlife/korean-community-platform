'use client';

import { useState, useEffect } from 'react';
import { fetchUsers } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import { eventDate } from '@/utils/date';
import PageShell from '@/components/PageShell';
import Pagination from '@/components/Pagination';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import { cardClass, emptyStateClass } from '@/components/admin/adminStyles';

/**
 * Accounts, read-only.
 *
 * This screen exists to remove the only routine reason to open a shell against
 * the production database — the credential in Entries 12 and 36 was pasted
 * while answering exactly the question this answers.
 *
 * Nothing here can be changed. Role changes have an endpoint but no interface,
 * and adding one to a screen whose purpose is to stop people opening a database
 * shell would be adding a second thing to get wrong.
 */
export default function AdminUsersPage() {
  const { user, loading: authLoading } = useAuth();

  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (authLoading || !user) return;

    let cancelled = false;

    async function load() {
      setLoading(true);
      try {
        const data = await fetchUsers(page);
        if (cancelled) return;

        setUsers(data?.content ?? []);
        setTotalPages(data?.totalPages ?? 0);
        setTotal(data?.totalElements ?? 0);
        setError('');
      } catch (err) {
        if (cancelled) return;
        setError(err.response?.data?.error?.message || 'Could not load accounts.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [user, authLoading, page]);

  return (
    <AdminGuard error={error}>
      <PageShell>
        <AdminBackButton href="/admin" label="Admin" />

        <h1 className="text-xl font-bold text-snow mb-1">Accounts</h1>
        <p className="text-[13px] text-muted mb-5">
          Read-only{total > 0 && ` · ${total}`}
        </p>

        {loading ? (
          <div className="grid gap-2.5">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="h-14 rounded-xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : users.length === 0 ? (
          <div className={emptyStateClass}>
            <p className="text-muted text-sm">No accounts yet.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-2.5">
              {users.map((account) => (
                <div
                  key={account.id}
                  className={`${cardClass} flex items-center justify-between gap-4`}
                >
                  <div className="min-w-0">
                    <p className="text-sm text-snow truncate">{account.email}</p>
                    <span className="text-[12px] text-faint">
                      {account.displayName || '이름 없음'} · {eventDate(account.createdAt)}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    {/* An unverified address is an attempt rather than an
                        account, and the difference is the whole reason for
                        looking. */}
                    {!account.emailVerified && (
                      <span className="text-[11px] px-2.5 py-0.5 rounded-full
                                       border border-border-dark text-muted">
                        미인증
                      </span>
                    )}
                    {account.role !== 'USER' && (
                      <span className="text-[11px] px-2.5 py-0.5 rounded-full
                                       bg-korea-blue/15 text-korea-blue font-medium">
                        {account.role}
                      </span>
                    )}
                    {account.accountStatus !== 'ACTIVE' && (
                      <span className="text-[11px] px-2.5 py-0.5 rounded-full
                                       border border-border-dark text-muted">
                        {account.accountStatus}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <Pagination page={page} totalPages={totalPages} onChange={setPage} />
          </>
        )}
      </PageShell>
    </AdminGuard>
  );
}