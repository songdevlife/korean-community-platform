'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Pencil, Check, Undo2, Archive } from 'lucide-react';
import { fetchGuideCategories } from '@/api/guides';
import { updateGuide, updateGuideStatus } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';

/**
 * Admin controls for a guide, shown on the article itself so a problem spotted
 * while reading can be fixed without hunting for it in the queue.
 *
 * A client component inside a server-rendered page. It receives the guide as a
 * plain object — the page has already fetched it, so this does not refetch.
 *
 * Editing replaces the article rather than opening beside it, which is why the
 * page passes `children`: when the form is closed, the server-rendered article
 * shows through unchanged.
 */
export default function GuideAdminBar({ guide, children }) {
  const router = useRouter();
  const { user } = useAuth();

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState({
    title: '', slug: '', summary: '', body: '', categoryId: '',
  });
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  // Only needed once the edit form opens, and only an admin can open it.
  useEffect(() => {
    if (editing && categories.length === 0) {
      fetchGuideCategories()
        .then((data) => setCategories(data ?? []))
        .catch((err) => console.error('Failed to load guide categories:', err));
    }
  }, [editing, categories.length]);

  function startEditing() {
    setDraft({
      title: guide.title ?? '',
      slug: guide.slug ?? '',
      summary: guide.summary ?? '',
      body: guide.body ?? '',
      categoryId: guide.category?.id ?? '',
    });
    setEditing(true);
    setError('');
  }

  async function saveEdits() {
    setError('');
    setBusy(true);
    try {
      const updated = await updateGuide(guide.id, draft);
      setEditing(false);

      // The slug is part of the URL, so a slug change leaves the address bar
      // pointing at an address that no longer resolves.
      if (updated.slug !== guide.slug) {
        router.replace(`/guides/${updated.slug}`);
      } else {
        // Re-fetches the server component so the article reflects the edit.
        router.refresh();
      }
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setBusy(false);
    }
  }

  /**
   * Returns the guide to DRAFT rather than archiving it — an admin unpublishing
   * from the article itself is usually correcting something, not retiring it.
   * Both statuses remove it from public reads, so staying here would 404 on the
   * next load.
   */
  async function unpublish() {
    setError('');
    setBusy(true);
    try {
      await updateGuideStatus(guide.id, 'DRAFT');
      router.push('/admin');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not unpublish.');
      setBusy(false);
    }
  }

  async function archive() {
    setError('');
    setBusy(true);
    try {
      await updateGuideStatus(guide.id, 'ARCHIVED');
      router.push('/admin');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not archive.');
      setBusy(false);
    }
  }

  // Non-admins see the article exactly as the server rendered it.
  if (!isAdmin) return children;

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  const labelClass = 'block text-[12px] font-medium text-muted mb-1.5';

  return (
    <>
      {!editing && (
        <div className="flex flex-wrap items-center gap-2 mb-5 pb-5 border-b border-border-dark">
          <span className="text-[12px] text-faint mr-1">Admin</span>
          <button onClick={startEditing} className={secondaryBtn}>
            <Pencil size={13} strokeWidth={2} />
            Edit
          </button>
          {guide.status === 'PUBLISHED' && (
            <button onClick={unpublish} disabled={busy} className={secondaryBtn}>
              <Undo2 size={13} strokeWidth={2} />
              Unpublish
            </button>
          )}
          {/* No delete anywhere: ARCHIVED hides an item completely and is
              recoverable, whereas a mistaken delete is not. */}
          <button onClick={archive} disabled={busy} className={secondaryBtn}>
            <Archive size={13} strokeWidth={2} />
            Archive
          </button>
        </div>
      )}

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      {editing ? (
        <div className="flex flex-col gap-3 mb-6">
          <div>
            <label htmlFor="edit-title" className={labelClass}>Title</label>
            <input
              id="edit-title"
              type="text"
              value={draft.title}
              onChange={(e) => setDraft({ ...draft, title: e.target.value })}
              maxLength={300}
              className={fieldClass}
            />
          </div>

          <div>
            <label htmlFor="edit-slug" className={labelClass}>
              Slug — changing this breaks existing links
            </label>
            <input
              id="edit-slug"
              type="text"
              value={draft.slug}
              onChange={(e) => setDraft({ ...draft, slug: e.target.value })}
              maxLength={320}
              className={fieldClass}
            />
          </div>

          <div>
            <label htmlFor="edit-category" className={labelClass}>Category</label>
            <select
              id="edit-category"
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
            <label htmlFor="edit-summary" className={labelClass}>Summary</label>
            <textarea
              id="edit-summary"
              value={draft.summary}
              onChange={(e) => setDraft({ ...draft, summary: e.target.value })}
              rows={3}
              maxLength={500}
              className={`${fieldClass} resize-y leading-relaxed`}
            />
          </div>

          <div>
            <label htmlFor="edit-body" className={labelClass}>Body (markdown)</label>
            <textarea
              id="edit-body"
              value={draft.body}
              onChange={(e) => setDraft({ ...draft, body: e.target.value })}
              rows={20}
              className={`${fieldClass} resize-y leading-relaxed font-mono text-[13px]`}
            />
          </div>

          <div className="flex gap-2">
            <button
              onClick={saveEdits}
              disabled={busy || !draft.title.trim() || !draft.body.trim()}
              className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium
                         bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors
                         disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <Check size={14} strokeWidth={2.5} />
              {busy ? 'Saving…' : 'Save'}
            </button>
            <button onClick={() => setEditing(false)} className={secondaryBtn}>
              Cancel
            </button>
          </div>
        </div>
      ) : (
        children
      )}
    </>
  );
}