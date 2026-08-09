import { notFound, permanentRedirect } from 'next/navigation';
import Link from 'next/link';
import {
  ArrowLeft, Calendar, MapPin, Ticket, User, ExternalLink, CalendarPlus,
} from 'lucide-react';
import { getEventById } from '@/api/server';
import PageShell from '@/components/PageShell';
import { eventDate, eventDateTime, eventTime } from '@/utils/date';
import { displayHost, isUrl } from '@/utils/url';
import { googleCalendarUrl } from '@/utils/calendar';
import Gallery from '@/components/Gallery';

/**
 * The category that changes how a listing is read.
 *
 * A promotion is stored as an event because the lifecycle is identical - a
 * window that ends, a listing that drops out, a page that survives - but it
 * is not one. It has no time of day, no venue, and nothing to diarise, and
 * saying otherwise reads as a defect: a voucher sheet showed a start time of
 * 12:00am and offered to add itself to a calendar.
 */
const PROMOTION_SLUG = 'promotions';

export async function generateMetadata({ params }) {
  const { slug } = await params;
  const event = await getEventById(slug);

  if (!event) return { title: 'Event not found' };

  const description = event.description
    ?.replace(/\s+/g, ' ')
    .slice(0, 160)
    .replace(/\S*$/, '')
    .trim();

  return {
    title: event.title,
    description,
    // The slug, not the id. Both addresses resolve so that links shared before
    // the change keep working, and this is what says which of them is the page.
    alternates: { canonical: `/events/${event.slug}` },
    openGraph: { title: event.title, description, type: 'article' },
  };
}

