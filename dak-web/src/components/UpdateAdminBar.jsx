'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Pencil, Check, Undo2 } from 'lucide-react';
import { updateUpdateMetadata, updateUpdateStatus } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';

/**
 * Admin controls for an Australia Update, shown on the article itself so a
 * problem spotted while reading can be fixed without hunting for it in the
 * queue.
 *
 * Only title and Korean summary are editable here. Category, scope and sources
 * are set in the review queue, where the completeness checks that gate
 * publication live.
 *
 * Archiving is deliberately absent: it belongs to the queue, where an admin can
 * see what else is in the same state. Unpublish returns the item there.
 */
export default function UpdateAdminBar({ update, children }) {
  const router = useRouter();
  const { user } = useAuth();

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState({ title: '', koreanSummary: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  function startEditing() {
    setDraft({
      title: update.title ?? '',
      koreanSummary: update.koreanSummary ?? '',
    });
    setEditing(true);
    setError('');
  }

  async function saveEdits() {
    setError('');
    setBusy(true);
    try {
      await updateUpdateMetadata(update.id, {
        title: draft.title,
        koreanSummary: draft.koreanSummary,
      });
      setEditing(false);
      // Re-fetches the server component so the article reflects the edit.
      router.refresh();
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setBusy(false);
    }
  }

  /**
   * Returns the update to the review queue rather than archiving it — an admin
   * unpublishing from the article itself is usually correcting something, not
   * retiring it.
   */
  async function unpublish() {
    setError('');
    setBusy(true);
    try {
      await updateUpdateStatus(update.id, 'DRAFT');
      router.push('/admin');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not unpublish.');
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
          {update.status === 'PUBLISHED' && (
            <button onClick={unpublish} disabled={busy} className={secondaryBtn}>
              <Undo2 size={13} strokeWidth={2} />
              Unpublish
            </button>
          )}
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
            <label htmlFor="edit-summary" className={labelClass}>
              Korean summary — write this yourself; do not paste the source
            </label>
            <textarea
              id="edit-summary"
              value={draft.koreanSummary}
              onChange={(e) => setDraft({ ...draft, koreanSummary: e.target.value })}
              rows={12}
              className={`${fieldClass} resize-y leading-relaxed`}
            />
          </div>

          <div className="flex gap-2">
            <button
              onClick={saveEdits}
              disabled={busy || !draft.title.trim() || !draft.koreanSummary.trim()}
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