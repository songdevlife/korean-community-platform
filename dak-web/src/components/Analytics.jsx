'use client';

import { useEffect } from 'react';
import { Suspense } from 'react';
import { usePathname, useSearchParams } from 'next/navigation';

// Paths that should not be counted. Admin work would otherwise show up as
// traffic, and on a site with a handful of readers the owner's own clicking
// around is enough to drown out everything real.
const IGNORED_PREFIXES = ['/admin', '/dashboard'];

function Tracker() {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (IGNORED_PREFIXES.some((prefix) => pathname.startsWith(prefix))) {
      return;
    }

    const query = searchParams.toString();
    const path = query ? `${pathname}?${query}` : pathname;

    // count.js is loaded async, so on the very first render it may not have
    // arrived yet. Poll briefly rather than dropping the pageview — the first
    // one is the arrival from search, which is the one worth having.
    let attempts = 0;
    const send = () => {
      if (window.goatcounter && typeof window.goatcounter.count === 'function') {
        window.goatcounter.count({ path });
        return;
      }
      if (attempts < 20) {
        attempts += 1;
        setTimeout(send, 250);
      }
    };
    send();
  }, [pathname, searchParams]);

  return null;
}

/**
 * Counts a pageview on every route change.
 *
 * The script is loaded with no_onload, so it never counts by itself — this
 * component is the only thing that calls count(). Without that the first view
 * would be counted twice: once by the script on load, once by this effect.
 *
 * useSearchParams forces everything below it into client-side rendering, so the
 * Suspense boundary keeps that contained to this component rather than opting
 * whole pages out of the server rendering the site was migrated for.
 */
export default function Analytics() {
  return (
    <Suspense fallback={null}>
      <Tracker />
    </Suspense>
  );
}