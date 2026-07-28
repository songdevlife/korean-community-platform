'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * Numbered page controls for admin queues.
 *
 * Renders nothing for a single page: showing "1" alone tells the reader
 * something they can already see.
 *
 * @param {number} page       Current page, zero-indexed to match Spring's Page.
 * @param {number} totalPages Total page count from the API.
 * @param {Function} onChange Called with the new zero-indexed page.
 */
function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;

  // Show at most seven numbers, windowed around the current page, so a long
  // queue doesn't produce a row of buttons wider than the content.
  const MAX_BUTTONS = 7;
  let start = Math.max(0, page - Math.floor(MAX_BUTTONS / 2));
  const end = Math.min(totalPages, start + MAX_BUTTONS);
  start = Math.max(0, end - MAX_BUTTONS);

  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);

  const btn =
    'min-w-[32px] h-8 px-2 rounded-lg text-[13px] transition-colors ' +
    'disabled:opacity-30 disabled:cursor-not-allowed';

  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-1 mt-4">
      <button
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        aria-label="Previous page"
        className={`${btn} text-muted hover:text-snow hover:bg-surface`}
      >
        <ChevronLeft size={16} strokeWidth={2} className="mx-auto" />
      </button>

      {start > 0 && <span className="px-1 text-faint text-[13px]">…</span>}

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
          className={`${btn} ${
            p === page
              ? 'bg-snow text-night font-medium'
              : 'text-muted hover:text-snow hover:bg-surface'
          }`}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages && <span className="px-1 text-faint text-[13px]">…</span>}

      <button
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
        className={`${btn} text-muted hover:text-snow hover:bg-surface`}
      >
        <ChevronRight size={16} strokeWidth={2} className="mx-auto" />
      </button>
    </nav>
  );
}

export default Pagination;