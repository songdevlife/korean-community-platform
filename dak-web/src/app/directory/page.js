import Link from 'next/link';
import { getBusinesses, getBusinessCategories } from '@/api/server';
import PageShell from '@/components/PageShell';
import BusinessCard from '@/components/BusinessCard';
import DirectoryFilters from '@/components/DirectoryFilters';
import AdminNewBusinessLink from '@/components/AdminNewBusinessLink';

export const metadata = {
  title: 'Korean businesses in Adelaide',
  description:
    'Browse Korean-speaking businesses and services across Adelaide — restaurants, healthcare, groceries, education and more.',
  alternates: { canonical: '/directory' },
};

export default async function DirectoryPage({ searchParams }) {
  const params = await searchParams;
  const activeCategory = params?.category ?? '';
  const activeSort = params?.sort || 'createdAt,desc';

  const [businessData, categories] = await Promise.all([
    getBusinesses({
      sort: activeSort,
      ...(activeCategory && { category: activeCategory }),
    }),
    getBusinessCategories(),
  ]);

  const businesses = businessData?.content ?? [];
  const totalCount = businessData?.totalElements ?? 0;

  return (
    <PageShell>
      {/* Submission sits opposite the heading rather than at the foot of the
          list: an owner arriving to add their business should not have to
          scroll past every existing listing to find the way in. */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <h1 className="text-xl font-bold text-snow">Businesses</h1>
        <AdminNewBusinessLink />
      </div>

      <DirectoryFilters
        categories={categories ?? []}
        activeCategory={activeCategory}
        activeSort={activeSort}
        totalCount={totalCount}
      />

      {businesses.length === 0 ? (
        <div className="rounded-xl border border-border-dark bg-night p-8 text-center">
          <p className="text-muted text-sm">
            {activeCategory
              ? 'No businesses in this category yet.'
              : 'No businesses listed yet.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {businesses.map((business) => (
            <Link
              key={business.id}
              href={`/businesses/${business.slug}`}
              className="min-w-0"
            >
              <BusinessCard business={business} />
            </Link>
          ))}
        </div>
      )}
    </PageShell>
  );
}