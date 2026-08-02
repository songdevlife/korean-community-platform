'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { createEvent } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import EventForm from '@/components/EventForm';

export default function EventCreatePage() {
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
          <p className="text-muted text-sm mb-5">Events are listed by the DAK team.</p>
          <Link
            href="/events"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors"
          >
            Browse events
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

      <h1 className="text-2xl font-bold text-snow leading-tight mb-6">New event</h1>

      <EventForm
        submitLabel="Create"
        onCancel={() => router.back()}
        onSubmit={async (payload) => {
          await createEvent(payload);
          // Created as DRAFT, so the public page would 404. The queue is where
          // it gets a category and a publish.
          router.push('/admin');
        }}
      />
    </PageShell>
  );
}