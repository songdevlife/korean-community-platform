import { Calendar, MapPin, Ticket } from 'lucide-react';
import { eventDate, eventTime, daysUntil } from '@/utils/date';

/**
 * Server component, matching GuideCard's treatment.
 *
 * Led by the date rather than the title, which is the difference between this
 * and every other card on the site. Guides and updates are read whenever
 * someone finds them; an event is a decision about a particular evening, and
 * the first thing a reader needs is whether they are free then.
 */
export default function EventCard({ event }) {
  const days = daysUntil(event.startsAt);

  // Only for the next week. Beyond that "23일 뒤" is arithmetic rather than
  // urgency, and the date itself says more.
  const proximity =
    days === 0 ? '오늘' :
    days === 1 ? '내일' :
    days > 1 && days <= 7 ? `${days}일 뒤` : null;

  return (
    <article
      className="group relative bg-night rounded-2xl p-4 border border-border-dark h-full overflow-hidden
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      <span
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                   bg-gradient-to-r from-transparent via-korea-blue to-transparent
                   group-hover:opacity-100 transition-opacity duration-300"
      />

      <div className="flex flex-wrap items-center gap-2 mb-2">
        {event.category && (
          <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
            {event.category.name}
          </span>
        )}
        {proximity && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-korea-blue/15 text-korea-blue font-medium">
            {proximity}
          </span>
        )}
        {event.isFree && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark text-muted">
            무료
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
            {eventDate(event.startsAt)} {eventTime(event.startsAt)}
          </span>
        </div>

        {event.venueName && (
          <div className="flex items-center gap-2">
            <MapPin size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
            <span className="truncate">{event.venueName}</span>
          </div>
        )}

        {/* Inside the same block as the date and venue rather than below it,
            so the three sit on one icon column. Only where there is something
            to say: a free event already carries its own tag above, and a blank
            line where a price would be reads as missing information. */}
        {!event.isFree && event.priceNote && (
          <div className="flex items-center gap-2">
            <Ticket size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
            <span className="truncate">{event.priceNote}</span>
          </div>
        )}
      </div>
    </article>
  );
}