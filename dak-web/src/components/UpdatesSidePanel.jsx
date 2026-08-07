import Link from 'next/link';
import { Newspaper, Sparkles } from 'lucide-react';
import { timeAgo, isNew } from '@/utils/date';
import NewBadge from './NewBadge';

/**
 * Server component. The updates arrive as a prop rather than being fetched on
 * mount, so the panel is in the initial HTML alongside the page it flanks — the
 * loading skeleton it used to need has no state left to cover.
 *
 * @param {Array} updates  Already sliced by the caller, which fetches it in the
 *   same Promise.all as the rest of the page.
 */
export default function UpdatesSidePanel({ updates = [] }) {
  return (
    /* No border needed — the lighter content column beside it provides the
       visual edge. */
    <aside className="w-72 shrink-0 py-2 px-3">
      <div className="flex items-baseline justify-between gap-3 mb-4">
        <h2 className="text-sm font-semibold text-snow">Australia Updates</h2>
        <Link
          href="/australia-updates"
          className="text-xs text-muted hover:text-snow transition-colors shrink-0"
        >
          View all
        </Link>
      </div>

      {updates.length === 0 ? (
        <p className="text-xs text-muted">No updates published yet.</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {updates.map((update) => (
            <li key={update.id}>
              <Link
                href={`/australia-updates/${update.slug}`}
                className="flex gap-3 p-2 -mx-2 rounded-lg hover:bg-surface transition-colors group"
              >
                {/* Icon tile, matching the wireframe's two-column row */}
                <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                                 flex items-center justify-center">
                  <Newspaper size={15} strokeWidth={1.75} className="text-muted" />
                </span>

                <span className="min-w-0 flex-1">
                  {/* Category on the left, age on the right. The panel is only
                      288px wide, so these share one line rather than stacking. */}
                  <span className="flex items-center justify-between gap-2 mb-0.5">
                    <span className="flex items-center gap-1.5 min-w-0">
                      {update.category && (
                        <span className="text-[10px] uppercase tracking-wide text-muted truncate">
                          {update.category.name}
                        </span>
                      )}
                      {/* AI disclosure is required wherever AI-generated content
                          appears — 03 MVP AI constraints, 06 UI/UX 6.3 */}
                      {update.aiGenerated && (
                        <Sparkles
                          size={11}
                          strokeWidth={2}
                          className="text-muted shrink-0"
                          aria-label="AI-assisted summary"
                        />
                      )}
                    </span>

                    {isNew(update.createdAt) ? (
                      <NewBadge />
                    ) : (
                      <span className="text-[10px] text-faint shrink-0">
                        {timeAgo(update.createdAt)}
                      </span>
                    )}
                  </span>

                  <span className="block text-[13px] leading-snug text-snow
                                   group-hover:text-white transition-colors line-clamp-2">
                    {update.title}
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}