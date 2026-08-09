import { Calendar, MapPin, ImageOff } from 'lucide-react';
import { eventDate, eventTime, daysUntil } from '@/utils/date';
import { sized } from '@/utils/image';

/**
 * Server component, matching GuideCard's treatment.
 *
 * Led by the date rather than the title, which is the difference between this
 * and every other card on the site. Guides and updates are read whenever
 * someone finds them; an event is a decision about a particular evening, and
 * the first thing a reader needs is whether they are free then.
 */
/** See the events detail page: a promotion is stored as an event but is not one. */
const PROMOTION_SLUG = 'promotions';

export default function EventCard({ event }) {
  const isPromotion = event.category?.slug === PROMOTION_SLUG;
  const days = daysUntil(event.startsAt);

  // Only for the next week. Beyond that "23일 뒤" is arithmetic rather than
  // urgency, and the date itself says more.
  const proximity =
    days === 0 ? '오늘' :
    days === 1 ? '내일' :
    days > 1 && days <= 7 ? `${days}일 뒤` : null;

  return (
    <article
      className="group relative bg-night rounded-2xl p-4 border border-border-dark overflow-hidden
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      <span
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                   bg-gradient-to-r from-transparent via-korea-blue to-transparent
                   group-hover:opacity-100 transition-opacity duration-300"
      />

      {/* The frame is always here, with or without a picture in it. Most
          events will never have one: a poster needs the organiser's
          permission, and most are transcribed from a post with nobody
          reachable behind it. Rendering the image only where it exists made
          the majority case look like the broken one — a short card beside a
          tall one, with the gap reading as something that failed to load.

          The icon rather than the words "이미지 없음": at eight cards in ten
          the same sentence repeated stops being a label and starts competing
          with the titles. It matches the empty state in Gallery, so one mark
          means "no image" everywhere on the site. */}
      <div className="w-full aspect-[16/9] max-h-48 rounded-xl mb-3 bg-surface
                      overflow-hidden flex items-center justify-center">
        {event.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={sized(event.thumbnailUrl, 600)}
            alt=""
            aria-hidden="true"
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <Calendar size={24} strokeWidth={1.5} className="text-border-dark" aria-hidden="true" />
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2 mb-2">
        {event.category && (
          <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
            {event.category.name}
          </span>
        )}
        {/* Not for a promotion. A voucher that started six weeks ago has no
            "3일 뒤" to show, and its start date is not what a reader needs. */}
        {!isPromotion && proximity && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-korea-blue/15 text-korea-blue font-medium">
            {proximity}
          </span>
        )}
        {/* Price sits with the other pills whatever its value. Free was a pill
            and an amount was a line at the far right of the venue row, which
            meant the same field moved depending on what it said — and a reader
            scanning several cards had to look in two places for one thing.

            Truncated rather than wrapped: a long note would push the pill row
            onto a second line and change the card's height. The detail page
            carries it in full. */}
        {(event.isFree || event.priceNote) && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark
                           text-muted max-w-[45%] truncate">
            {event.isFree ? '무료' : event.priceNote}
          </span>
        )}
      </div>

      <h3 className="font-semibold text-snow leading-snug mb-2.5 line-clamp-2">
        {event.title}
      </h3>

      <div className="flex flex-col gap-1.5 text-[13px] text-muted">
      <div className="flex items-center gap-2">
          <Calendar size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
          <span className="truncate">
            {isPromotion
              ? `${eventDate(event.endsAt ?? event.startsAt)}까지`
              : `${eventDate(event.startsAt)} ${eventTime(event.startsAt)}`}
          </span>
        </div>

        {/* Venue and price share a line so the block is always two rows deep.
            Given its own row, a price made cards with one taller than cards
            without, which is a difference in layout standing for no
            difference in the event.

            The price is shrink-0 and the venue truncates: when a long price
            note competes with a long venue for the same line, the venue is
            what gives way. Somewhere to go matters more than what it costs,
            and the detail page carries both in full either way. */}
        {/* The row is always here, with or without a venue in it. Rendering it
            only where one exists made a card without a venue sit shorter than
            the ones beside it, and the gap read as something that failed to
            load rather than as a fact about the listing. Same treatment the
            image frame above already gets, and the same one RentalCard applies
            to its availability date.

            A promotion has no venue at all - a voucher is valid wherever the
            brand trades - so it says so rather than leaving the line blank. */}
        <div className="flex items-center gap-2">
          <MapPin size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
          <span className={`truncate ${event.venueName ? '' : 'text-faint'}`}>
            {event.venueName ?? (isPromotion ? '참여 매장' : '장소 미정')}
          </span>
        </div>
      </div>
    </article>
  );
}