import Link from 'next/link';
import { CalendarDays } from 'lucide-react';
import { getEvents, getEventCategories } from '@/api/server';
import PageShell from '@/components/PageShell';
import EventCard from '@/components/EventCard';

export const metadata = {
  title: 'Events',
  description:
    '애들레이드 한인 커뮤니티 행사 정보. 스포츠 동호회, 공연, 모임, 무료강좌를 안내합니다.',
  alternates: { canonical: '/events' },
};

/**
 * Upcoming events, soonest first.
 *
 * A server component like the other public listings, so the content is in the
 * HTML a crawler reads. The category filter is a set of links rather than
 * buttons for the same reason: a link is a URL, which means a filtered view
 * can be shared, bookmarked and indexed, and needs no JavaScript to work.
 */
export default async function EventsPage({ searchParams }) {
  const params = await searchParams;
  const activeCategory = params?.category ?? '';

  const [data, categories] = await Promise.all([
    getEvents({ category: activeCategory || undefined }),
    getEventCategories(),
  ]);

  const events = data?.content ?? [];

  const chipClass = (active) =>
    `text-[13px] px-3 py-1.5 rounded-full border transition-colors whitespace-nowrap ${
      active
        ? 'bg-surface border-faint text-snow font-medium'
        : 'border-border-dark text-muted hover:text-snow hover:border-faint'
    }`;

  return (
    <PageShell>
      <div className="mb-5">
        <h1 className="text-xl font-bold text-snow">Events</h1>
        <p className="text-[13px] text-muted mt-1">
          애들레이드에서 열리는 행사와 모임
        </p>
      </div>

      {/* Horizontal scroll below the breakpoint rather than wrapping to three
          rows on a phone. */}
      <div className="flex gap-2 overflow-x-auto pb-1 mb-5 -mx-4 px-4 md:mx-0 md:px-0 md:flex-wrap">
        <Link href="/events" className={chipClass(!activeCategory)}>
          전체
        </Link>
        {(categories ?? []).map((c) => (
          <Link
            key={c.id}
            href={`/events?category=${c.slug}`}
            className={chipClass(activeCategory === c.slug)}
          >
            {c.name}
          </Link>
        ))}
      </div>

      {events.length === 0 ? (
        // Says what is true rather than that nothing exists: an empty category
        // usually means nothing is scheduled this month, not that the section
        // is unused.
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <CalendarDays size={22} strokeWidth={1.5} className="text-border-dark mx-auto mb-3" />
          <p className="text-snow font-medium mb-1">예정된 행사가 없습니다</p>
          <p className="text-[13px] text-muted leading-relaxed">
            {activeCategory
              ? '다른 분류도 확인해 보세요.'
              : '새로운 행사가 등록되면 이곳에 표시됩니다.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {events.map((event) => (
            <Link key={event.id} href={`/events/${event.id}`}>
              <EventCard event={event} />
            </Link>
          ))}
        </div>
      )}

      {/* Below the list rather than above it, and present whether or not the
          list is empty. Submissions are by email because there is no form and
          no moderation queue for one - and while that is the arrangement, an
          organiser who cannot find the address simply does not get listed.
          The channel matters more than the mechanism: an organiser who writes
          in once tends to write in again. */}
      <div className="mt-8 rounded-2xl border border-border-dark bg-night p-5 text-center">
        <p className="text-[14px] text-snow font-medium mb-1.5">행사를 알리고 싶으신가요?</p>
        <p className="text-[13px] text-muted leading-relaxed">
          
          행사 또는 모임을 무료로 게시해 드립니다.<br className="hidden sm:inline" />
          행사 정보나 문의 사항을{' '}
          <a
            href="mailto:admin@discoveradelaidekorea.au?subject=행사 등록 문의"
            className="text-korea-blue underline underline-offset-2 hover:opacity-80 break-all"
          >
            admin@discoveradelaidekorea.au
          </a>
          {' '}로 보내 주세요.
        </p>
      </div>
    </PageShell>
  );
}