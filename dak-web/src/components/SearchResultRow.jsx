import { BadgeCheck, ImageOff, BookOpen, Newspaper } from 'lucide-react';
import { isNew, timeAgo } from '@/utils/date';
import NewBadge from './NewBadge';

/**
 * Horizontal result row: small thumbnail left, text right. Search results are
 * scanned rather than browsed, so a compact row fits more per screen than the
 * card grid used on Directory.
 */
export function BusinessResultRow({ business }) {
  return (
    <article
      className="flex gap-3.5 p-3 rounded-2xl border border-border-dark bg-night
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      <div className="w-16 h-16 shrink-0 rounded-xl bg-surface flex items-center justify-center overflow-hidden">
        {business.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={business.thumbnailUrl}
            alt=""
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <ImageOff size={18} strokeWidth={1.5} className="text-border-dark" />
        )}
      </div>

      <div className="min-w-0 flex-1 self-center">
        <div className="flex items-start gap-1.5">
          <h3 className="font-semibold text-snow text-[15px] leading-snug line-clamp-1">
            {business.name}
          </h3>
          {business.verified && (
            <BadgeCheck
              size={15}
              strokeWidth={2}
              className="text-korea-blue shrink-0 mt-0.5"
              aria-label="Verified business"
            />
          )}
          {isNew(business.createdAt) && <NewBadge />}
        </div>

        {business.shortDescription && (
          <p className="text-[13px] text-muted mt-0.5 line-clamp-1">
            {business.shortDescription}
          </p>
        )}
        {business.suburb && (
          <p className="text-[12px] text-faint mt-0.5">{business.suburb}</p>
        )}
      </div>
    </article>
  );
}

/**
 * Guides and updates share a row shape: an icon where a business has its
 * thumbnail, since neither carries an image. The icon also tells a reader what
 * kind of result they are looking at without a text label.
 */
export function ArticleResultRow({ kind, title, summary, meta, isNewItem }) {
  const Icon = kind === 'guide' ? BookOpen : Newspaper;

  return (
    <article
      className="flex gap-3.5 p-3 rounded-2xl border border-border-dark bg-night
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      <div className="w-16 h-16 shrink-0 rounded-xl bg-surface flex items-center justify-center">
        <Icon size={20} strokeWidth={1.5} className="text-muted" />
      </div>

      <div className="min-w-0 flex-1 self-center">
        <div className="flex items-start gap-1.5">
          <h3 className="font-semibold text-snow text-[15px] leading-snug line-clamp-1">
            {title}
          </h3>
          {isNewItem && <NewBadge />}
        </div>

        {summary && (
          <p className="text-[13px] text-muted mt-0.5 line-clamp-1">{summary}</p>
        )}
        {meta && <p className="text-[12px] text-faint mt-0.5">{meta}</p>}
      </div>
    </article>
  );
}