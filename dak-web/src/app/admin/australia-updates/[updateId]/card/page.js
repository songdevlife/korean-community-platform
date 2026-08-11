'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import {
  ArrowLeft, ChevronLeft, ChevronRight, Download, RefreshCw, Sparkles,
} from 'lucide-react';
import { renderUpdateCard } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

/**
 * Card preview for one Australia Update.
 *
 * A first render takes around half a minute: the text is written, then an
 * illustration is generated, then the card is drawn. Both are stored, so
 * pressing Render again returns the same card immediately. Regenerating is
 * the only route to different artwork and is deliberately separate, because
 * it is the action that costs money.
 *
 * Where the content warranted a carousel the update produces several cards.
 * Only the first carries artwork; the rest are text and render in a moment.
 */
export default function UpdateCardPage() {
  const router = useRouter();
  const { updateId } = useParams();
  const { user, loading: authLoading } = useAuth();

  // One entry per card index, filled as each is fetched.
  const [cards, setCards] = useState({});
  const [cardCount, setCardCount] = useState(1);
  const [index, setIndex] = useState(0);

  const [busy, setBusy] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState('');

  const isAdmin = user?.role === 'ADMINISTRATOR';
  const currentUrl = cards[index];

  // Object URLs are not garbage collected on their own, and a session here
  // creates one per card rendered.
  useEffect(() => {
    return () => {
      Object.values(cards).forEach((url) => URL.revokeObjectURL(url));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function render(cardIndex, { regenerate = false } = {}) {
    setError('');
    setBusy(true);
    setRegenerating(regenerate);

    try {
      const { blob, cardCount: count } = await renderUpdateCard(updateId, {
        regenerate,
        index: cardIndex,
      });

      // Some mobile browsers drop the download attribute's filename and save
      // the blob with no extension, which leaves it out of the gallery. An
      // explicit type is what they fall back to.
      const png =
        blob.type === 'image/png'
          ? blob
          : new Blob([blob], { type: 'image/png' });

      setCards((previous) => {
        // Regenerating invalidates every card, since the text is rewritten
        // along with the artwork.
        const kept = regenerate ? {} : { ...previous };

        if (regenerate) {
          Object.values(previous).forEach((url) => URL.revokeObjectURL(url));
        } else if (kept[cardIndex]) {
          URL.revokeObjectURL(kept[cardIndex]);
        }

        kept[cardIndex] = URL.createObjectURL(png);
        return kept;
      });

      setCardCount(count);
      setIndex(cardIndex);
    } catch (err) {
      // The error body arrives as a blob because the request asked for one,
      // so it has to be read back as text before the message is reachable.
      let message = 'Could not render the card.';

      try {
        const text = await err.response?.data?.text?.();
        const parsed = text ? JSON.parse(text) : null;
        message = parsed?.error?.message ?? message;
      } catch {
        // Leave the default message.
      }

      setError(message);
    } finally {
      setBusy(false);
      setRegenerating(false);
    }
  }

  // Fetches a card the first time it is looked at; later visits reuse it.
  function goTo(nextIndex) {
    if (nextIndex < 0 || nextIndex >= cardCount) return;

    if (cards[nextIndex]) {
      setIndex(nextIndex);
      return;
    }

    render(nextIndex);
  }

  function download() {
    if (!currentUrl) return;

    const link = document.createElement('a');
    link.href = currentUrl;
    link.download =
      cardCount > 1
        ? `dak-card-${updateId}-${index + 1}.png`
        : `dak-card-${updateId}.png`;
    link.click();
  }

  const primaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium ' +
    'bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-korea-blue';

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg ' +
    'border border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:text-muted ' +
    'disabled:hover:border-border-dark';

  if (authLoading) {
    return (
      <PageShell>
        <div className="h-40 rounded-2xl bg-night border border-border-dark animate-pulse" />
      </PageShell>
    );
  }

  if (!isAdmin) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <p className="text-snow font-medium mb-1">This page is not available</p>
          <Link
            href="/"
            className="inline-block mt-4 px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm
                       font-medium hover:bg-korea-blue/85 transition-colors"
          >
            Home
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <button
        onClick={() => router.back()}
        aria-label="Back"
        className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
      >
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span className="hidden sm:inline">Back</span>
      </button>

      <h1 className="text-xl font-bold text-snow mb-1">Social card</h1>
      <p className="text-[13px] text-muted mb-5">
        1080 × 1350, ready for Instagram.
        {cardCount > 1 && ` Carousel of ${cardCount}.`}
      </p>

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex flex-wrap gap-2 mb-5">
        <button onClick={() => render(index)} disabled={busy} className={primaryBtn}>
          <Sparkles size={14} strokeWidth={2} />
          {busy && !regenerating ? 'Rendering…' : currentUrl ? 'Render again' : 'Render'}
        </button>

        {/* Separate from Render because this is what pays for new artwork,
            and it rewrites the text as well. */}
        <button
          onClick={() => render(0, { regenerate: true })}
          disabled={busy}
          className={secondaryBtn}
        >
          <RefreshCw
            size={14}
            strokeWidth={2}
            className={regenerating ? 'animate-spin' : undefined}
          />
          {regenerating ? 'Generating…' : 'New illustration'}
        </button>

        <button onClick={download} disabled={!currentUrl || busy} className={secondaryBtn}>
          <Download size={14} strokeWidth={2} />
          Download
        </button>
      </div>

      {busy && (
        <p className="text-[13px] text-muted mb-4">
          {regenerating
            ? 'Writing the card and generating a new illustration. This takes about a minute.'
            : 'Rendering. If the illustration has to be generated this takes about a minute.'}
        </p>
      )}

      {/* Only where there is more than one card. A pager over a single card
          is a control that does nothing. */}
      {cardCount > 1 && (
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={() => goTo(index - 1)}
            disabled={busy || index === 0}
            className={secondaryBtn}
            aria-label="Previous card"
          >
            <ChevronLeft size={14} strokeWidth={2} />
          </button>

          <span className="text-[13px] text-muted tabular-nums">
            {index + 1} / {cardCount}
          </span>

          <button
            onClick={() => goTo(index + 1)}
            disabled={busy || index === cardCount - 1}
            className={secondaryBtn}
            aria-label="Next card"
          >
            <ChevronRight size={14} strokeWidth={2} />
          </button>
        </div>
      )}

      <div
        className="rounded-2xl border border-border-dark bg-night p-4 flex justify-center"
        style={{ minHeight: 320 }}
      >
        {currentUrl ? (
          <img
            src={currentUrl}
            alt={`Card ${index + 1}`}
            className="rounded-lg max-w-full"
            style={{ maxWidth: 405 }}
          />
        ) : (
          <p className="text-muted text-sm self-center">
            {busy ? 'Working…' : 'Nothing rendered yet.'}
          </p>
        )}
      </div>
    </PageShell>
  );
}