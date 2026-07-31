import Link from 'next/link';
import { getGuides, getBusinesses, getBusinessCategories, getUpdates } from '@/api/server';
import PageShell from '@/components/PageShell';
import GuideCard from '@/components/GuideCard';
import BusinessCard from '@/components/BusinessCard';
import CategoryChipLink from '@/components/CategoryChipLink';
import HomeSearch from '@/components/HomeSearch';
import HomeGreeting from '@/components/HomeGreeting';
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
  const [guideData, businessData, categories, updateData] = await Promise.all([
    getGuides({ pageSize: FEATURED_GUIDE_COUNT }),
    getBusinesses({ pageSize: FEATURED_COUNT }),
    getBusinessCategories(),
    getUpdates({ pageSize: SIDE_PANEL_COUNT }),
  ]);

  const guides = guideData?.content ?? [];
  const businesses = businessData?.content ?? [];
  const updates = updateData?.content ?? [];

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

      {/* Chips scroll horizontally on mobile, wrap onto rows on desktop. Plain
          links rather than buttons — each is a destination, so a crawler can
          follow them into the category listings. */}
      {categories?.length > 0 && (
        <div
          className="flex gap-2 mb-6 overflow-x-auto pb-2 -mx-4 px-4
                     md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
        >
          {categories.map((category) => (
            <CategoryChipLink key={category.id} name={category.name} />
          ))}
        </div>
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

      <div className="flex items-baseline justify-between gap-4 mb-3 mt-8">
        <h2 className="text-lg font-semibold text-snow">Featured businesses</h2>
        <Link
          href="/directory"
          className="text-sm text-muted hover:text-snow transition-colors shrink-0"
        >
          View all
        </Link>
      </div>

      {businesses.length === 0 ? (
        <div className="rounded-xl border border-border-dark bg-night p-8 text-center">
          <p className="text-muted text-sm">No businesses listed yet.</p>
        </div>
      ) : (
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
      )}
    </PageShell>
  );
}