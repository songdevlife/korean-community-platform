import Link from 'next/link';
import { getUpdates, getUpdateCategories } from '@/api/server';
import PageShell from '@/components/PageShell';
import UpdateCard from '@/components/UpdateCard';
import UpdateFilters from '@/components/UpdateFilters';
import PageLinks from '@/components/PageLinks';

export const metadata = {
  title: 'Australia Updates',
  description:
    'Visa, healthcare, transport and cost-of-living updates for Korean speakers in Adelaide and across Australia.',
  alternates: { canonical: '/australia-updates' },
};

/**
 * Australia Updates list. Filters arrive as searchParams and are applied
 * server-side, so a filtered view is a real page rather than client state.
 */
export default async function AustraliaUpdatesPage({ searchParams }) {
  const params = await searchParams;
  const activeCategoryId = params?.category ?? '';
  const activeScope = params?.scope ?? '';
  const activeSort = params?.sort || 'createdAt,desc';
  // Zero-indexed to match Spring's Page, but one-indexed in the URL, where a
  // reader who edits it by hand would expect the first page to be 1.
  const activePage = Math.max(0, Number(params?.page ?? 0));

  // Parameter names must match AustraliaUpdateController. Spring silently drops
  // unrecognised query parameters, so a mismatch fails quietly — 12 Entry 7
  // records a filter that appeared broken for exactly this reason.
  const [updateData, categories] = await Promise.all([
    getUpdates({
      sort: activeSort,
      page: activePage,
      ...(activeCategoryId && { category: activeCategoryId }),
      ...(activeScope && { scope: activeScope }),
    }),
    getUpdateCategories(),
  ]);

  const updates = updateData?.content ?? [];
  const totalCount = updateData?.totalElements ?? 0;
  const isFiltered = Boolean(activeCategoryId || activeScope);

  return (
    <PageShell>
      <h1 className="text-xl font-bold text-snow mb-4">Australia Updates</h1>

      <UpdateFilters
        categories={categories ?? []}
        activeCategoryId={activeCategoryId}
        activeScope={activeScope}
        activeSort={activeSort}
        totalCount={totalCount}
      />

      {updates.length === 0 ? (
        <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
        {/* A page beyond the end of a filtered list is empty for a different
            reason than a filter with no matches, and saying the wrong one
            tells a reader there is nothing here when there is — on page one.
            Reachable by narrowing a filter while deeper in, or by a shared
            link to a page that has since shrunk. */}
        {activePage > 0 ? (
          <>
            <p className="text-muted text-sm mb-3">
              이 페이지에는 결과가 없습니다.
            </p>
            <Link
              href={
                activeCategoryId || activeScope
                  ? `/australia-updates?${new URLSearchParams({
                      ...(activeCategoryId && { category: activeCategoryId }),
                      ...(activeScope && { scope: activeScope }),
                    })}`
                  : '/australia-updates'
              }
              className="inline-block px-4 py-2 rounded-xl bg-korea-blue text-white
                         text-sm font-medium hover:bg-korea-blue/85 transition-colors"
            >
              첫 페이지로
            </Link>
          </>
        ) : (
          <p className="text-muted text-sm">
            {isFiltered
              ? 'No updates match these filters.'
              : 'No updates published yet.'}
          </p>
        )}
      </div>
      ) : (
        // Two columns from the small breakpoint, like guides and events. One
        // full-width column made each card span the whole screen, so a
        // two-line clamp still produced two very long lines.
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {updates.map((update) => (
            <Link
              key={update.id}
              href={`/australia-updates/${update.id}`}
              className="block"
            >
              <UpdateCard update={update} />
            </Link>
          ))}
        </div>
      )}

      <PageLinks
        page={activePage}
        totalPages={updateData?.totalPages ?? 0}
        basePath="/australia-updates"
        params={{ category: activeCategoryId, scope: activeScope,
                  sort: activeSort === 'createdAt,desc' ? '' : activeSort }}
      />
    </PageShell>
  );
}