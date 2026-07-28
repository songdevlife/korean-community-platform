import { timeAgo, isNew } from '@/utils/date';
import NewBadge from './NewBadge';

/**
 * Server component. The category tag renders as a plain label rather than a
 * filter control: an onClick handler cannot exist in a server component, and
 * making the whole card interactive would mean shipping the entire list as
 * JavaScript to keep one tag clickable. Filtering lives in the chips above the
 * list instead — the same capability, in one place rather than two.
 */
function GuideCard({ guide }) {
  return (
    <article
      className="group relative bg-night rounded-2xl p-4 border border-border-dark h-full overflow-hidden
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      {/* Gradient hairline that fades in along the top edge on hover */}
      <span
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                   bg-gradient-to-r from-transparent via-korea-blue to-transparent
                   group-hover:opacity-100 transition-opacity duration-300"
      />

      <div className="flex flex-wrap items-center gap-2 mb-2">
        {guide.category && (
          <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
            {guide.category.name}
          </span>
        )}
      </div>

      <h3 className="font-semibold text-snow leading-snug mb-2 flex items-start gap-2">
        <span className="min-w-0">{guide.title}</span>
        {isNew(guide.publishedAt) && <NewBadge />}
      </h3>

      {/* Summary is a stored field rather than a truncation of the body, so it
          reads as written prose instead of a cut-off markdown heading. */}
      {guide.summary && (
        <p className="text-[13px] text-muted leading-6 line-clamp-2 mb-3">
          {guide.summary}
        </p>
      )}

      <div className="flex items-center justify-end text-[11px] text-muted">
        <span className="shrink-0">{timeAgo(guide.publishedAt)}</span>
      </div>
    </article>
  );
}

export default GuideCard;