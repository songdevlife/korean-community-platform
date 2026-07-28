import Link from 'next/link';
import { getUpdates, getUpdateCategories } from '@/api/server';
import PageShell from '@/components/PageShell';
import UpdateCard from '@/components/UpdateCard';
import UpdateFilters from '@/components/UpdateFilters';

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

  // Parameter names must match AustraliaUpdateController. Spring silently drops
  // unrecognised query parameters, so a mismatch fails quietly — 12 Entry 7
  // records a filter that appeared broken for exactly this reason.
  const [updateData, categories] = await Promise.all([
    getUpdates({
      sort: activeSort,
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
          <p className="text-muted text-sm">
            {isFiltered
              ? 'No updates match these filters.'
              : 'No updates published yet.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-3">
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
    </PageShell>
  );
}