import Link from 'next/link';
import { getGuides, getBusinesses, getEvents, getUpdates, getRentals } from '@/api/server';
import PageShell from '@/components/PageShell';
import GuideCard from '@/components/GuideCard';
import BusinessCard from '@/components/BusinessCard';
import EventCard from '@/components/EventCard';
import RentalCard from '@/components/RentalCard';
import HomeSearch from '@/components/HomeSearch';
import HomeGreeting from '@/components/HomeGreeting';
import HomeNotice from '@/components/HomeNotice';
import UpdatesSidePanel from '@/components/UpdatesSidePanel';

export const metadata = {
  description:
    'Korean-language local information for Adelaide: find services and community updates in South Australia.',
  alternates: { canonical: '/' },
};

// Home is a preview, not a listing: a handful of cards to show what the site
// holds, with everything else behind View all.
const FEATURED_COUNT = 3;

// Guides lead the page: 02 Product Vision treats search as the primary
// acquisition channel, and a guide is what a search arrives on.
const FEATURED_GUIDE_COUNT = 3;
const SIDE_PANEL_COUNT = 5;

export default async function HomePage() {
  const [guideData, businessData, eventData, updateData, rentalData] = await Promise.all([
    getGuides({ pageSize: FEATURED_GUIDE_COUNT }),
    getBusinesses({ pageSize: FEATURED_COUNT }),
    getEvents({ pageSize: FEATURED_COUNT }),
    getUpdates({ pageSize: SIDE_PANEL_COUNT }),
    getRentals({ pageSize: FEATURED_COUNT }),
  ]);

  const guides = guideData?.content ?? [];
  const businesses = businessData?.content ?? [];
  const events = eventData?.content ?? [];
  const updates = updateData?.content ?? [];
  const rentals = rentalData?.content ?? [];

  return (
    <PageShell aside={<UpdatesSidePanel updates={updates} />}>
      {/* Mobile only. The sidebar carries the mark and the name on desktop, but
          there is no sidebar below md, so someone arriving from a search result
          on a phone sees a search box with nothing identifying the site around
          it. Hidden from md up rather than duplicated. */}
      <div className="md:hidden flex flex-col items-center text-center mb-5">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/logo-mark-dark.png"
          alt=""
          aria-hidden="true"
          className="w-14 h-14 object-contain mb-2 opacity-90"
        />
        <span className="block text-2xl font-bold text-snow leading-none">DAK</span>
        <span className="block text-[11px] tracking-[.08em] text-muted mt-1.5">
          Discover Adelaide Korea
        </span>
      </div>

      <HomeGreeting />

      {/* Hero block. Wraps the headline and search in their own raised panel,
          per the home wireframe. */}
      <section className="bg-night rounded-2xl p-5 md:p-6 mb-5">
        <h1 className="text-base md:text-lg font-semibold text-snow text-center mb-3">
          What are you looking for?
        </h1>
        <HomeSearch />
      </section>

      {/* Below the hero rather than above it. Someone who came to search
          should reach the box first; the notice is for the reader who did not
          have a question in mind, and that reader is already scrolling.

          Removed by deleting this block. There is no expiry and no admin
          control on purpose — a notice nobody has to take down stays up. */}
      <HomeNotice
        title="2026 센서스 — 8월 11일 기준, 유학생과 임시비자도 대상입니다"
        description="제출 마감은 10월 1일이고, 벌금은 서면 지시를 받은 뒤부터 발생합니다. 자세한 내용은 더보기를 클릭해주세요."
        href="/guides/australia-census-2026-guide"
        expiresAt="2026-10-02"
      />

      {/* Where the directory category chips used to be. Seven links into a
          directory with nothing approved in it meant seven routes to an empty
          page, which is a worse first impression than no shortcuts at all.
          Restore them when there are listings to reach.

          Events take the slot for the one reason nothing else on this page
          has: they expire. A guide read next month is the same guide; an
          event scrolled past this week is gone. Hidden when empty, as the
          guides are. */}
      {events.length > 0 && (
        <>
          <div className="flex items-baseline justify-between gap-4 mb-3">
            <h2 className="text-lg font-semibold text-snow">Upcoming events</h2>
            <Link
              href="/events"
              className="text-sm text-muted hover:text-snow transition-colors shrink-0"
            >
              View all
            </Link>
          </div>

          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 mb-8 items-start">
          {events.map((event) => (
              <Link key={event.id} href={`/events/${event.slug}`} className="min-w-0">
                <EventCard event={event} />
              </Link>
            ))}
          </div>
        </>
      )}

      {/* Rentals, directly below events and above guides. Housing is the
          first problem for most people arriving in Adelaide, and unlike a
          guide a listing is a reason to come back next week - it changes
          while the guides stay the same. Hidden when empty, as the others
          are. */}
      {rentals.length > 0 && (
        <>
          <div className="flex items-baseline justify-between gap-4 mb-3">
            <h2 className="text-lg font-semibold text-snow">Rentals</h2>
            <Link
              href="/rentals"
              className="text-sm text-muted hover:text-snow transition-colors shrink-0"
            >
              View all
            </Link>
          </div>

          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3 mb-8 items-start">
            {rentals.map((rental) => (
              <Link key={rental.id} href={`/rentals/${rental.slug}`} className="min-w-0">
                <RentalCard rental={rental} />
              </Link>
            ))}
          </div>
        </>
      )}

      {/* Guides preview, above the directory. Hidden entirely when nothing is
          published rather than showing an empty state: a guest has no way to
          add one, so the message would be noise. */}
      {guides.length > 0 && (
        <>
          <div className="flex items-baseline justify-between gap-4 mb-3">
            <h2 className="text-lg font-semibold text-snow">Local guides</h2>
            <Link
              href="/guides"
              className="text-sm text-muted hover:text-snow transition-colors shrink-0"
            >
              View all
            </Link>
          </div>

          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {guides.map((guide) => (
              <Link key={guide.id} href={`/guides/${guide.slug}`} className="min-w-0">
                <GuideCard guide={guide} />
              </Link>
            ))}
          </div>
        </>
      )}

      {/* Hidden when empty, matching the guides rather than the earlier
          behaviour of announcing that nothing is listed. A visitor cannot add
          a business from here, so the empty state was telling them about a
          gap they have no way to fill — and doing it on the front page. */}
      {businesses.length > 0 && (
        <>
          <div className="flex items-baseline justify-between gap-4 mb-3 mt-8">
            <h2 className="text-lg font-semibold text-snow">Featured businesses</h2>
            <Link
              href="/directory"
              className="text-sm text-muted hover:text-snow transition-colors shrink-0"
            >
              View all
            </Link>
          </div>

          <div className="grid gap-3 grid-cols-2 md:grid-cols-3">
            {businesses.map((business) => (
              <Link
                key={business.id}
                href={`/businesses/${business.slug}`}
                className="min-w-0"
              >
                <BusinessCard business={business} compact />
              </Link>
            ))}
          </div>
        </>
      )}
    </PageShell>
  );
}