import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { ArrowUpDown, Plus } from 'lucide-react';
import { fetchBusinesses, fetchBusinessCategories } from '../api/businesses';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import CategoryChip from '../components/CategoryChip';
import BusinessCard from '../components/BusinessCard';
import PageMeta from '../components/PageMeta';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'name,asc', label: 'Name (A–Z)' },
  { value: 'suburb,asc', label: 'Suburb' },
];

function DirectoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeCategory = searchParams.get('category') || '';
  const activeSort = searchParams.get('sort') || 'createdAt,desc';

  const { user } = useAuth();

  const [categories, setCategories] = useState([]);
  const [businesses, setBusinesses] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBusinessCategories()
      .then((data) => setCategories(data ?? []))
      .catch((error) => console.error('Failed to load categories:', error));
  }, []);

  useEffect(() => {
    setLoading(true);
    fetchBusinesses({
      category: activeCategory || undefined,
      sort: activeSort,
    })
      .then((data) => {
        setBusinesses(data?.content ?? []);
        setTotalCount(data?.totalElements ?? 0);
      })
      .catch((error) => console.error('Failed to load businesses:', error))
      .finally(() => setLoading(false));
  }, [activeCategory, activeSort]);

  // Both filters live in the URL, so each update must preserve the other.
  function updateParams(changes) {
    const next = {};
    if (activeCategory) next.category = activeCategory;
    if (activeSort !== 'createdAt,desc') next.sort = activeSort;
    Object.assign(next, changes);

    Object.keys(next).forEach((key) => {
      if (!next[key]) delete next[key];
    });

    setSearchParams(next);
  }

  function handleCategoryClick(categoryName) {
    updateParams({ category: activeCategory === categoryName ? '' : categoryName });
  }

  return (
    <Layout>
      <PageShell>
      <PageMeta
          title={activeCategory ? `${activeCategory} in Adelaide` : 'Korean businesses in Adelaide'}
          path="/directory"
          description="Browse Korean-speaking businesses and services across Adelaide — restaurants, healthcare, groceries, education and more."
        />

        {/* Submission sits opposite the heading rather than at the foot of the
            list: an owner arriving to add their business should not have to
            scroll past every existing listing to find the way in. Shown to any
            signed-in user — listings are reviewed before they appear, so the
            queue is the control rather than a role. */}
        <div className="flex items-center justify-between gap-4 mb-4">
          <h1 className="text-xl font-bold text-snow">Businesses</h1>

          {user && (
            <Link
              to="/businesses/new"
              className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border
                         border-border-dark text-muted hover:text-snow hover:border-faint
                         transition-colors shrink-0"
            >
              <Plus size={13} strokeWidth={2} />
              List your business
            </Link>
          )}
        </div>

        {/* Category chips: scroll horizontally when narrow, wrap on desktop. */}
        {categories.length > 0 && (
          <div
            className="flex gap-2 mb-5 overflow-x-auto pb-2 -mx-4 px-4
                       md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
          >
            {categories.map((category) => (
              <CategoryChip
                key={category.id}
                name={category.name}
                active={activeCategory === category.name}
                onClick={() => handleCategoryClick(category.name)}
              />
            ))}
          </div>
        )}

        <div className="flex items-center justify-between gap-4 mb-3">
          <p className="text-sm text-muted">
            {loading ? '\u00A0' : `${totalCount} business${totalCount === 1 ? '' : 'es'}`}
          </p>

          <div className="flex items-center gap-1.5 shrink-0">
            <ArrowUpDown size={14} strokeWidth={1.75} className="text-muted" />
            <select
              value={activeSort}
              onChange={(e) => updateParams({ sort: e.target.value })}
              aria-label="Sort by"
              className="bg-transparent text-sm text-muted outline-none cursor-pointer
                         hover:text-snow transition-colors [color-scheme:dark]"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value} className="bg-surface">
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                className="rounded-2xl bg-night border border-border-dark overflow-hidden animate-pulse"
              >
                <div className="aspect-[4/3] bg-surface" />
                <div className="p-3.5">
                  <div className="h-4 w-3/4 rounded bg-surface mb-2" />
                  <div className="h-3 w-1/2 rounded bg-surface" />
                </div>
              </div>
            ))}
          </div>
        ) : businesses.length === 0 ? (
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
              <Link key={business.id} to={`/businesses/${business.slug}`} className="min-w-0">
                <BusinessCard business={business} />
              </Link>
            ))}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default DirectoryPage;