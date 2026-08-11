import Link from 'next/link';
import { Tag } from 'lucide-react';
import { eventDate } from '@/utils/date';

/**
 * Server component, matching UpdatesSidePanel: the promotions arrive as a
 * prop rather than being fetched here, so the panel is in the initial HTML.
 *
 * Separated from the events grid because a promotion is not an event. An
 * event is a decision about a particular evening; a voucher runs for weeks
 * and asks nothing of a reader's diary. Shown side by side they read as one
 * kind of thing, and the long window of a promotion holds a slot in the
 * grid that a dated event needed — a burger voucher published in June was
 * still occupying one of three cards in August.
 *
 * @param {Array} promotions  Already filtered and sliced by the caller.
 */
export default function PromotionsSidePanel({ promotions = [] }) {
  if (promotions.length === 0) return null;

  return (
    <aside className="w-72 shrink-0 py-2 px-3">
      <div className="flex items-baseline justify-between gap-3 mb-4">
        <h2 className="text-sm font-semibold text-snow">할인·프로모션</h2>
        <Link
          href="/events"
          className="text-xs text-muted hover:text-snow transition-colors shrink-0"
        >
          View all
        </Link>
      </div>

      <ul className="flex flex-col gap-1">
        {promotions.map((promotion) => (
          <li key={promotion.id}>
            <Link
              href={`/events/${promotion.slug}`}
              className="flex gap-3 p-2 -mx-2 rounded-lg hover:bg-surface transition-colors group"
            >
              <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                               flex items-center justify-center">
                <Tag size={15} strokeWidth={1.75} className="text-muted" />
              </span>

              <span className="min-w-0 flex-1">
                {/* The end date, not the start. A voucher that began six weeks
                    ago has nothing useful in its start date; what a reader
                    needs is how long is left. */}
                <span className="block text-[10px] text-muted mb-0.5">
                  {eventDate(promotion.endsAt ?? promotion.startsAt)}까지
                </span>

                <span className="block text-[13px] leading-snug text-snow
                                 group-hover:text-white transition-colors line-clamp-2">
                  {promotion.title}
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </aside>
  );
}