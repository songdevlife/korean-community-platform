import Link from 'next/link';
import { Megaphone, ArrowRight } from 'lucide-react';

/**
 * A single notice on the home page, for something time-limited worth reading
 * now — a census, a deadline, a change in the rules.
 *
 * Deliberately not a modal. The home page is where search lands, and an
 * overlay covering it there is the exact pattern Google treats as an
 * intrusive interstitial. This sits in the layout instead, which costs
 * nothing in ranking and gains an internal link to the guide that a crawler
 * can follow.
 *
 * A server component, so the headline and the link are in the initial HTML.
 * That also means no dismissal state: storing "already seen" needs browser
 * storage, which would have to be disclosed in privacy-policy.md and would
 * undo the reason GoatCounter was chosen over GA4. Scrolling past it is
 * dismissal enough.
 *
 * expiresAt is the one piece of scheduling here, and it exists because the
 * alternative is remembering. A notice with a date in it — a census, a
 * deadline — becomes wrong the day after rather than merely stale, and
 * nothing in the code says so. Deleting the call in page.js is still how a
 * notice is removed early; this is what happens when nobody does.
 *
 * Not a CMS. One notice has been needed so far, and a table, two endpoints
 * and an admin form to manage something used twice a year is work better
 * spent on the register.
 *
 * @param {string} [expiresAt] YYYY-MM-DD. The notice stops rendering on this
 *   date. Compared in Adelaide time, since that is where the deadlines are.
 */
export default function HomeNotice({
    title,
    description,
    href,
    linkLabel = '더보기',
    expiresAt,
  }) {
    if (expiresAt) {
      // Server component, so this runs on the server clock rather than the
      // reader's — a phone set to Seoul does not hide a notice a day early.
      const today = new Date().toLocaleDateString('en-CA', {
        timeZone: 'Australia/Adelaide',
      });
      if (today >= expiresAt) return null;
    }
  
    return (
    <Link
      href={href}
      className="group relative flex items-start gap-3 rounded-2xl border border-border-dark
                 bg-night p-4 mb-6 overflow-hidden
                 hover:border-faint hover:-translate-y-0.5 transition-all duration-300"
    >
      <span
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                   bg-gradient-to-r from-transparent via-korea-blue to-transparent
                   group-hover:opacity-100 transition-opacity duration-300"
      />

      <Megaphone
        size={18}
        strokeWidth={1.75}
        aria-hidden="true"
        className="shrink-0 mt-0.5 text-korea-blue"
      />

      <div className="min-w-0 flex-1">
        <p className="text-[14px] font-semibold text-snow leading-snug">{title}</p>
        <p className="text-[13px] text-muted leading-relaxed mt-1">{description}</p>
      </div>

      <span className="hidden sm:flex items-center gap-1 shrink-0 self-center
                       text-[13px] text-korea-blue">
        {linkLabel}
        <ArrowRight
          size={14}
          strokeWidth={2}
          className="group-hover:translate-x-0.5 transition-transform duration-300"
        />
      </span>
    </Link>
  );
}