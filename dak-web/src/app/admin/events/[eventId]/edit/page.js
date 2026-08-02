'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useParams } from 'next/navigation';
import { ArrowLeft, Trash2 } from 'lucide-react';
import { fetchEventById, updateEvent, deleteEvent } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import EventForm, { isoToAdelaideLocal } from '@/components/EventForm';

export default function EventEditPage() {
  const router = useRouter();
  const { eventId } = useParams();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = user?.role === 'ADMINISTRATOR';

  const [initial, setInitial] = useState(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    if (!isAdmin) return;
    fetchEventById(eventId)
      .then((e) =>
        setInitial({
          title: e.title ?? '',
          description: e.description ?? '',
          startsAt: isoToAdelaideLocal(e.startsAt),
          endsAt: isoToAdelaideLocal(e.endsAt),
          venueName: e.venueName ?? '',
          venueAddress: e.venueAddress ?? '',
          isFree: e.isFree ?? false,
          priceNote: e.priceNote ?? '',
          organiser: e.organiser ?? '',
          organiserContact: e.organiserContact ?? '',
          sourceUrl: e.sourceUrl ?? '',
          categoryId: e.category?.id ?? '',
        })
      )
      .catch(() => setLoadError('Could not load this event.'));
  }, [eventId, isAdmin]);

  async function handleDelete() {
    // Deletion rather than archiving is offered here because most edits that
    // end in removal are corrections - a duplicate, or a date typed wrong.
    // Archiving stays available from the queue for anything worth keeping.
    if (!window.confirm('이 행사를 삭제할까요? 되돌릴 수 없습니다.')) return;
    await deleteEvent(eventId);
    router.push('/admin');
  }

  if (authLoading || (isAdmin && !initial && !loadError)) {
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
          <Link href="/events" className="text-korea-blue text-sm hover:underline">
            Browse events
          </Link>
        </div>
      </PageShell>
    );
  }

  if (loadError) {
    return (
      <PageShell>
        <p className="text-adelaide-red text-[14px]">{loadError}</p>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <div className="flex items-center justify-between gap-4 mb-5">
        <button
          onClick={() => router.back()}
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">Back</span>
        </button>
        <button
          onClick={handleDelete}
          className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border
                     border-border-dark text-muted hover:text-adelaide-red hover:border-adelaide-red
                     transition-colors"
        >
          <Trash2 size={14} strokeWidth={1.75} />
          Delete
        </button>
      </div>

      <h1 className="text-2xl font-bold text-snow leading-tight mb-6">Edit event</h1>

      <EventForm
        initial={initial}
        submitLabel="Save"
        onCancel={() => router.back()}
        onSubmit={async (payload) => {
          await updateEvent(eventId, payload);
          router.push('/admin');
        }}
      />
    </PageShell>
  );
}