'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowUpDown, X } from 'lucide-react';

// Mirrors the ck_australia_updates_scope database constraint.
const SCOPES = [
  { value: 'ADELAIDE', label: 'Adelaide' },
  { value: 'SOUTH_AUSTRALIA', label: 'South Australia' },
  { value: 'AUSTRALIA', label: 'Australia' },
  { value: 'COUNCIL_AREA', label: 'Council area' },
  { value: 'SUBURB', label: 'Suburb' },
];

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'createdAt,asc', label: 'Oldest' },
  { value: 'title,asc', label: 'Title (A–Z)' },
];

/**
 * All three filter controls in one client component. They share the URL and
 * each change has to preserve the others, so splitting them would mean
 * duplicating that merge logic three times.
 *
 * Only the controls are interactive — the list itself is rendered on the server
 * from the resulting query string, so a filtered view is a crawlable page.
 *
 * @param {number} totalCount  Rendered here rather than by the page because it
 *   sits on the same row as Clear filters, and splitting one flex row across a
 *   server/client boundary would need a wrapper for no benefit.
 */
export default function UpdateFilters({
  categories,
  activeCategoryId,
  activeScope,
  activeSort,
  totalCount,
}) {
  const router = useRouter();
  const searchParams = useSearchParams();

  // All filters live in the URL, so each change must preserve the others.
  function updateParams(changes) {
    const next = new URLSearchParams(searchParams);

    Object.entries(changes).forEach(([key, value]) => {
      if (value) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
    });

    // The default sort is implied by its absence, keeping the common URL clean.
    if (next.get('sort') === 'createdAt,desc') next.delete('sort');

    // Any change to what is being listed invalidates where you are in it.
    // Without this, narrowing a filter from page two lands on a page two that
    // no longer exists, and the empty result reads as "nothing matches" when
    // the matches are on page one.
    next.delete('page');

    const query = next.toString();
    router.push(query ? `/australia-updates?${query}` : '/australia-updates');
  }

  const allCategories = [{ id: '', name: 'All' }, ...categories];
  const filterCount = (activeCategoryId ? 1 : 0) + (activeScope ? 1 : 0);

  const selectClass =
    'bg-transparent text-sm text-muted outline-none cursor-pointer ' +
    'hover:text-snow transition-colors [color-scheme:dark]';

  return (
    <>
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
            {totalCount} update{totalCount === 1 ? '' : 's'}
          </p>

          {filterCount > 0 && (
            <button
              type="button"
              onClick={() => router.push('/australia-updates')}
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
    </>
  );
}