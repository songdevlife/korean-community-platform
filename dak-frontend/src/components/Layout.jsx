import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Home, Search, LayoutGrid, BookOpen, Newspaper, User } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import logoMark from '../assets/logo-mark-dark.png';

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
  const location = useLocation();
  const navigate = useNavigate();
  const { user, clearSession } = useAuth();

  // Exact match for home; prefix match elsewhere so detail pages keep
  // their section highlighted (e.g. /businesses/xyz under Directory).
  const isActive = (to) =>
    to === '/' ? location.pathname === '/' : location.pathname.startsWith(to);

  // Signing out from a page that requires a session would otherwise leave the
  // user staring at a "you need to log in" screen they just chose to leave.
  function handleLogout() {
    clearSession();
    navigate('/');
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
        <Link to="/" className="mb-8 flex items-center gap-2.5 group">
          <img
            src={logoMark}
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
              to={to}
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

        {/* Account footer. Identity and sign-out only — navigation to My Page
            now lives in the list above. */}
        {user && (
          <div className="mt-auto pt-4 border-t border-border-dark">
            <div className="flex flex-col gap-2 px-3 py-2 text-sm">
              <span className="text-snow font-medium truncate">{user.displayName}</span>
              <button
                onClick={handleLogout}
                className="text-muted text-left hover:text-snow transition-colors"
              >
                Log out
              </button>
            </div>
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
            to={to}
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