import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Heart, X } from 'lucide-react';
import { fetchSavedItems, removeSavedItem } from '../api/savedItems';
import { resourceLink, resourceIcon } from '../utils/savedItems';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import PageMeta from '../components/PageMeta';

function FavouritesPage() {
  const navigate = useNavigate();
  const [savedItems, setSavedItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  function loadItems() {
    setLoading(true);
    fetchSavedItems()
      .then((items) => setSavedItems(items ?? []))
      .catch((err) => console.error('Failed to load saved items:', err))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadItems();
  }, []);

  async function handleRemove(savedItemId) {
    setError('');
    // Optimistic removal — the row disappears immediately and is restored
    // only if the request fails.
    const previous = savedItems;
    setSavedItems((items) => items.filter((i) => i.id !== savedItemId));

    try {
      await removeSavedItem(savedItemId);
    } catch (err) {
      console.error('Failed to remove saved item:', err);
      setSavedItems(previous);
      setError('Could not remove that item. Please try again.');
    }
  }

  return (
    <Layout>
      <PageShell>
      <PageMeta title="Favourites" noIndex />
        {/* Favourites is reached from My Page, so it gets a back control like
            the other second-level screens. */}
        <button
          onClick={() => navigate(-1)}
          aria-label="Back"
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">Back</span>
        </button>

        <h1 className="text-xl font-bold text-snow mb-1">Favourites</h1>
        <p className="text-sm text-muted mb-4">
          {loading ? '\u00A0' : `${savedItems.length} item${savedItems.length === 1 ? '' : 's'}`}
        </p>

        {error && <p className="text-adelaide-red text-[13px] mb-3">{error}</p>}

        {loading ? (
          <div className="grid gap-2.5">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="h-16 rounded-xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : savedItems.length === 0 ? (
          <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
            <Heart size={22} strokeWidth={1.5} className="text-border-dark mx-auto mb-2" />
            <p className="text-muted text-sm">Nothing saved yet.</p>
            <p className="text-faint text-[13px] mt-1">
              Save a business, guide or update and it will appear here.
            </p>
          </div>
        ) : (
          <div className="grid gap-2.5">
            {savedItems.map((item) => {
              const Icon = resourceIcon(item.resourceType);

              // A row is kept when its resource is deleted or taken out of
              // public view, but the link is not: saving was the user's action,
              // while unpublishing was someone else's, and following a dead
              // link would 404 with no explanation of why.
              const inner = (
                <>
                  <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                                   flex items-center justify-center">
                    <Icon size={15} strokeWidth={1.75} className="text-muted" />
                  </span>
                  <div className="min-w-0">
                    <p className={`text-sm font-medium truncate ${
                      item.available ? 'text-snow' : 'text-muted'
                    }`}>
                      {item.title}
                    </p>
                    {!item.available && (
                      <p className="text-[12px] text-faint">더 이상 공개되지 않는 항목입니다</p>
                    )}
                  </div>
                </>
              );

              return (
                <div
                  key={item.id}
                  className={`rounded-xl border border-border-dark bg-night flex items-center
                              transition-colors ${item.available ? 'hover:border-faint' : ''}`}
                >
                  {item.available ? (
                    <Link
                      to={resourceLink(item)}
                      className="flex-1 min-w-0 flex items-center gap-3 p-3.5"
                    >
                      {inner}
                    </Link>
                  ) : (
                    <div className="flex-1 min-w-0 flex items-center gap-3 p-3.5">
                      {inner}
                    </div>
                  )}

                  <button
                    onClick={() => handleRemove(item.id)}
                    aria-label={`Remove ${item.title} from favourites`}
                    className="p-3 mr-1 rounded-lg text-muted hover:text-adelaide-red
                               transition-colors shrink-0"
                  >
                    <X size={16} strokeWidth={2} />
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default FavouritesPage;