import Link from 'next/link';

const LEGAL_LINKS = [
  { href: '/legal/terms-of-service', label: '이용약관' },
  { href: '/legal/privacy-policy', label: '개인정보처리방침' },
  { href: '/legal/content-policy', label: '콘텐츠 정책' },
  { href: '/legal/ai-usage-policy', label: 'AI 사용 방침' },
];

/**
 * Site footer. A server component — nothing here changes with the reader.
 *
 * Exists mostly to put the legal documents somewhere every visitor can reach
 * them. Until now they were in the desktop sidebar and on the account page,
 * which between them miss a signed-out reader on a phone — the largest group
 * on the site. The contact address is here for the same reason: an organiser
 * or a business owner who wants to be listed should not have to be on the
 * events page to find out how.
 */
export default function SiteFooter() {
  return (
    <footer className="mt-16 pt-6 border-t border-border-dark">
      <div className="flex flex-col items-center text-center gap-4 text-[13px]">

        <div>
          <p className="text-snow font-semibold">Discover Adelaide Korea</p>
          <p className="text-muted mt-0.5">애들레이드 한인을 위한 지역 정보</p>
        </div>

        <p className="text-muted">
          문의 · 제보{' '}
          <a
            href="mailto:admin@discoveradelaidekorea.au"
            className="text-korea-blue underline underline-offset-2 hover:opacity-80 break-all"
          >
            admin@discoveradelaidekorea.au
          </a>
        </p>

        <nav className="flex flex-wrap justify-center gap-x-4 gap-y-1.5">
          {LEGAL_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-muted hover:text-snow transition-colors"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        {/* No "all rights reserved". Some of what is published here is not
            DAK's to reserve: ACCC material is CC BY and carries its own terms
            on the page, and the source articles behind Australia Updates
            belong to their publishers. The Korean summaries and guides are
            DAK's own, which is what the notice covers. */}
        <p className="text-faint text-[12px] leading-relaxed">
          © {new Date().getFullYear()} Discover Adelaide Korea<br />
          일부 콘텐츠는 원저작자의 라이선스를 따릅니다.
        </p>
      </div>
    </footer>
  );
}