export default async function EventPage({ params }) {
  const { slug } = await params;
  const event = await getEventById(slug);

  if (!event) notFound();

  // Events were addressed by UUID until the slug migration, and those links
  // are in KakaoTalk threads and search results already. The backend resolves
  // either form; this sends the old one to the new one permanently, so a
  // crawler updates its record rather than indexing the same event twice.
  if (event.slug && slug !== event.slug) {
    permanentRedirect(`/events/${event.slug}`);
  }

  // Event rather than Article: this is a thing happening at a time and place,
  // and the structured data is what lets a search result say so.
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'Event',
    name: event.title,
    startDate: event.startsAt,
    inLanguage: 'ko',
    eventStatus: 'https://schema.org/EventScheduled',
    ...(event.endsAt && { endDate: event.endsAt }),
    ...(event.description && {
      description: event.description.replace(/\s+/g, ' ').slice(0, 200),
    }),
    ...(event.venueName && {
      location: {
        '@type': 'Place',
        name: event.venueName,
        ...(event.venueAddress && { address: event.venueAddress }),
      },
    }),
    ...(event.organiser && {
      organizer: { '@type': 'Organization', name: event.organiser },
    }),
    ...(event.isFree && {
      offers: { '@type': 'Offer', price: '0', priceCurrency: 'AUD' },
    }),
    // Search results show an image where one is given, which for an event is
    // most of what makes a result worth clicking. schema.org takes an array,
    // and the first is the one a result will use.
    ...(event.images?.length > 0 && {
      image: event.images.map((i) => i.imageUrl),
    }),
  };

  const rowClass = 'flex items-start gap-3 text-[14px]';
  const iconClass = 'shrink-0 mt-0.5 text-faint';
  const isPromotion = event.category?.slug === PROMOTION_SLUG;
  const calendarUrl = googleCalendarUrl(event);

  // Three columns only where there is an image to fill the third. Most events
  // have no poster, and an empty left column would narrow the description for
  // nothing.
  const hasImages = event.images?.length > 0;

  return (
    <PageShell>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
      />

      <Link
        href="/events"
        className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
      >
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span>Events</span>
      </Link>

      {/* Two columns above 1024px, matching the update detail. One narrow
          column left half a wide screen empty, and what filled it — the facts
          of when and where — is what a reader checks against their own diary
          rather than reads in sequence. */}
      <div className="flex flex-col lg:flex-row gap-6">

          {/* Own column above 1280px, where there is room for one beside the
              description and the facts. Below that it sits above the writing,
              which is where a poster belongs on a narrower screen anyway. */}
          {hasImages && (
            <div className="xl:w-96 xl:shrink-0">
              <Gallery images={event.images} alt={`${event.title} 포스터`} />
            </div>
          )}

              {/* No width cap, matching the update detail. A measure limit is better
            typography — sixty to eighty characters a line rather than the
            hundred-plus a wide screen gives — but applying it here and not
            there made the two pages look like different sites. Worth revisiting
            for both together rather than fixing one of them. */}
        <article className="flex-1 min-w-0">
          {/* Said before anything else, and in the same place the reader's eye
              already is. Someone arriving from a link shared weeks ago should
              not read the whole page before learning they missed it. */}
          {event.hasPassed && (
            <div className="rounded-xl border border-border-dark bg-surface px-4 py-3 mb-5">
              <p className="text-[13px] text-muted">종료된 행사입니다.</p>
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2 mb-3">
            {event.category && (
              <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
                {event.category.name}
              </span>
            )}
            {event.isFree && (
              <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-korea-blue/15 text-korea-blue font-medium">
                무료
              </span>
            )}
          </div>

          <h1
            className={`text-2xl font-bold leading-tight mb-5 ${
              event.hasPassed ? 'text-muted' : 'text-snow'
            }`}
          >
            {event.title}
          </h1>

          {/* break-words alongside pre-line: pre-line keeps the organiser's
              line breaks, but a long unbroken string — a pasted URL, most
              likely — has no break to keep and would otherwise widen the page
              past the viewport. */}
          {event.description && (
            <p className="text-[15px] leading-relaxed text-snow whitespace-pre-line break-words">
              {event.description}
            </p>
          )}

<p className="text-[12px] text-faint leading-relaxed mt-8 pt-5 border-t border-border-dark">
            {isPromotion ? (
              <>
                DAK는 판매자가 아니며 가격이나 조건을 보증하지 않습니다.
                조건은 예고 없이 바뀔 수 있으니 매장에서 확인해 주세요.
              </>
            ) : (
              <>
                DAK는 행사 주최자가 아니며 행사의 진행이나 내용을 보증하지 않습니다.
                참가 전 원문과 주최자를 통해 세부 사항을 확인해 주세요.
              </>
            )}
          </p>
        </article>

        {/* Facts rail. Stacks above the description below the breakpoint,
            since on a phone when and where is still the first thing wanted
            and there is no column to put it in. */}
        <aside className="lg:w-80 lg:shrink-0 flex flex-col gap-3 order-first lg:order-last">

          <div className="rounded-xl border border-border-dark bg-night p-4 flex flex-col gap-3">
            <div className={rowClass}>
              <Calendar size={16} strokeWidth={1.75} className={iconClass} />
              <div className="min-w-0">
                {isPromotion ? (
                  /* A window rather than a moment. The times are midnight and
                     one minute to midnight, which are storage artefacts rather
                     than facts about the offer. */
                  <p className="text-snow">
                    {eventDate(event.startsAt)}
                    {event.endsAt && ` ~ ${eventDate(event.endsAt)}`}
                  </p>
                ) : (
                  <>
                    <p className="text-snow">{eventDateTime(event.startsAt)}</p>
                    {event.endsAt && (
                      <p className="text-[13px] text-muted mt-0.5">
                        종료 {eventTime(event.endsAt)}
                      </p>
                    )}
                    {/* The events are in Adelaide; the readers are not all in
                        Adelaide, and some are reading from Korea before they
                        arrive. */}
                    <p className="text-[12px] text-faint mt-0.5">애들레이드 시간 기준</p>
                  </>
                )}
              </div>
            </div>

            {event.venueName && (
              <div className={rowClass}>
                <MapPin size={16} strokeWidth={1.75} className={iconClass} />
                <div className="min-w-0">
                  <p className="text-snow">{event.venueName}</p>
                  {event.venueAddress && (
                    <p className="text-[13px] text-muted mt-0.5">{event.venueAddress}</p>
                  )}
                </div>
              </div>
            )}

            {(event.isFree || event.priceNote) && (
              <div className={rowClass}>
                <Ticket size={16} strokeWidth={1.75} className={iconClass} />
                <p className="text-snow">{event.isFree ? '무료' : event.priceNote}</p>
              </div>
            )}

            {event.organiser && (
              <div className={rowClass}>
                <User size={16} strokeWidth={1.75} className={iconClass} />
                <div className="min-w-0">
                  <p className="text-snow">{event.organiser}</p>
                  {/* Rendered as a link where it is one. An Instagram address
                      printed as text is a thing to retype rather than a way to
                      reach anyone, and reaching the organiser is the only
                      reason it is here. */}
                  {event.organiserContact && (
                    isUrl(event.organiserContact) ? (
                      <a
                        href={event.organiserContact}
                        target="_blank"
                        rel="noreferrer"
                        className="text-[13px] text-korea-blue hover:underline mt-0.5 block break-all"
                      >
                        {displayHost(event.organiserContact)}
                      </a>
                    ) : (
                      <p className="text-[13px] text-muted mt-0.5 break-all">
                        {event.organiserContact}
                      </p>
                    )
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Not offered once the date has passed: adding a finished event to
              a diary is a control that does nothing. */}
          {/* Not for a promotion: a voucher valid for eleven weeks is not an
              appointment, and offering to diarise it suggests it is. */}
          {!event.hasPassed && !isPromotion && calendarUrl && (
            <a
              href={calendarUrl}
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-2 rounded-xl bg-korea-blue
                         text-white text-sm font-medium py-2.5
                         hover:bg-korea-blue/85 transition-colors"
            >
              <CalendarPlus size={16} strokeWidth={1.75} />
              캘린더에 추가
            </a>
          )}

          {/* Boxed like the source panel on an Australia Update, for the same
              reason: it is where the organiser answers questions and where
              anything transcribed wrongly can be checked. The host is shown
              because "원문 보기" alone does not say where it goes. */}
          {event.sourceUrl && (
            <div className="rounded-xl border border-border-dark p-4">
              <h2 className="text-sm font-semibold text-snow mb-3">Source</h2>
              <a
                href={event.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-start gap-2 text-[13px] text-korea-blue hover:underline"
              >
                <ExternalLink size={14} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                원문 보기
              </a>
              {displayHost(event.sourceUrl) && (
                <span className="block text-[12px] text-faint pl-[22px] mt-0.5">
                  {displayHost(event.sourceUrl)}
                </span>
              )}
            </div>
          )}
        </aside>

      </div>
    </PageShell>
  );
}