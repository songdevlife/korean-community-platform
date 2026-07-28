'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Check } from 'lucide-react';
import { fetchGuideCategories } from '@/api/guides';
import { createGuide } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

/**
 * Guide authoring. Admin-only and entirely a client component — a long form
 * with no reason to be crawlable.
 */
export default function GuideCreatePage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();

  const [draft, setDraft] = useState({
    title: '',
    slug: '',
    summary: '',
    body: '',
    categoryId: '',
  });
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  useEffect(() => {
    if (!isAdmin) return;
    fetchGuideCategories()
      .then((data) => setCategories(data ?? []))
      .catch((err) => console.error('Failed to load guide categories:', err));
  }, [isAdmin]);

  async function handleSubmit() {
    setError('');
    setBusy(true);
    try {
      // categoryId is a UUID on the backend — an empty string would fail
      // validation, so send null instead when nothing is selected.
      await createGuide({
        title: draft.title.trim(),
        slug: draft.slug.trim() || null,
        summary: draft.summary.trim() || null,
        body: draft.body,
        categoryId: draft.categoryId || null,
      });
      // A new guide is created as DRAFT, so the public detail page would 404.
      // The admin queue is where it can be reviewed and published.
      router.push('/admin');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not create the guide.');
      setBusy(false);
    }
  }

  // Waits for the session before deciding — rendering the refusal while it is
  // still unknown would flash it at an admin who is signed in.
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
          <p className="text-muted text-sm mb-5">
            Guides are written by the DAK team.
          </p>
          <Link
            href="/guides"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors"
          >
            Browse guides
          </Link>
        </div>
      </PageShell>
    );
  }

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  return (
    <PageShell>
      <div className="flex items-center justify-between mb-5">
        <button
          onClick={() => router.back()}
          aria-label="Back"
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">Back</span>
        </button>
      </div>

      <h1 className="text-2xl font-bold text-snow leading-tight mb-6">New guide</h1>

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex flex-col gap-3">
        <div>
          <label htmlFor="new-title" className="block text-[12px] font-medium text-muted mb-1.5">
            Title
          </label>
          <input
            id="new-title"
            type="text"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            maxLength={300}
            className={fieldClass}
          />
        </div>

        <div>
          <label htmlFor="new-slug" className="block text-[12px] font-medium text-muted mb-1.5">
            Slug — use English; a Korean title cannot generate one
          </label>
          <input
            id="new-slug"
            type="text"
            value={draft.slug}
            onChange={(e) => setDraft({ ...draft, slug: e.target.value })}
            maxLength={320}
            placeholder="korean-licence-transfer-south-australia-2026"
            className={`${fieldClass} placeholder:text-faint`}
          />
        </div>

        <div>
          <label htmlFor="new-category" className="block text-[12px] font-medium text-muted mb-1.5">
            Category
          </label>
          <select
            id="new-category"
            value={draft.categoryId}
            onChange={(e) => setDraft({ ...draft, categoryId: e.target.value })}
            className={fieldClass}
          >
            <option value="" className="bg-surface">Select a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id} className="bg-surface">
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="new-summary" className="block text-[12px] font-medium text-muted mb-1.5">
            Summary — one or two sentences, no line breaks
          </label>
          <textarea
            id="new-summary"
            value={draft.summary}
            onChange={(e) => setDraft({ ...draft, summary: e.target.value })}
            rows={3}
            maxLength={500}
            className={`${fieldClass} resize-y leading-relaxed`}
          />
          <p className="text-[11px] text-faint mt-1">{draft.summary.length}/500</p>
        </div>

        <div>
          <label htmlFor="new-body" className="block text-[12px] font-medium text-muted mb-1.5">
            Body (markdown) — do not repeat the title as a heading; the page
            renders it above this
          </label>
          <textarea
            id="new-body"
            value={draft.body}
            onChange={(e) => setDraft({ ...draft, body: e.target.value })}
            rows={24}
            className={`${fieldClass} resize-y leading-relaxed font-mono text-[13px]`}
          />
        </div>

        <div className="flex gap-2 mt-1">
          <button
            onClick={handleSubmit}
            disabled={busy || !draft.title.trim() || !draft.body.trim()}
            className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium
                       bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <Check size={14} strokeWidth={2.5} />
            {busy ? 'Creating…' : 'Create'}
          </button>
          <button onClick={() => router.back()} className={secondaryBtn}>
            Cancel
          </button>
        </div>
      </div>
    </PageShell>
  );
}