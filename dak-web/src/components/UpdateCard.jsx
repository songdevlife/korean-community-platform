import { Sparkles } from 'lucide-react';
import { timeAgo, isNew } from '@/utils/date';
import NewBadge from './NewBadge';

/**
 * Server component. Both tags render as plain labels rather than filter
 * controls: an onClick handler cannot exist in a server component, and making
 * the card interactive would mean shipping the whole list as JavaScript to keep
 * two tags clickable. Filtering lives in the controls above the list instead.
 */
function UpdateCard({ update }) {
  const tagBase =
    'text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full';

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
        {update.category && (
          <span className={`${tagBase} text-snow`}>{update.category.name}</span>
        )}
        {update.geographicScope && (
          <span className={`${tagBase} text-muted`}>{update.geographicScope}</span>
        )}
      </div>

      <h3 className="font-semibold text-snow leading-snug mb-2 flex items-start gap-2">
        <span className="min-w-0">{update.title}</span>
        {isNew(update.createdAt) && <NewBadge />}
      </h3>

      {/* Opening lines of the administrator's Korean summary. Titles here are
          the publisher's English headline, so without this a Korean-speaking
          reader has nothing in their own language to judge the item by —
          which is the whole point of the section. Matches GuideCard's clamp. */}
      {update.koreanSummary && (
        <p className="text-[13px] text-muted leading-6 line-clamp-2 mb-3">
          {update.koreanSummary}
        </p>
      )}

      <div className="flex items-center justify-between gap-3 text-[11px] text-muted">
        {/* AI disclosure is required wherever AI-generated content appears —
            03 MVP AI constraints, 06 UI/UX 6.3 */}
        {update.aiGenerated ? (
          <span className="flex items-center gap-1.5">
            <Sparkles size={12} strokeWidth={2} className="shrink-0" />
            AI summary
          </span>
        ) : (
          <span />
        )}
        <span className="shrink-0">{timeAgo(update.createdAt)}</span>
      </div>
    </article>
  );
}

export default UpdateCard;