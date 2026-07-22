import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: '홈', icon: '🏠' },
  { to: '/search', label: '검색', icon: '🔍' },
  { to: '/directory', label: '디렉토리', icon: '🏪' },
  { to: '/australia-updates', label: 'AU Updates', icon: '📰' },
];

function Layout({ children }) {
  const location = useLocation();
  const { user, clearSession } = useAuth();

  const myPageLink = user
    ? { to: '/dashboard', label: 'My Page', icon: '👤' }
    : { to: '/login', label: '로그인', icon: '👤' };

  const allItems = [...NAV_ITEMS, myPageLink];

  return (
    <div className="min-h-screen bg-cream flex">
      {/* 데스크톱 사이드바 (768px 이상) */}
      <aside className="hidden md:flex md:flex-col w-56 border-r border-gray-100 p-4 shrink-0">
        <Link to="/" className="text-lg font-bold text-ink mb-8">
          Discover<span className="text-adelaide-red">Adelaide</span>
          <span className="text-korea-blue">Korea</span>
        </Link>

        <nav className="flex flex-col gap-1">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm ${
                location.pathname === item.to
                  ? 'bg-korea-blue text-white font-medium'
                  : 'text-ink hover:bg-white'
              }`}
            >
              <span>{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="mt-auto">
          {user ? (
            <div className="flex flex-col gap-2 px-3 py-2 text-sm">
              <span className="text-ink font-medium">{user.displayName}님</span>
              <button onClick={clearSession} className="text-gray-400 text-left">
                로그아웃
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-korea-blue font-medium"
            >
              로그인
            </Link>
          )}
        </div>
      </aside>

      {/* 메인 콘텐츠 */}
      <div className="flex-1 pb-16 md:pb-0">{children}</div>

      {/* 모바일 하단 탭바 (768px 미만) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 flex justify-around py-2 z-10">
        {allItems.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className={`flex flex-col items-center text-xs gap-0.5 px-2 ${
              location.pathname === item.to ? 'text-korea-blue font-medium' : 'text-gray-400'
            }`}
          >
            <span className="text-base">{item.icon}</span>
            {item.label}
          </Link>
        ))}
      </nav>
    </div>
  );
}

export default Layout;