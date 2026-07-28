import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowUpDown, X } from 'lucide-react';
import { fetchUpdates, fetchUpdateCategories } from '../api/updates';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import UpdateCard from '../components/UpdateCard';
import PageMeta from '../components/PageMeta';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'createdAt,asc', label: 'Oldest' },
  { value: 'title,asc', label: 'Title (A–Z)' },
];

// Mirrors the ck_australia_updates_scope database constraint.
const SCOPES = [
  { value: 'ADELAIDE', label: 'Adelaide' },
  { value: 'SOUTH_AUSTRALIA', label: 'South Australia' },
  { value: 'AUSTRALIA', label: 'Australia' },
  { value: 'COUNCIL_AREA', label: 'Council area' },
  { value: 'SUBURB', label: 'Suburb' },
];

function AustraliaUpdatesPage() {
  const [updates, setUpdates] = useState([]);
  const [categories, setCategories] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();

  const activeCategoryId = searchParams.get('category') ?? '';
  const activeScope = searchParams.get('scope') ?? '';
  const activeSort = searchParams.get('sort') || 'createdAt,desc';

  // Categories are static for the page's lifetime, so fetch them once.
  useEffect(() => {
    fetchUpdateCategories()
      .then((data) => setCategories(data ?? []))
      .catch((error) => console.error('Failed to load update categories:', error));
  }, []);

  useEffect(() => {
    setLoading(true);

    // Parameter names must match AustraliaUpdateController. Spring silently
    // drops unrecognised query parameters, so a mismatch fails quietly.
    const params = { sort: activeSort };
    if (activeCategoryId) params.category = activeCategoryId;
    if (activeScope) params.scope = activeScope;

    fetchUpdates(params)
      .then((data) => {
        setUpdates(data?.content ?? []);
        setTotalCount(data?.totalElements ?? 0);
      })
      .catch((error) => console.error('Failed to load updates:', error))
      .finally(() => setLoading(false));
  }, [activeCategoryId, activeScope, activeSort]);

  // All filters live in the URL, so each change must preserve the others.
  function updateParams(changes) {
    const next = {
      ...(activeCategoryId && { category: activeCategoryId }),
      ...(activeScope && { scope: activeScope }),
      ...(activeSort !== 'createdAt,desc' && { sort: activeSort }),
      ...changes,
    };

    Object.keys(next).forEach((key) => {
      if (!next[key]) delete next[key];
    });

    setSearchParams(next);
  }

  /**
   * Tag clicks toggle: clicking the tag you are already filtered by removes
   * the filter, which is what a second click on a highlighted control implies.
   */
  function handleTagFilter({ categoryId, scope }) {
    if (categoryId !== undefined) {
      updateParams({ category: categoryId === activeCategoryId ? '' : categoryId });
    }
    if (scope !== undefined) {
      updateParams({ scope: scope === activeScope ? '' : scope });
    }
  }

  const allCategories = [{ id: '', name: 'All' }, ...categories];
  const filterCount = (activeCategoryId ? 1 : 0) + (activeScope ? 1 : 0);

  const selectClass =
    'bg-transparent text-sm text-muted outline-none cursor-pointer ' +
    'hover:text-snow transition-colors [color-scheme:dark]';

  return (
    <Layout>
      <PageShell>
      <PageMeta
          title="Australia Updates"
          path="/australia-updates"
          description="Visa, healthcare, transport and cost-of-living updates for Korean speakers in Adelaide and across Australia."
        />
        <h1 className="text-xl font-bold text-snow mb-4">Australia Updates</h1>

        {/* Category filter as chips at all widths. Scrolls horizontally when
            narrow, wraps onto rows when there's room. */}
        <div
          className="flex gap-2 mb-4 overflow-x-auto pb-2 -mx-4 px-4
                     md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
        >
          {allCategories.map((category) => (
            <button
              key={category.id || 'all'}
              type="button"
              onClick={() => updateParams({ category: category.id })}
              aria-pressed={activeCategoryId === category.id}
              className={`shrink-0 px-4 py-1.5 rounded-full text-[13px] border
                          transition-all duration-300 ${
                activeCategoryId === category.id
                  ? 'bg-snow text-night border-snow font-medium'
                  : 'bg-transparent text-muted border-border-dark hover:text-snow hover:border-faint hover:-translate-y-0.5'
              }`}
            >
              {category.name}
            </button>
          ))}
        </div>

        <div className="flex items-center justify-between gap-4 mb-3 flex-wrap">
          <div className="flex items-center gap-3">
            <p className="text-sm text-muted">
              {loading ? '\u00A0' : `${totalCount} update${totalCount === 1 ? '' : 's'}`}
            </p>

            {filterCount > 0 && (
              <button
                type="button"
                onClick={() => setSearchParams({})}
                className="flex items-center gap-1 text-[13px] text-muted hover:text-snow transition-colors"
              >
                <X size={13} strokeWidth={2} />
                Clear filters
              </button>
            )}
          </div>

          <div className="flex items-center gap-4 shrink-0">
            {/* Region and category are independent axes — a reader may want
                Adelaide news regardless of topic, or the reverse. */}
            <select
              value={activeScope}
              onChange={(e) => updateParams({ scope: e.target.value })}
              aria-label="Region"
              className={selectClass}
            >
              <option value="" className="bg-surface">All regions</option>
              {SCOPES.map((scope) => (
                <option key={scope.value} value={scope.value} className="bg-surface">
                  {scope.label}
                </option>
              ))}
            </select>

            <div className="flex items-center gap-1.5">
              <ArrowUpDown size={14} strokeWidth={1.75} className="text-muted" />
              <select
                value={activeSort}
                onChange={(e) => updateParams({ sort: e.target.value })}
                aria-label="Sort by"
                className={selectClass}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value} className="bg-surface">
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="grid gap-3">
            {[0, 1, 2, 3].map((i) => (
              <div
                key={i}
                className="h-28 rounded-2xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : updates.length === 0 ? (
          <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
            <p className="text-muted text-sm">
              {filterCount > 0
                ? 'No updates match these filters.'
                : 'No updates published yet.'}
            </p>
          </div>
        ) : (
          <div className="grid gap-3">
            {updates.map((update) => (
              <Link key={update.id} to={`/australia-updates/${update.id}`} className="block">
                <UpdateCard update={update} onFilter={handleTagFilter} />
              </Link>
            ))}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default AustraliaUpdatesPage;