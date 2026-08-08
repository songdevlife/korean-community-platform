import { notFound, permanentRedirect } from 'next/navigation';
import Link from 'next/link';
import {
  ArrowLeft, MapPin, Wallet, CalendarCheck, Clock, Sofa,
  ExternalLink, Phone, ShieldCheck, ShieldAlert, ShieldQuestion, Eye, Link2,
} from 'lucide-react';
import { getRentalById } from '@/api/server';
import PageShell from '@/components/PageShell';
import { displayHost, isUrl } from '@/utils/url';
import Gallery from '@/components/Gallery';

const TYPE_LABELS = {
  SHARE_ROOM: '방 임대',
  WHOLE_PROPERTY: '집 전체',
  LEASE_TRANSFER: '계약 승계',
  STUDENT_ACCOMMODATION: '학생 숙소',
};

const BILLS_LABELS = {
  INCLUDED: '빌 포함',
  EXCLUDED: '빌 별도',
  OPTIONAL: '빌 선택 가능',
  UNKNOWN: '빌 정보 없음',
};

/**
 * What the Residential Tenancies Act says about a property with this many
 * rooms let, in the terms a reader can act on.
 *
 * This is the one thing on the page no other listing site carries, and the
 * reason the rentals section is worth building rather than pointing at
 * Gumtree. The number of rooms let decides whether a bond must be lodged with
 * CBS, whether sixty days' notice is required to remove someone, and whether
 * the occupant can go to SACAT at all - and almost nobody arriving from Korea
 * knows the question exists.
 */
const TENANCY = {
  ROOMING_HOUSE: {
    Icon: ShieldCheck,
    tone: 'text-korea-blue',
    title: '임대차법의 보호를 받습니다',
    body: '방 2개 이상을 세놓는 집이라 법적으로 rooming house에 해당합니다. '
        + '본드는 CBS에 예치되어야 하고, 내보내려면 60일 통지와 법정 사유가 필요하며, '
        + '방에 잠금장치가 있어야 합니다. 분쟁은 SACAT에서 다룹니다.',
  },
  REGISTERED_ROOMING_HOUSE: {
    Icon: ShieldCheck,
    tone: 'text-korea-blue',
    title: '임대차법의 보호를 받으며, 등록 의무가 있는 집입니다',
    body: '방 5개 이상을 세놓는 집은 designated rooming house로, 운영자가 CBS에 '
        + '등록해야 합니다. 등록 없이 운영하는 것은 위법이므로 들어가기 전에 CBS(131 882)에 '
        + '확인해 볼 수 있습니다. 본드 예치, 60일 통지, 잠금장치 의무는 동일하게 적용됩니다.',
  },
  OUTSIDE_ACT: {
    Icon: ShieldAlert,
    tone: 'text-adelaide-red',
    title: '임대차법이 적용되지 않습니다',
    body: '집주인이 사는 집에서 방 1개만 세놓는 경우는 임대차법 5조(b)에 따라 법 적용 '
        + '대상이 아닙니다. 본드 예치 의무도, 통지 기간 규정도 없고, 분쟁이 생겨도 SACAT에 '
        + '갈 수 없습니다. 계약서가 유일한 보호막이므로 금액·기간·통지 기간·보증금 반환 조건을 '
        + '서면으로 남기고 계좌이체로 지불하세요.',
  },
  UNKNOWN: {
    Icon: ShieldQuestion,
    tone: 'text-muted',
    title: '법적 지위를 확인해야 합니다',
    body: '이 집에서 몇 개의 방을 세놓는지 광고에 나와 있지 않습니다. 방 2개 이상이면 '
        + '임대차법의 보호를 받고, 집주인이 사는 집에서 방 1개만 세놓는 경우라면 법 밖입니다. '
        + '보러 갈 때 세어보거나 직접 물어보세요. 이 질문 하나가 나중에 법적으로 본드를 돌려받을 수 '
        + '있는지를 가릅니다.',
  },
};

export async function generateMetadata({ params }) {
  const { slug } = await params;
  const rental = await getRentalById(slug);

  if (!rental) return { title: 'Rental not found' };

  const rent = rental.rentMax && rental.rentMax !== rental.rentMin
    ? `$${rental.rentMin}~$${rental.rentMax}`
    : `$${rental.rentMin}`;

  const description = `${rental.suburb} · 주 ${rent} · `
    + `${TYPE_LABELS[rental.listingType] ?? rental.listingType}`;

  return {
    title: rental.title,
    description,
    alternates: { canonical: `/rentals/${rental.slug}` },
    openGraph: { title: rental.title, description, type: 'article' },
  };
}

