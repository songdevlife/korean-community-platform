'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Home, Search, LayoutGrid, BookOpen, Newspaper, User, LogOut, Scale } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: 'Home', Icon: Home },
  { to: '/search', label: 'Search', Icon: Search },
  { to: '/directory', label: 'Businesses', Icon: LayoutGrid },
  // Guides sits before AU Updates: reference material that stays useful, ahead
  // of time-sensitive news.
  { to: '/guides', label: 'Guides', Icon: BookOpen },
  { to: '/australia-updates', label: 'AU Updates', Icon: Newspaper },
];

// The tab bar drops Businesses. Six items left every label cramped, and
// Businesses is the one with the most alternative routes in: Home features
// listings with a View all, and Search now has a Businesses tab of its own.
// The sidebar has room, so it keeps the full set.
const TAB_BAR_EXCLUDED = ['/directory'];

function Layout({ children }) {
  // usePathname and useRouter replace react-router's useLocation and
  // useNavigate. Both are client-only hooks, which is why this file carries
  // 'use client' — the nav has to know the current route and the signed-in
  // user, neither of which exists during a server render.
  const pathname = usePathname();
  const router = useRouter();
  const { user, signOut } = useAuth();

  // Exact match for home; prefix match elsewhere so detail pages keep
  // their section highlighted (e.g. /businesses/xyz under Directory).
  const isActive = (to) =>
    to === '/' ? pathname === '/' : pathname.startsWith(to);

// Signing out from a page that requires a session would otherwise leave the
  // user staring at a "you need to log in" screen they just chose to leave.
  async function handleLogout() {
    await signOut();
    router.push('/');
  }

  const myPageLink = user
    ? { to: '/dashboard', label: 'My Page', Icon: User }
    : { to: '/login', label: 'Log in', Icon: User };

  // My Page belongs in the nav list rather than only in the account footer:
  // it is a destination like any other, and the mobile tab bar has always
  // treated it that way. The footer keeps the identity and sign-out controls.
  const sidebarItems = [...NAV_ITEMS, myPageLink];
  const tabBarItems = sidebarItems.filter((item) => !TAB_BAR_EXCLUDED.includes(item.to));

  return (
    <div className="min-h-screen bg-night flex">
      {/* Desktop sidebar (>=768px), per 06 UI/UX 6.1. Sticky so the nav stays
          reachable from partway down a long list. */}
      <aside className="hidden md:flex md:flex-col w-56 bg-night p-4 shrink-0
                        sticky top-0 h-screen overflow-y-auto">
        <Link href="/" className="mb-8 flex items-center gap-2.5 group">
          {/* Served from public/ rather than imported. Plain img rather than
              next/image: the mark is a fixed 32px and already small, so the
              optimisation pipeline would add a request without saving bytes. */}
          <img
            src="/logo-mark-dark.png"
            alt=""
            aria-hidden="true"
            className="w-8 h-8 object-contain shrink-0 opacity-90
                       group-hover:opacity-100 transition-opacity duration-300"
          />
          <div>
            <span className="block text-lg font-bold text-snow leading-none">DAK</span>
            <span className="block text-[9px] tracking-[.06em] text-muted mt-1
                             group-hover:text-snow transition-colors duration-300">
              Discover Adelaide Korea
            </span>
          </div>
        </Link>

        <nav className="flex flex-col gap-1">
          {sidebarItems.map(({ to, label, Icon }) => (
            <Link
              key={to}
              href={to}
              aria-current={isActive(to) ? 'page' : undefined}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                isActive(to)
                  ? 'bg-surface text-snow font-medium'
                  : 'text-muted hover:text-snow hover:bg-surface/60'
              }`}
            >
              <Icon size={18} strokeWidth={1.75} className="shrink-0" />
              {label}
            </Link>
          ))}
        </nav>


{/* Legal sits at the foot of the sidebar rather than in the nav list:
            it is something people look for when they have a reason to, not
            something to browse. mt-auto pushes it down when signed out, where
            the account footer below would otherwise be doing that job. */}
        <Link
          href="/legal"
          className={`mt-auto flex items-center gap-3 px-3 py-2.5 rounded-lg text-[13px] transition-colors ${
            isActive('/legal')
              ? 'bg-surface text-snow font-medium'
              : 'text-faint hover:text-snow hover:bg-surface/60'
          }`}
        >
          <Scale size={16} strokeWidth={1.75} className="shrink-0" />
          Legal
        </Link>

        {/* Account footer. Identity and sign-out only — navigation to My Page
            now lives in the list above. */}
        {user && (
          <div className="pt-4 border-t border-border-dark">
            <span className="block px-3 pb-1 text-sm text-snow font-medium truncate">
              {user.displayName}
            </span>
            {/* Same padding, radius and hover as the nav links above, so the
                control reads as part of the same list rather than as loose text
                beneath it. */}
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-left
                         text-muted hover:text-snow hover:bg-surface/60 cursor-pointer
                         transition-colors"
            >
              <LogOut size={18} strokeWidth={1.75} className="shrink-0" />
              Log out
            </button>
          </div>
        )}
      </aside>

      {/* Main content. Bottom padding on mobile clears the fixed tab bar. */}
      <div className="flex-1 pb-20 md:pb-0 min-w-0">{children}</div>

      {/* Mobile tab bar (<768px). pb-safe keeps it clear of the iOS home
          indicator. Five items is the practical ceiling at this width — a
          marketplace section would need something else to move into Home or
          Search rather than being added alongside. */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-surface border-t border-border-dark flex justify-around pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] z-10">
        {tabBarItems.map(({ to, label, Icon }) => (
          <Link
            key={to}
            href={to}
            aria-current={isActive(to) ? 'page' : undefined}
            className={`flex flex-col items-center gap-1 px-2 text-[11px] transition-colors ${
              isActive(to) ? 'text-snow font-medium' : 'text-muted'
            }`}
          >
            <Icon size={20} strokeWidth={1.75} />
            {label}
          </Link>
        ))}
      </nav>
    </div>
  );
}

export default Layout;