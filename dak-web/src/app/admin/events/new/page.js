'use client';

import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { createEvent, fetchEventById } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import EventForm, { isoToAdelaideLocal } from '@/components/EventForm';

/**
 * Creation, optionally seeded from an existing event via ?from=<id>.
 *
 * Every event found so far recurs weekly, and without this each occurrence
 * means retyping twelve fields that differ only in the date. Copying rather
 * than editing the original keeps the past occurrence intact — an edit would
 * move the event forward and lose the record that it happened.
 *
 * The start time is deliberately not carried over: it is the one field that
 * must change, and a prefilled date that looks right is the easiest kind to
 * publish by mistake.
 */
function EventCreateForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const copyFrom = searchParams.get('from');

  const [initial, setInitial] = useState(copyFrom ? null : {});

  useEffect(() => {
    if (!copyFrom) return;
    fetchEventById(copyFrom)
      .then((e) =>
        setInitial({
          title: e.title ?? '',
          description: e.description ?? '',
          // Left blank on purpose. See above.
          startsAt: '',
          endsAt: '',
          venueName: e.venueName ?? '',
          venueAddress: e.venueAddress ?? '',
          isFree: e.isFree ?? false,
          priceNote: e.priceNote ?? '',
          organiser: e.organiser ?? '',
          organiserContact: e.organiserContact ?? '',
          sourceUrl: e.sourceUrl ?? '',
          imageUrls: e.images?.length > 0 ? e.images.map((i) => i.imageUrl) : [''],
          categoryId: e.category?.id ?? '',
        })
      )
      .catch(() => setInitial({}));
  }, [copyFrom]);

  if (!initial) {
    return <div className="h-40 rounded-2xl bg-night border border-border-dark animate-pulse" />;
  }

  return (
    <EventForm
      initial={initial}
      submitLabel="Create"
      onCancel={() => router.back()}
      onSubmit={async (payload) => {
        await createEvent(payload);
        // Created as DRAFT, so the public page would 404. The queue is where
        // it gets published.
        router.push('/admin');
      }}
    />
  );
}

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

      {/* useSearchParams opts its subtree out of server rendering, so the
          boundary keeps that to the form. */}
      <Suspense fallback={null}>
        <EventCreateForm />
      </Suspense>
    </PageShell>
  );
}