export default async function RentalPage({ params }) {
  const { slug } = await params;
  const rental = await getRentalById(slug);

  if (!rental) notFound();

  if (rental.slug && slug !== rental.slug) {
    permanentRedirect(`/rentals/${rental.slug}`);
  }

  const rent = rental.rentMax && rental.rentMax !== rental.rentMin
    ? `$${rental.rentMin}~$${rental.rentMax}`
    : `$${rental.rentMin}`;

  const tenancy = TENANCY[rental.tenancyStatus] ?? TENANCY.UNKNOWN;
  const TenancyIcon = tenancy.Icon;

  const hasImages = rental.images?.length > 0;
  const rowClass = 'flex items-start gap-3 text-[14px]';
  const iconClass = 'shrink-0 mt-0.5 text-faint';

  return (
    <PageShell>
      <Link
        href="/rentals"
        className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
      >
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span>Rentals</span>
      </Link>

      <div className="flex flex-col lg:flex-row gap-6">

        {hasImages && (
          <div className="xl:w-96 xl:shrink-0">
            <Gallery images={rental.images} alt={`${rental.title} 사진`} />
          </div>
        )}

        <article className="flex-1 min-w-0">
          {/* Said before anything else. Someone arriving from a link shared
              three weeks ago should not read the whole page before learning
              the room has almost certainly gone. */}
          {rental.hasExpired && (
            <div className="rounded-xl border border-border-dark bg-surface px-4 py-3 mb-5">
              <p className="text-[13px] text-muted">
                게시 기간이 지난 매물입니다. 이미 나갔을 가능성이 높습니다.
              </p>
            </div>
          )}

          {/* Above the title, because it changes how everything below it
              should be read. DAK undertakes in content-policy.md section 18
              to make this distinction visible rather than leave a reader to
              assume the advertiser has been spoken to. */}
          {rental.consentStatus !== 'FULL' && (
            <div className="rounded-xl border border-border-dark bg-night px-4 py-3 mb-5">
              <div className="flex items-start gap-2.5">
                <Link2 size={16} strokeWidth={2} className="shrink-0 mt-0.5 text-faint" />
                <div className="min-w-0">
                  <p className="text-[14px] text-snow font-medium mb-1">
                    다른 곳에 올라온 매물을 정리한 것입니다
                  </p>
                  <p className="text-[13px] text-muted leading-relaxed">
                    DAK가 게시자와 연락한 적이 없고, 매물이 아직 나와 있는지 확인할 수
                    없습니다. 아래 원문에서 직접 확인하고 문의해 주세요.
                    {rental.lastCheckedAt && (
                      <> 마지막으로 확인한 날짜는 {rental.lastCheckedAt.slice(0, 10)}입니다.</>
                    )}
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2 mb-3">
            <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
              {TYPE_LABELS[rental.listingType] ?? rental.listingType}
            </span>
            {rental.furnished && (
              <span className="text-[11px] px-2.5 py-0.5 rounded-full border border-border-dark text-muted">
                가구 포함
              </span>
            )}
          </div>

          <h1
            className={`text-2xl font-bold leading-tight mb-2 ${
              rental.hasExpired ? 'text-muted' : 'text-snow'
            }`}
          >
            {rental.title}
          </h1>

          <div className="flex items-baseline gap-1.5 mb-5">
            <span className="text-2xl font-bold text-snow">{rent}</span>
            <span className="text-[14px] text-muted">/ 주</span>
          </div>

          {/* The part of this page that does not exist anywhere else. Placed
              above the description rather than in the rail: it is not a
              detail of the listing, it is what the listing means. */}
          <div className="rounded-xl border border-border-dark bg-night p-4 mb-6">
            <div className="flex items-start gap-2.5">
              <TenancyIcon size={18} strokeWidth={2} className={`shrink-0 mt-0.5 ${tenancy.tone}`} />
              <div className="min-w-0">
                <p className="text-[14px] text-snow font-medium mb-1.5">{tenancy.title}</p>
                <p className="text-[13px] text-muted leading-relaxed">{tenancy.body}</p>
                <Link
                  href="/guides/adelaide-rent-share-house-tenant-rights-2026"
                  className="inline-block text-[13px] text-korea-blue underline underline-offset-2
                             hover:opacity-80 mt-2"
                >
                  렌트 권리 가이드에서 자세히 보기
                </Link>
              </div>
            </div>
          </div>

          {rental.description && (
            <p className="text-[15px] leading-relaxed text-snow whitespace-pre-line break-words">
              {rental.description}
            </p>
          )}

          <p className="text-[12px] text-faint leading-relaxed mt-8 pt-5 border-t border-border-dark">
            DAK는 이 매물의 임대인이나 중개인이 아니며, 매물의 존재나 조건을 확인하지 않습니다.
            직접 확인하기 전에 어떤 명목으로도 돈을 보내지 마세요.
            무료 상담: RentRight SA 1800 060 462.
          </p>
        </article>

        <aside className="lg:w-80 lg:shrink-0 flex flex-col gap-3 order-first lg:order-last">

          <div className="rounded-xl border border-border-dark bg-night p-4 flex flex-col gap-3">
            <div className={rowClass}>
              <MapPin size={16} strokeWidth={1.75} className={iconClass} />
              <p className="text-snow">{rental.suburb}</p>
            </div>

            <div className={rowClass}>
              <Wallet size={16} strokeWidth={1.75} className={iconClass} />
              <div className="min-w-0">
                <p className="text-snow">{BILLS_LABELS[rental.billsIncluded] ?? rental.billsIncluded}</p>
                {rental.billsNote && (
                  <p className="text-[13px] text-muted mt-0.5">{rental.billsNote}</p>
                )}
                {rental.bondWeeks && (
                  <p className="text-[13px] text-muted mt-0.5">본드 {rental.bondWeeks}주치</p>
                )}
              </div>
            </div>

            {rental.availableFrom && (
              <div className={rowClass}>
                <CalendarCheck size={16} strokeWidth={1.75} className={iconClass} />
                <p className="text-snow">{rental.availableFrom} 입주 가능</p>
              </div>
            )}

            {rental.minTermMonths && (
              <div className={rowClass}>
                <Clock size={16} strokeWidth={1.75} className={iconClass} />
                <p className="text-snow">최소 {rental.minTermMonths}개월</p>
              </div>
            )}

            {/* Conditions the advertiser set, recorded as stated. Publishing
                one is not an assessment of whether it is lawful - see
                content-policy.md section 17. */}
            {(rental.genderPreference || rental.couplesAllowed === false
              || rental.petsAllowed === false || rental.smokingAllowed === false) && (
              <div className={rowClass}>
                <Sofa size={16} strokeWidth={1.75} className={iconClass} />
                <div className="min-w-0 flex flex-wrap gap-1.5">
                  {rental.genderPreference && (
                    <span className="text-[12px] px-2 py-0.5 rounded-full border border-border-dark text-muted">
                      {rental.genderPreference}
                    </span>
                  )}
                  {rental.couplesAllowed === false && (
                    <span className="text-[12px] px-2 py-0.5 rounded-full border border-border-dark text-muted">
                      커플 불가
                    </span>
                  )}
                  {rental.petsAllowed === false && (
                    <span className="text-[12px] px-2 py-0.5 rounded-full border border-border-dark text-muted">
                      반려동물 불가
                    </span>
                  )}
                  {rental.smokingAllowed === false && (
                    <span className="text-[12px] px-2 py-0.5 rounded-full border border-border-dark text-muted">
                      금연
                    </span>
                  )}
                </div>
              </div>
            )}

            {rental.inspectionNote && (
              <div className={rowClass}>
                <Eye size={16} strokeWidth={1.75} className={iconClass} />
                <p className="text-snow">{rental.inspectionNote}</p>
              </div>
            )}
          </div>

          {/* Contact where the advertiser agreed to it, the original
              advertisement where they did not. One or the other is always
              present: a listing without permission is refused without a
              source URL, since otherwise a reader has no way to enquire. */}
          {rental.contact ? (
            <div className="rounded-xl border border-border-dark p-4">
              <h2 className="text-sm font-semibold text-snow mb-3">연락처</h2>
              <div className="flex items-start gap-2 text-[13px]">
                <Phone size={14} strokeWidth={1.75} className="shrink-0 mt-0.5 text-faint" />
                {isUrl(rental.contact) ? (
                  <a
                    href={rental.contact}
                    target="_blank"
                    rel="noreferrer"
                    className="text-korea-blue hover:underline break-all"
                  >
                    {displayHost(rental.contact)}
                  </a>
                ) : (
                  <span className="text-snow break-all">{rental.contact}</span>
                )}
              </div>
            </div>
          ) : rental.sourceUrl && (
            <div className="rounded-xl border border-border-dark p-4">
              <h2 className="text-sm font-semibold text-snow mb-3">원문</h2>
              <a
                href={rental.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-start gap-2 text-[13px] text-korea-blue hover:underline"
              >
                <ExternalLink size={14} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                원문에서 문의하기
              </a>
              {displayHost(rental.sourceUrl) && (
                <span className="block text-[12px] text-faint pl-[22px] mt-0.5">
                  {displayHost(rental.sourceUrl)}
                </span>
              )}
              {/* Facebook shows the linked post with the rest of the group's
                  feed underneath it, so a reader arriving from here meets
                  several listings rather than the one they clicked. Said only
                  for Facebook: a Gumtree or Flatmates link opens on the
                  listing itself, and the same note there would be wrong. */}
              {displayHost(rental.sourceUrl)?.includes('facebook.com') && (
                <p className="text-[12px] text-faint mt-2 leading-relaxed">
                  페이스북 그룹으로 연결됩니다. 맨 위에 있는 글이 이 매물입니다.
                </p>
              )}
              <p className="text-[12px] text-faint mt-2 leading-relaxed">
                게시자의 동의를 받지 못해 연락처를 싣지 않았습니다. 원문에서 확인해 주세요.
              </p>
            </div>
          )}
        </aside>

      </div>
    </PageShell>
  );
}