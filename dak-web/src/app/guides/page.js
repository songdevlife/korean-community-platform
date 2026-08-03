import Link from 'next/link';
import { getGuides, getGuideCategories } from '@/api/server';
import PageShell from '@/components/PageShell';
import GuideCard from '@/components/GuideCard';
import GuideFilterChips, { ClearFilterButton } from '@/components/GuideFilterChips';
import PageLinks from '@/components/PageLinks';
import AdminNewGuideLink from '@/components/AdminNewGuideLink';

export const metadata = {
  title: 'Guides',
  description:
    'Practical guides on study, visas, housing and daily life for Korean speakers in Adelaide.',
  alternates: { canonical: '/guides' },
};

/**
 * Guide list. searchParams arrives as a prop rather than being read from a hook,
 * so the filtered list is fetched and rendered on the server — /guides?category=x
 * is a real page a crawler can read, not a client-side state change.
 *
 * Cards render here rather than inside a client wrapper, which means the tag on
 * each card is a plain label. Filtering lives in the chips above instead: a
 * function prop cannot cross into a server component, and putting the whole grid
 * on the client to keep one tag clickable would ship the entire list as
 * JavaScript for no gain.
 */
export default async function GuidesPage({ searchParams }) {
  const params = await searchParams;
  const activeCategoryId = params?.category ?? '';
  const activePage = Math.max(0, Number(params?.page ?? 0));

  // Both are fetched server-side and in parallel — neither depends on the other.
  const [guideData, categories] = await Promise.all([
    getGuides({
      page: activePage,
      ...(activeCategoryId && { categoryId: activeCategoryId }),
    }),
    getGuideCategories(),
  ]);

  const guides = guideData?.content ?? [];
  const totalCount = guideData?.totalElements ?? 0;
  const isFiltered = Boolean(activeCategoryId);

  return (
    <PageShell>
      {/* Title row. The write button sits opposite the heading so it stays
          out of the reading path for the guests who make up most visitors. */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <h1 className="text-xl font-bold text-snow">Guides</h1>
        <AdminNewGuideLink />
      </div>

      <GuideFilterChips
        categories={categories ?? []}
        activeCategoryId={activeCategoryId}
      />

      <div className="flex items-center justify-between gap-4 mb-3 flex-wrap">
        <div className="flex items-center gap-3">
          <p className="text-sm text-muted">
            {totalCount} guide{totalCount === 1 ? '' : 's'}
          </p>
          {isFiltered && <ClearFilterButton />}
        </div>
      </div>

      {guides.length === 0 ? (
        <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
          <p className="text-muted text-sm">
            {isFiltered ? 'No guides match this filter.' : 'No guides published yet.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-3">
          {guides.map((guide) => (
            <Link key={guide.id} href={`/guides/${guide.slug}`} className="block">
              <GuideCard guide={guide} />
            </Link>
          ))}
        </div>
      )}

      <PageLinks
        page={activePage}
        totalPages={guideData?.totalPages ?? 0}
        basePath="/guides"
        params={{ category: activeCategoryId }}
      />
    </PageShell>
  );
}