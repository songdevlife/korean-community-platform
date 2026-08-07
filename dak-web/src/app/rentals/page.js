import Link from 'next/link';
import { BedDouble, AlertTriangle } from 'lucide-react';
import { getRentals } from '@/api/server';
import PageShell from '@/components/PageShell';
import RentalCard from '@/components/RentalCard';
import PageLinks from '@/components/PageLinks';

export const metadata = {
  title: 'Rentals',
  description:
    '애들레이드 렌트와 쉐어하우스 정보. 주급, 본드, 빌 포함 여부와 임대차법 적용 여부를 함께 확인하세요.',
  alternates: { canonical: '/rentals' },
};

const TYPES = [
  { value: '', label: '전체' },
  { value: 'SHARE_ROOM', label: '방 임대' },
  { value: 'WHOLE_PROPERTY', label: '집 전체' },
  { value: 'LEASE_TRANSFER', label: '계약 승계' },
  { value: 'STUDENT_ACCOMMODATION', label: '학생 숙소' },
];

/**
 * Current rentals, newest first.
 *
 * A server component like the other listings, so the content is in the HTML a
 * crawler reads, and the type filter is a set of links rather than buttons so
 * a filtered view can be shared and indexed.
 */
export default async function RentalsPage({ searchParams }) {
  const params = await searchParams;
  const activeType = params?.type ?? '';
  const activePage = Math.max(0, Number(params?.page ?? 0));

  const data = await getRentals({ type: activeType || undefined, page: activePage });
  const rentals = data?.content ?? [];

  const chipClass = (active) =>
    `text-[13px] px-3 py-1.5 rounded-full border transition-colors whitespace-nowrap ${
      active
        ? 'bg-surface border-faint text-snow font-medium'
        : 'border-border-dark text-muted hover:text-snow hover:border-faint'
    }`;

  return (
    <PageShell>
      <div className="mb-5">
        <h1 className="text-xl font-bold text-snow">Rentals</h1>
        <p className="text-[13px] text-muted mt-1">
          애들레이드 렌트와 쉐어하우스
        </p>
      </div>

      {/* Said once, at the top, and not repeated on every card. Advance-payment
          fraud is the failure that costs a reader real money, and a warning
          they scroll past on a listing page is worth more than one they meet
          only after they have already sent it. */}
      <div className="rounded-2xl border border-border-dark bg-night p-4 mb-5">
        <div className="flex items-start gap-2.5">
          <AlertTriangle size={16} strokeWidth={2} className="shrink-0 mt-0.5 text-adelaide-red" />
          <div className="text-[13px] text-muted leading-relaxed">
            <p className="text-snow font-medium mb-1">돈부터 요구하면 의심하세요</p>
            <p>
            남호주에서 계약을 시작할 때 지불하는 금액은 본드와 선불 렌트 2주치까지입니다.
              본드는 집주인이 아니라 정부(CBS)가 보관합니다. 그 밖의 명목으로 미리 돈을
              요구한다면 한 번 더 확인해 보세요. 무료 상담은 RentRight SA(1800 060 462)에서
              받을 수 있습니다.
            </p>
            <Link
              href="/guides/adelaide-rent-share-house-tenant-rights-2026"
              className="inline-block text-korea-blue underline underline-offset-2 hover:opacity-80 mt-1.5"
            >
              렌트 권리 가이드 읽기
            </Link>
          </div>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1 mb-5 -mx-4 px-4 md:mx-0 md:px-0 md:flex-wrap">
        {TYPES.map((t) => (
          <Link
            key={t.value}
            href={t.value ? `/rentals?type=${t.value}` : '/rentals'}
            className={chipClass(activeType === t.value)}
          >
            {t.label}
          </Link>
        ))}
      </div>

      {rentals.length === 0 ? (
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <BedDouble size={22} strokeWidth={1.5} className="text-border-dark mx-auto mb-3" />
          <p className="text-snow font-medium mb-1">등록된 매물이 없습니다</p>
          <p className="text-[13px] text-muted leading-relaxed">
            {activeType
              ? '다른 분류도 확인해 보세요.'
              : '새로운 매물이 등록되면 이곳에 표시됩니다.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 items-start">
          {rentals.map((rental) => (
            <Link key={rental.id} href={`/rentals/${rental.slug}`}>
              <RentalCard rental={rental} />
            </Link>
          ))}
        </div>
      )}

      <PageLinks
        page={activePage}
        totalPages={data?.totalPages ?? 0}
        basePath="/rentals"
        params={{ type: activeType }}
      />

      {/* Listings expire after three weeks whether or not anyone says the room
          has gone, so this is also where an advertiser learns their listing
          will come down and how to keep it up. */}
      <div className="mt-8 rounded-2xl border border-border-dark bg-night p-5 text-center">
        <p className="text-[14px] text-snow font-medium mb-1.5">매물을 올리고 싶으신가요?</p>
        <p className="text-[13px] text-muted leading-relaxed">
          무료로 게시해 드립니다. 게시 기간은 3주이며, 그 전에 나가면 알려주세요.
        </p>
        <a
          href="mailto:admin@discoveradelaidekorea.au?subject=렌트 매물 등록"
          className="inline-block text-[13px] text-korea-blue underline underline-offset-2
                     hover:opacity-80 mt-1.5 break-all"
        >
          admin@discoveradelaidekorea.au
        </a>
      </div>
    </PageShell>
  );
}