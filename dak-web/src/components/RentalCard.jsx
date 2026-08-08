import { MapPin, ImageOff, CalendarCheck, Link2 } from 'lucide-react';
import { sized } from '@/utils/image';

const TYPE_LABELS = {
  SHARE_ROOM: '방 임대',
  WHOLE_PROPERTY: '집 전체',
  LEASE_TRANSFER: '계약 승계',
  STUDENT_ACCOMMODATION: '학생 숙소',
};

const BILLS_LABELS = {
  INCLUDED: '빌 포함',
  EXCLUDED: '빌 별도',
  OPTIONAL: '빌 선택',
};

/**
 * Server component, matching EventCard.
 *
 * Led by the rent, which is the one number every reader compares first and
 * the reason they are on the page at all. Everything else on the card exists
 * to say whether that number is comparable to the one beside it - bills
 * included or not changes a $300 room into a $350 one.
 */
export default function RentalCard({ rental }) {
  const rent = rental.rentMax && rental.rentMax !== rental.rentMin
    ? `$${rental.rentMin}~$${rental.rentMax}`
    : `$${rental.rentMin}`;

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

      {/* The frame is always here, with or without a photograph in it. Most
          listings will not have one: an image needs the advertiser's
          permission, and most are recorded from an advertisement with nobody
          reachable behind it. Same treatment as EventCard, so a listing
          without a picture reads as ordinary rather than broken. */}
      <div className="w-full aspect-[16/9] max-h-48 rounded-xl mb-3 bg-surface
                      overflow-hidden flex items-center justify-center">
        {rental.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={sized(rental.thumbnailUrl, 600)}
            alt=""
            aria-hidden="true"
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <ImageOff size={24} strokeWidth={1.5} className="text-border-dark" aria-hidden="true" />
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2 mb-2">
        {/* Said on the card rather than only on the page. A reader scanning a
            list is deciding which one to open, and whether DAK has spoken to
            the advertiser changes what opening it will get them. */}
        {rental.consentStatus !== 'FULL' && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark
                           text-muted inline-flex items-center gap-1">
            <Link2 size={11} strokeWidth={2} />
            외부 매물
          </span>
        )}
        <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
          {TYPE_LABELS[rental.listingType] ?? rental.listingType}
        </span>
        {BILLS_LABELS[rental.billsIncluded] && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark text-muted">
            {BILLS_LABELS[rental.billsIncluded]}
          </span>
        )}
        {rental.furnished && (
          <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark text-muted">
            가구 포함
          </span>
        )}
      </div>

      {/* The rent leads, in the largest type on the card. Everything a reader
          does on this page is a comparison of these numbers. */}
      <div className="flex items-baseline gap-1.5 mb-1.5">
        <span className="text-xl font-bold text-snow">{rent}</span>
        <span className="text-[13px] text-muted">/ 주</span>
      </div>

      <h3 className="text-[14px] text-snow leading-snug mb-2.5 line-clamp-2">
        {rental.title}
      </h3>

      <div className="flex flex-col gap-1.5 text-[13px] text-muted">
        <div className="flex items-center gap-2">
          <MapPin size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
          <span className="truncate">{rental.suburb}</span>
        </div>

        {/* The row is always here, with or without a date in it. Rendering it
            only where one exists made a card without an availability date sit
            shorter than the ones beside it, and the gap read as something that
            failed to load rather than as a fact about the listing. The same
            reasoning EventCard applies to its image frame.

            External listings often have no date because the advertisement did
            not give one, and inventing today's date would be DAK asserting
            something it has not checked. */}
        <div className="flex items-center gap-2">
          <CalendarCheck size={14} strokeWidth={1.75} className="shrink-0 text-faint" />
          <span className={`truncate ${rental.availableFrom ? '' : 'text-faint'}`}>
            {rental.availableFrom
              ? `${rental.availableFrom} 입주 가능`
              : '입주일 문의'}
          </span>
        </div>
      </div>
    </article>
  );
}