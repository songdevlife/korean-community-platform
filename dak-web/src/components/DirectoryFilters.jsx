'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowUpDown } from 'lucide-react';
import CategoryChip from './CategoryChip';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'name,asc', label: 'Name (A–Z)' },
  { value: 'suburb,asc', label: 'Suburb' },
];

/**
 * Category chips and sort control. Client-side because both change the URL, but
 * the listing itself is fetched and rendered on the server from the resulting
 * query — so /directory?category=Healthcare is a page a crawler can read.
 *
 * @param {number} totalCount  Rendered here because it shares a flex row with
 *   the sort control; splitting that row across the server/client boundary
 *   would need a wrapper for no benefit.
 */
export default function DirectoryFilters({
  categories,
  activeCategory,
  activeSort,
  totalCount,
}) {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Both filters live in the URL, so each update must preserve the other.
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

    const query = next.toString();
    router.push(query ? `/directory?${query}` : '/directory');
  }

  /**
   * Clicking the category already selected clears it, which is what a second
   * click on a highlighted control implies.
   */
  function handleCategoryClick(categoryName) {
    updateParams({ category: activeCategory === categoryName ? '' : categoryName });
  }

  return (
    <>
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
          {totalCount} business{totalCount === 1 ? '' : 'es'}
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
    </>
  );
}