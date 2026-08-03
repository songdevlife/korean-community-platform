import Link from 'next/link';
import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * Numbered page controls for public listings.
 *
 * Links rather than buttons, unlike the admin Pagination component: a page of
 * a public list is a place, so it should have a URL that can be shared,
 * bookmarked and crawled. That also keeps this a server component, so a list
 * page does not ship as JavaScript to make its footer work.
 *
 * @param {number} page       Current page, zero-indexed to match Spring's Page
 * @param {number} totalPages Total page count from the API
 * @param {string} basePath   e.g. '/australia-updates'
 * @param {object} [params]   Filters to preserve across pages
 */
export default function PageLinks({ page, totalPages, basePath, params = {} }) {
  if (totalPages <= 1) return null;

  const MAX_BUTTONS = 7;
  let start = Math.max(0, page - Math.floor(MAX_BUTTONS / 2));
  const end = Math.min(totalPages, start + MAX_BUTTONS);
  start = Math.max(0, end - MAX_BUTTONS);

  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);

  // Filters travel with the page number. Without this, turning to page two of
  // a filtered list quietly drops the filter.
  const href = (p) => {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v) q.set(k, v);
    });
    if (p > 0) q.set('page', p);
    const s = q.toString();
    return s ? `${basePath}?${s}` : basePath;
  };

  const base =
    'min-w-[32px] h-8 px-2 rounded-lg text-[13px] flex items-center justify-center transition-colors';
  const inactive = 'text-muted hover:text-snow hover:bg-surface';
  const disabled = 'text-faint opacity-30 pointer-events-none';

  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-1 mt-6">
      <Link
        href={href(page - 1)}
        aria-label="Previous page"
        aria-disabled={page === 0}
        className={`${base} ${page === 0 ? disabled : inactive}`}
      >
        <ChevronLeft size={16} strokeWidth={2} />
      </Link>

      {start > 0 && <span className="px-1 text-faint text-[13px]">…</span>}

      {pages.map((p) => (
        <Link
          key={p}
          href={href(p)}
          aria-current={p === page ? 'page' : undefined}
          className={`${base} ${
            p === page ? 'bg-snow text-night font-medium' : inactive
          }`}
        >
          {p + 1}
        </Link>
      ))}

      {end < totalPages && <span className="px-1 text-faint text-[13px]">…</span>}

      <Link
        href={href(page + 1)}
        aria-label="Next page"
        aria-disabled={page >= totalPages - 1}
        className={`${base} ${page >= totalPages - 1 ? disabled : inactive}`}
      >
        <ChevronRight size={16} strokeWidth={2} />
      </Link>
    </nav>
  );
}