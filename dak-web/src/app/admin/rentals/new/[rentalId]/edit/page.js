'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { fetchRental, updateRental } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';
import RentalForm from '@/components/RentalForm';

/**
 * Editing. The slug field is absent, as it is on the event edit screen and
 * for the same reason: a published address is what every shared link points
 * at, and the server refuses to change it.
 */
export default function RentalEditPage() {
  const router = useRouter();
  const { rentalId } = useParams();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = user?.role === 'ADMINISTRATOR';

  const [initial, setInitial] = useState(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    if (!isAdmin || !rentalId) return;
    fetchRental(rentalId)
      .then((r) =>
        setInitial({
          title: r.title ?? '',
          description: r.description ?? '',
          suburb: r.suburb ?? '',
          listingType: r.listingType ?? 'SHARE_ROOM',
          roomTypes: r.roomTypes ? r.roomTypes.split(',') : [],
          rentMin: r.rentMin ?? '',
          rentMax: r.rentMax ?? '',
          bondWeeks: r.bondWeeks ?? '',
          billsIncluded: r.billsIncluded ?? 'UNKNOWN',
          billsNote: r.billsNote ?? '',
          availableFrom: r.availableFrom ?? '',
          minTermMonths: r.minTermMonths ?? '',
          furnished: r.furnished ?? false,
          roomsLet: r.roomsLet ?? '',
          genderPreference: r.genderPreference ?? '',
          // Null means the advertisement did not say, which is not the same
          // as no. The select carries all three states.
          couplesAllowed: r.couplesAllowed === null || r.couplesAllowed === undefined
            ? '' : String(r.couplesAllowed),
          petsAllowed: r.petsAllowed === null || r.petsAllowed === undefined
            ? '' : String(r.petsAllowed),
          smokingAllowed: r.smokingAllowed === null || r.smokingAllowed === undefined
            ? '' : String(r.smokingAllowed),
          inspectionNote: r.inspectionNote ?? '',
          consentStatus: r.consentStatus ?? 'NONE',
          // Not on the public response, so it arrives only through the admin
          // endpoint this screen uses.
          consentNote: r.consentNote ?? '',
          sourceUrl: r.sourceUrl ?? '',
          contact: r.contact ?? '',
          imageUrls: r.images?.length > 0 ? r.images.map((i) => i.imageUrl) : [''],
        })
      )
      .catch((err) =>
        setLoadError(err.response?.data?.error?.message || 'Could not load that rental.')
      );
  }, [isAdmin, rentalId]);

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
          <Link
            href="/rentals"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors mt-4"
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

      <h1 className="text-2xl font-bold text-snow leading-tight mb-6">Edit rental</h1>

      {loadError && <p className="text-adelaide-red text-[13px] mb-4">{loadError}</p>}

      {!initial && !loadError ? (
        <div className="h-40 rounded-2xl bg-night border border-border-dark animate-pulse" />
      ) : initial && (
        <RentalForm
          initial={initial}
          submitLabel="Save"
          onCancel={() => router.back()}
          onSubmit={async (payload) => {
            await updateRental(rentalId, payload);
            router.push('/admin');
          }}
        />
      )}
    </PageShell>
  );
}