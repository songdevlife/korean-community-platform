'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft, Download, RefreshCw, Sparkles } from 'lucide-react';
import { renderUpdateCard } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

/**
 * Card preview for one Australia Update.
 *
 * A first render takes around half a minute: the text is written, then an
 * illustration is generated, then the card is drawn. The backend keeps the
 * illustration, so pressing the button again returns the same artwork
 * immediately. Regenerate is the only route to different artwork and is
 * deliberately separate, because it is the action that costs money.
 */
export default function UpdateCardPage() {
  const router = useRouter();
  const { updateId } = useParams();
  const { user, loading: authLoading } = useAuth();

  const [cardUrl, setCardUrl] = useState(null);
  const [busy, setBusy] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState('');

  const isAdmin = user?.role === 'ADMINISTRATOR';

  // Object URLs are not garbage collected on their own, and a session here
  // creates one per render.
  useEffect(() => {
    return () => {
      if (cardUrl) URL.revokeObjectURL(cardUrl);
    };
  }, [cardUrl]);

  async function render({ regenerate = false } = {}) {
    setError('');
    setBusy(true);
    setRegenerating(regenerate);

    try {
      const blob = await renderUpdateCard(updateId, { regenerate });

      setCardUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return URL.createObjectURL(blob);
      });
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

  function download() {
    if (!cardUrl) return;

    const link = document.createElement('a');
    link.href = cardUrl;
    link.download = `dak-card-${updateId}.png`;
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
      </p>

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex gap-2 mb-5">
        <button
          onClick={() => render()}
          disabled={busy}
          className={primaryBtn}
        >
          <Sparkles size={14} strokeWidth={2} />
          {busy && !regenerating ? 'Rendering…' : cardUrl ? 'Render again' : 'Render'}
        </button>

        {/* Separate from Render because this is what pays for new artwork.
            Render on its own reuses whatever was generated before. */}
        <button
          onClick={() => render({ regenerate: true })}
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

        <button
          onClick={download}
          disabled={!cardUrl || busy}
          className={secondaryBtn}
        >
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

      <div
        className="rounded-2xl border border-border-dark bg-night p-4 flex justify-center"
        style={{ minHeight: 320 }}
      >
        {cardUrl ? (
          <img
            src={cardUrl}
            alt="Rendered card"
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