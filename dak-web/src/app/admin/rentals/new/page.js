'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { createRental } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import RentalForm from '@/components/RentalForm';

/**
 * Creation. No copy-from equivalent to the events screen: a rental is a
 * particular room in a particular house, and nothing about the last one
 * transfers to the next.
 */
export default function RentalCreatePage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = user?.role === 'ADMINISTRATOR';

  if (authLoading) {
    return (
      <PageShell>
        <div className="h-40 rounded-2xl bg-night border border-border-dark animate-pulse" />
      </PageShell>
    );
  }

  if (!isAdmin) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <p className="text-snow font-medium mb-1">This page is not available</p>
          <p className="text-muted text-sm mb-5">Rentals are listed by the DAK team.</p>
          <Link
            href="/rentals"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors"
          >
            Browse rentals
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <button
        onClick={() => router.back()}
        className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
      >
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span className="hidden sm:inline">Back</span>
      </button>

      <h1 className="text-2xl font-bold text-snow leading-tight mb-6">New rental</h1>

      <RentalForm
        showSlug
        submitLabel="Create"
        onCancel={() => router.back()}
        onSubmit={async (payload) => {
          await createRental(payload);
          // Created as DRAFT, so the public page would 404. The queue is
          // where it gets published, and publication is what starts the
          // twenty-one day clock.
          router.push('/admin');
        }}
      />
    </PageShell>
  );
}