'use client';

import { useState, useEffect } from 'react';
import { Heart } from 'lucide-react';
import { saveItem, checkIsSaved, removeSavedByResource } from '@/api/savedItems';
import { useAuth } from '@/context/AuthContext';

/**
 * Save toggle, shared by every detail page.
 *
 * A client component sitting inside a server-rendered page: the article around
 * it is HTML by the time it reaches the browser, and only this control ships
 * JavaScript. That split is the point of the App Router — interactivity is
 * opt-in per component rather than per page.
 *
 * Generalised across resource types because all three detail pages were running
 * near-identical copies of this logic, and the toggle had to be added to each of
 * them separately.
 *
 * @param {'GUIDE'|'BUSINESS'|'AUSTRALIA_UPDATE'} resourceType
 * @param {string} resourceId
 * @param {'rail'|'icon'} [variant]  Full-width button for a desktop rail, or a
 *   bare heart for a mobile top bar.
 */
export default function SaveButton({ resourceType, resourceId, variant = 'rail' }) {
  const { user, loading } = useAuth();
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    // Wait until the session is known. During loading `user` is null but that
    // means "not read yet", not "signed out" — checking now would fire an
    // unauthenticated request that always comes back false.
    if (loading) return;

    if (!user) {
      setSaved(false);
      return;
    }

    checkIsSaved(resourceType, resourceId)
      .then(setSaved)
      .catch((err) => console.error('Failed to check saved state:', err));
  }, [user, loading, resourceType, resourceId]);

  /**
   * Toggles rather than only saving. A control that fills in and then refuses to
   * respond reads as broken, and an accidental save would otherwise be undoable
   * only from the favourites list.
   */
  async function toggle() {
    if (!user) {
      setError('Please log in to save.');
      return;
    }

    setBusy(true);
    setError('');

    try {
      if (saved) {
        await removeSavedByResource(resourceType, resourceId);
        setSaved(false);
      } else {
        await saveItem(resourceType, resourceId);
        setSaved(true);
      }
    } catch (err) {
      // 409 on save means it was already saved — the state the caller wanted
      // either way, so treat it as success rather than an error.
      if (!saved && err.response?.status === 409) {
        setSaved(true);
      } else {
        setError(saved ? 'Could not remove. Please try again.' : 'Could not save. Please try again.');
      }
    } finally {
      setBusy(false);
    }
  }

  if (variant === 'icon') {
    return (
      <>
        <button
          onClick={toggle}
          disabled={busy}
          aria-label={saved ? 'Remove from favourites' : 'Save to favourites'}
          aria-pressed={saved}
          className={`p-2 rounded-lg transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${
            saved ? 'text-adelaide-red' : 'text-muted hover:text-snow hover:bg-night'
          }`}
        >
          <Heart size={20} strokeWidth={1.75} fill={saved ? 'currentColor' : 'none'} />
        </button>
        {/* The icon variant has nowhere to show a message, and a guest tapping
            the heart would otherwise get no response at all. Announced to
            screen readers; sighted users see the prompt on the rail variant. */}
        {error && <span className="sr-only" role="status">{error}</span>}
      </>
    );
  }

  return (
    <>
      <button
        onClick={toggle}
        disabled={busy}
        className={`flex items-center justify-center gap-2 text-sm px-4 py-2.5 rounded-xl font-medium
                    transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${
          saved
            ? 'bg-night text-muted border border-border-dark hover:text-snow hover:border-faint'
            : 'bg-korea-blue text-white hover:bg-korea-blue/85'
        }`}
      >
        <Heart size={15} strokeWidth={2} fill={saved ? 'currentColor' : 'none'} />
        {saved ? 'Saved' : 'Save to favourites'}
      </button>
      {error && <p className="text-adelaide-red text-xs">{error}</p>}
    </>
  );
}