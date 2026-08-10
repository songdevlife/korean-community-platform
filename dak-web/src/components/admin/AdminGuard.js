'use client';

import Link from 'next/link';
import { AlertTriangle } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

/**
 * Wraps an admin screen so it only renders for an administrator.
 *
 * The real guard is SecurityConfig's /api/v1/admin/** rule — this decides what
 * is displayed, not what is permitted. It waits for the session before
 * refusing, because rendering the refusal while the role is still unknown
 * flashes it at an admin who is in fact signed in.
 */
export default function AdminGuard({ children, error }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <PageShell>
        <div className="h-7 w-48 rounded bg-night animate-pulse mb-6" />
        <div className="grid gap-2.5">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
            />
          ))}
        </div>
      </PageShell>
    );
  }

  if (!user || error) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
          <AlertTriangle
            size={22}
            strokeWidth={1.5}
            className="text-adelaide-red mx-auto mb-2"
          />
          <p className="text-snow text-sm">{error || 'You need to be signed in.'}</p>
          <p className="text-muted text-[13px] mt-1.5">
            Check that you are logged in with an account that has admin privileges.
          </p>
          <Link
            href="/"
            className="inline-block mt-5 px-5 py-2.5 rounded-xl bg-korea-blue text-white
                       text-sm font-medium hover:bg-korea-blue/85 transition-colors"
          >
            Home
          </Link>
        </div>
      </PageShell>
    );
  }

  return <>{children}</>;
}