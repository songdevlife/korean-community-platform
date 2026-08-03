'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { X } from 'lucide-react';

/**
 * Category chips. A client component because clicking one has to change the
 * URL — but it only pushes a new address; the filtered list is fetched and
 * rendered on the server, so /guides?category=... is a crawlable page rather
 * than a client-side state change.
 */
export default function GuideFilterChips({ categories, activeCategoryId }) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function setCategory(categoryId) {
    const next = new URLSearchParams(searchParams);
    if (categoryId) {
      next.set('category', categoryId);
    } else {
      next.delete('category');
    }
    // Changing what is listed invalidates where you are in it: narrowing from
    // page two lands on a page two the new list may not have.
    next.delete('page');
    const query = next.toString();
    router.push(query ? `/guides?${query}` : '/guides');
  }

  const allCategories = [{ id: '', name: 'All' }, ...categories];

  return (
    <>
      {/* Category filter as chips at all widths, matching AU Updates.
          Scrolls horizontally when narrow, wraps onto rows when there's room. */}
      <div
        className="flex gap-2 mb-4 overflow-x-auto pb-2 -mx-4 px-4
                   md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
      >
        {allCategories.map((category) => (
          <button
            key={category.id || 'all'}
            type="button"
            onClick={() => setCategory(category.id)}
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
    </>
  );
}

export function ClearFilterButton() {
  const router = useRouter();

  return (
    <button
      type="button"
      onClick={() => router.push('/guides')}
      className="flex items-center gap-1 text-[13px] text-muted hover:text-snow transition-colors"
    >
      <X size={13} strokeWidth={2} />
      Clear filters
    </button>
  );
}