import Link from 'next/link';
import { Compass, CalendarDays, BookOpen, Newspaper, BedDouble, Search } from 'lucide-react';
import PageShell from '@/components/PageShell';

export const metadata = {
  title: '페이지를 찾을 수 없습니다',
  // Nothing here is worth indexing, and a 404 that appears in search results
  // is a search result that wastes the reader's click.
  robots: { index: false, follow: false },
};

const DESTINATIONS = [
  {
    href: '/rentals',
    label: 'Rentals',
    description: '애들레이드 렌트와 쉐어하우스',
    Icon: BedDouble,
  },
  {
    href: '/events',
    label: 'Events',
    description: '한인 행사와 모임',
    Icon: CalendarDays,
  },
  {
    href: '/guides',
    label: 'Guides',
    description: '비자, 세금, 집, 병원 안내',
    Icon: BookOpen,
  },
  {
    href: '/australia-updates',
    label: 'AU Updates',
    description: '알아두면 좋은 호주 소식',
    Icon: Newspaper,
  },
];

/**
 * The 404 for the whole site.
 *
 * Next.js ships an English default that says the page was not found and stops
 * there, which on a Korean-language site is two failures at once: the reader
 * cannot read it, and it gives them nowhere to go. Most arrivals here are one
 * of three cases - a link shared before a listing expired, an event that was
 * archived rather than left to pass, or a search result pointing at something
 * that has since moved - and all three are reading rather than browsing, so
 * the useful thing is a way back into the section they wanted.
 */
export default function NotFound() {
  return (
    <PageShell>
      <div className="max-w-2xl">
        <div className="flex items-center gap-3 mb-3">
          <Compass size={22} strokeWidth={1.75} className="text-faint shrink-0" />
          <h1 className="text-2xl font-bold text-snow leading-tight">
            페이지를 찾을 수 없습니다
          </h1>
        </div>

        <p className="text-[15px] text-muted leading-relaxed mb-2">
          주소가 잘못되었거나, 게시 기간이 끝나 내려간 글일 수 있습니다.
        </p>
        <p className="text-[14px] text-faint leading-relaxed mb-8">
          렌트 매물은 3주 뒤에 자동으로 내려가고, 지난 행사는 목록에서 빠집니다.
          찾으시던 내용이 있다면 아래에서 다시 찾아보세요.
        </p>

        <div className="grid gap-3 sm:grid-cols-2 mb-6">
          {DESTINATIONS.map(({ href, label, description, Icon }) => (
            <Link
              key={href}
              href={href}
              className="group rounded-2xl border border-border-dark bg-night p-4
                         hover:border-faint hover:-translate-y-1 transition-all duration-300"
            >
              <div className="flex items-start gap-3">
                <Icon size={18} strokeWidth={1.75} className="shrink-0 mt-0.5 text-faint" />
                <div className="min-w-0">
                  <p className="text-[15px] font-medium text-snow mb-0.5">{label}</p>
                  <p className="text-[13px] text-muted leading-relaxed">{description}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>

        {/* Search last rather than first. Someone who followed a dead link
            usually wants the section it was in, not a box to retype a title
            they may not remember. */}
        <div className="flex flex-wrap items-center gap-3">
          <Link
            href="/search"
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-border-dark
                       text-[14px] text-muted hover:text-snow hover:border-faint transition-colors"
          >
            <Search size={16} strokeWidth={1.75} />
            검색하기
          </Link>
          <Link
            href="/"
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-korea-blue
                       text-white text-[14px] font-medium hover:bg-korea-blue/85 transition-colors"
          >
            홈으로
          </Link>
        </div>

        <p className="text-[12px] text-faint leading-relaxed mt-8 pt-5 border-t border-border-dark">
          찾으시던 페이지가 있어야 할 것 같다면 알려주세요.{' '}
          <a
            href="mailto:admin@discoveradelaidekorea.au?subject=404"
            className="text-korea-blue underline underline-offset-2 hover:opacity-80 break-all"
          >
            admin@discoveradelaidekorea.au
          </a>
        </p>
      </div>
    </PageShell>
  );
}