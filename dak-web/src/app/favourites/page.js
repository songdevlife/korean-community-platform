'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Heart, X } from 'lucide-react';
import { fetchSavedItems, removeSavedItem } from '@/api/savedItems';
import { resourceLink, resourceIcon } from '@/utils/savedItems';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

/**
 * Saved items. A client component in full: the list is per-user, so there is
 * nothing a server render could produce that would be correct for anyone.
 *
 * The loading skeleton stays here, unlike the public pages that lost theirs.
 * Those wait for data on the server before sending anything; this one cannot,
 * because the session it depends on only exists in the browser.
 */
export default function FavouritesPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const [savedItems, setSavedItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    // Wait until the session is known. During authLoading `user` is null but
    // that means "not read yet", not "signed out" — fetching now would send an
    // unauthenticated request that always fails.
    if (authLoading) return;

    if (!user) {
      setLoading(false);
      return;
    }

    fetchSavedItems()
      .then((items) => setSavedItems(items ?? []))
      .catch((err) => console.error('Failed to load saved items:', err))
      .finally(() => setLoading(false));
  }, [user, authLoading]);

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

  if (!authLoading && !user) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <Heart size={22} strokeWidth={1.5} className="text-border-dark mx-auto mb-2" />
          <p className="text-snow font-medium mb-1">Log in to see your favourites</p>
          <p className="text-muted text-sm mb-5">
            Saved businesses, guides and updates are kept with your account.
          </p>
          <Link
            href="/login"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors"
          >
            Log in
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      {/* Favourites is reached from My Page, so it gets a back control like
          the other second-level screens. */}
      <button
        onClick={() => router.back()}
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
                    href={resourceLink(item)}
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
  );
}