'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Search } from 'lucide-react';

const TYPES = [
  { value: 'all', label: 'All' },
  { value: 'events', label: 'Events' },
  { value: 'businesses', label: 'Businesses' },
  { value: 'guides', label: 'Guides' },
  { value: 'updates', label: 'Updates' },
];

/**
 * Search box and type tabs. Both change the URL rather than filtering in place,
 * so the results themselves are fetched and rendered on the server.
 *
 * The input holds local state so typing does not refire the query on every
 * keystroke; only submitting updates the address.
 */
export default function SearchForm({ keyword, activeType }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [value, setValue] = useState(keyword);

  // Keep the input in sync when the URL changes from outside this component —
  // browser back/forward, or a search submitted from the home page.
  useEffect(() => {
    setValue(keyword);
  }, [keyword]);

  function push(changes) {
    const next = new URLSearchParams(searchParams);

    Object.entries(changes).forEach(([key, v]) => {
      if (v) {
        next.set(key, v);
      } else {
        next.delete(key);
      }
    });

    const query = next.toString();
    router.push(query ? `/search?${query}` : '/search');
  }

  function handleSubmit(e) {
    e.preventDefault();
    push({ keyword: value.trim() });
  }

  return (
    <>
      {/* Search stays on the page so results can be refined without navigating
          back to Home. */}
      <form onSubmit={handleSubmit} className="mb-4">
        <div className="relative">
          <Search
            size={18}
            strokeWidth={1.75}
            className="absolute left-4 top-1/2 -translate-y-1/2 text-faint pointer-events-none"
          />
          <input
            type="search"
            placeholder="Search events, guides, businesses"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            aria-label="Search"
            className="w-full pl-11 pr-4 py-3.5 rounded-xl border border-border-dark bg-night
                       text-[16px] md:text-[15px] text-snow placeholder:text-faint
                       outline-none focus:border-faint transition-colors [color-scheme:dark]"
          />
        </div>
      </form>

      {/* Type tabs, matching the chip pattern used on Guides and AU Updates.
          Search carries no other filters: Directory and Guides each hold their
          own category controls, and duplicating them here would mean maintaining
          the same filter in three places. */}
      <div
        className="flex gap-2 mb-5 overflow-x-auto pb-2 -mx-4 px-4
                   md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
      >
        {TYPES.map((type) => (
          <button
            key={type.value}
            type="button"
            onClick={() => push({ type: type.value === 'all' ? '' : type.value })}
            aria-pressed={activeType === type.value}
            className={`shrink-0 px-4 py-1.5 rounded-full text-[13px] border
                        transition-all duration-300 ${
              activeType === type.value
                ? 'bg-snow text-night border-snow font-medium'
                : 'bg-transparent text-muted border-border-dark hover:text-snow hover:border-faint hover:-translate-y-0.5'
            }`}
          >
            {type.label}
          </button>
        ))}
      </div>
    </>
  );
}