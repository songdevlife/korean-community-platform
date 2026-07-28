import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Heart, ShieldCheck, ChevronRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { fetchSavedItems } from '../api/savedItems';
import { resourceLink, resourceIcon } from '../utils/savedItems';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import logoMark from '../assets/logo-mark-dark.png';
import PageMeta from '../components/PageMeta';

const PREVIEW_COUNT = 4;

function DashboardPage() {
  const { user } = useAuth();
  const [savedItems, setSavedItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchSavedItems()
      .then((items) => setSavedItems((items ?? []).slice(0, PREVIEW_COUNT)))
      .catch((error) => console.error('Failed to load saved items:', error))
      .finally(() => setLoading(false));
  }, []);

  if (!user) {
    return (
      <Layout>
        <PageShell>
        <PageMeta title="My Page" noIndex />
          <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
            <img
              src={logoMark}
              alt=""
              aria-hidden="true"
              className="w-20 h-20 mx-auto mb-5 object-contain opacity-60"
            />
            <p className="text-snow font-medium mb-1">You're signed out</p>
            <p className="text-muted text-sm mb-5">
              Log in to see your saved businesses and updates.
            </p>
            <Link
              to="/login"
              className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                         hover:bg-korea-blue/85 transition-colors"
            >
              Log in
            </Link>
          </div>
        </PageShell>
      </Layout>
    );
  }
  // Admin entry point lives here rather than in the sidebar, so the tool is
  // not advertised to users who cannot open it. The real guard is server-side;
  // this only controls visibility.
  const isAdmin = user.role === 'ADMINISTRATOR';

  return (
    <Layout>
      <PageShell>
        <h1 className="text-xl font-bold text-snow mb-5">My Page</h1>

        {/* Account summary. Initial-letter avatar stands in until profile
            images exist (04 DB 8.10 media domain, not yet implemented). */}
        <div className="rounded-2xl border border-border-dark bg-night p-5 mb-4 flex items-center gap-4">
          <div className="w-14 h-14 shrink-0 rounded-full bg-korea-blue text-white
                          flex items-center justify-center text-xl font-bold">
            {user.displayName?.[0]?.toUpperCase() || '?'}
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-snow truncate">{user.displayName}</p>
            <p className="text-sm text-muted truncate">{user.email}</p>
          </div>
        </div>

        {isAdmin && (
          <Link
            to="/admin"
            className="rounded-2xl border border-border-dark bg-night p-4 mb-6
                       flex items-center gap-3 hover:border-faint transition-colors"
          >
            <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                             flex items-center justify-center">
              <ShieldCheck size={16} strokeWidth={1.75} className="text-korea-blue" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-medium text-snow">Admin</span>
              <span className="block text-[13px] text-muted">
                Review pending businesses and draft updates
              </span>
            </span>
            <ChevronRight size={16} strokeWidth={2} className="text-muted shrink-0" />
          </Link>
        )}

        <div className={`flex items-baseline justify-between gap-4 mb-3 ${isAdmin ? '' : 'mt-6'}`}>
          <h2 className="text-lg font-semibold text-snow">Saved items</h2>
          <Link
            to="/favourites"
            className="text-sm text-muted hover:text-snow transition-colors shrink-0"
          >
            View all
          </Link>
        </div>

        {loading ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {[0, 1, 2, 3].map((i) => (
              <div
                key={i}
                className="h-20 rounded-xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : savedItems.length === 0 ? (
          <div className="rounded-xl border border-border-dark bg-night p-8 text-center">
            <Heart size={22} strokeWidth={1.5} className="text-border-dark mx-auto mb-2" />
            <p className="text-muted text-sm">Nothing saved yet.</p>
            <p className="text-faint text-[13px] mt-1">
              Browse the{' '}
              <Link to="/directory" className="text-korea-blue hover:underline">
                directory
              </Link>{' '}
              to find businesses worth keeping.
            </p>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {savedItems.map((item) => {
              const Icon = resourceIcon(item.resourceType);

              const cardClass =
                'rounded-xl border border-border-dark bg-night p-3.5 flex items-start gap-2.5';

              const inner = (
                <>
                  <span className="w-8 h-8 shrink-0 rounded-lg bg-surface border border-border-dark
                                   flex items-center justify-center">
                    <Icon size={14} strokeWidth={1.75} className="text-muted" />
                  </span>
                  <div className="min-w-0">
                    <p className={`text-sm font-medium line-clamp-2 ${
                      item.available ? 'text-snow' : 'text-muted'
                    }`}>
                      {item.title}
                    </p>
                    {!item.available && (
                      <p className="text-[12px] text-faint mt-0.5">
                        더 이상 공개되지 않는 항목입니다
                      </p>
                    )}
                  </div>
                </>
              );

              // Cards were previously inert, which reads as a broken control on
              // a page where everything else navigates. Unavailable rows stay
              // inert deliberately, and say why.
              return item.available ? (
                <Link
                  key={item.id}
                  to={resourceLink(item)}
                  className={`${cardClass} hover:border-faint transition-colors`}
                >
                  {inner}
                </Link>
              ) : (
                <div key={item.id} className={cardClass}>
                  {inner}
                </div>
              );
            })}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default DashboardPage;