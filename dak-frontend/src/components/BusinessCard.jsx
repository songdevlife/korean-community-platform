import { BadgeCheck, ImageOff } from 'lucide-react';
import { isNew } from '../utils/date';
import NewBadge from './NewBadge';

/**
 * @param {boolean} [compact] Shorter image and tighter padding, for previews
 *   where several cards share the width rather than filling it. Directory uses
 *   the default; Home uses compact.
 */
function BusinessCard({ business, compact = false }) {
  return (
    <article
      className="group relative h-full rounded-2xl border border-border-dark bg-night overflow-hidden
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      {/* Gradient hairline that fades in along the top edge on hover.
          Sits above the card content via z-10 since the image area is opaque. */}
      <span
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 z-10 h-px opacity-0
                   bg-gradient-to-r from-transparent via-korea-blue to-transparent
                   group-hover:opacity-100 transition-opacity duration-300"
      />

      <div
        className={`${compact ? 'aspect-[3/2]' : 'aspect-[4/3]'}
                    bg-surface flex items-center justify-center overflow-hidden`}
      >
        {business.thumbnailUrl ? (
          <img
            src={business.thumbnailUrl}
            alt=""
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <ImageOff
            size={compact ? 18 : 22}
            strokeWidth={1.5}
            className="text-border-dark"
          />
        )}
      </div>

      <div className={compact ? 'p-3' : 'p-3.5'}>
        <div className="flex items-start gap-1.5">
          <h3
            className={`font-semibold text-snow leading-snug line-clamp-2 ${
              compact ? 'text-[14px]' : 'text-[15px]'
            }`}
          >
            {business.name}
          </h3>
          {business.verified && (
            <BadgeCheck
              size={compact ? 14 : 16}
              strokeWidth={2}
              className="text-korea-blue shrink-0 mt-0.5"
              aria-label="Verified business"
            />
          )}
          {isNew(business.createdAt) && <NewBadge />}
        </div>

        {business.suburb && (
          <p className={`text-muted mt-1 ${compact ? 'text-[12px]' : 'text-[13px]'}`}>
            {business.suburb}
          </p>
        )}
      </div>
    </article>
  );
}

export default BusinessCard;