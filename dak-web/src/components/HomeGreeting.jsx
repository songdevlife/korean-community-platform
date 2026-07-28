'use client';

import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';

/**
 * Greeting line and sign-up prompt. Isolated because both depend on the session,
 * which only exists in the browser — keeping it here lets the rest of the home
 * page render on the server.
 */
export default function HomeGreeting() {
  const { user } = useAuth();

  return (
    <div className="flex items-center justify-between gap-4 mb-4">
      {/* Signed out, the mobile brand block above already names the site, so
          repeating a tagline here would say the same thing twice in two
          different wordings. The empty span keeps Sign up pinned right. */}
      <p className="text-sm text-muted truncate">
        {user ? `Welcome back, ${user.displayName}` : ''}
      </p>
     {/* Desktop only. On mobile the tab bar carries Log in at all times, so a
          second entry point on the home page alone is both redundant and, with
          the greeting text gone, visually unanchored. */}
      {!user && (
        <Link
          href="/login"
          className="hidden md:inline-block shrink-0 px-4 py-1.5 rounded-full bg-snow text-night
                     text-[13px] font-medium hover:bg-white transition-colors duration-300"
        >
          Sign up
        </Link>
      )}
    </div>
  );
}