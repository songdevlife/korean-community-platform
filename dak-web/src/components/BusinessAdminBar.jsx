'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Pencil, Check, Undo2 } from 'lucide-react';
import { updateBusiness, updateBusinessStatus } from '@/api/admin';
import { useAuth } from '@/context/AuthContext';

/**
 * Admin controls for a business listing, shown on the listing itself so a
 * problem spotted while reading can be fixed without hunting for it in the
 * queue.
 *
 * Limited to fields an admin can verify by eye. Address, coordinates and
 * categories are edited elsewhere — nothing on this page tells you whether a
 * street number is right, so offering to change it here invites a guess.
 */
export default function BusinessAdminBar({ business, children }) {
  const router = useRouter();
  const { user } = useAuth();

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  function startEditing() {
    setDraft({
      name: business.name ?? '',
      shortDescription: business.shortDescription ?? '',
      description: business.description ?? '',
      phone: business.phone ?? '',
      email: business.email ?? '',
      websiteUrl: business.websiteUrl ?? '',
      verified: business.verified ?? false,
    });
    setEditing(true);
    setError('');
  }

  async function saveEdits() {
    setError('');
    setBusy(true);
    try {
      await updateBusiness(business.id, draft);
      setEditing(false);
      // Re-fetches the server component so the listing reflects the edit.
      router.refresh();
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setBusy(false);
    }
  }

  /**
   * Returns the listing to the review queue rather than archiving it — an admin
   * unpublishing while reading a page is usually correcting something rather
   * than retiring it.
   */
  async function unpublish() {
    setError('');
    setBusy(true);
    try {
      await updateBusinessStatus(business.id, 'PENDING');
      router.push('/admin');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Could not unpublish.');
      setBusy(false);
    }
  }

  // Non-admins see the listing exactly as the server rendered it.
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
            Edit listing
          </button>
          {business.status === 'PUBLISHED' && (
            <button onClick={unpublish} disabled={busy} className={secondaryBtn}>
              <Undo2 size={13} strokeWidth={2} />
              Unpublish
            </button>
          )}
          <span className="text-[12px] text-faint">Status: {business.status}</span>
        </div>
      )}

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      {editing ? (
        <div className="flex flex-col gap-3 mb-6">
          <div>
            <label htmlFor="edit-name" className={labelClass}>Name</label>
            <input
              id="edit-name"
              type="text"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              maxLength={200}
              className={fieldClass}
            />
          </div>

          <div>
            <label htmlFor="edit-short" className={labelClass}>Short description</label>
            <input
              id="edit-short"
              type="text"
              value={draft.shortDescription}
              onChange={(e) => setDraft({ ...draft, shortDescription: e.target.value })}
              maxLength={300}
              className={fieldClass}
            />
          </div>

          <div>
            <label htmlFor="edit-desc" className={labelClass}>Description</label>
            <textarea
              id="edit-desc"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              rows={6}
              className={`${fieldClass} resize-y leading-relaxed`}
            />
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label htmlFor="edit-phone" className={labelClass}>Phone</label>
              <input
                id="edit-phone"
                type="tel"
                value={draft.phone}
                onChange={(e) => setDraft({ ...draft, phone: e.target.value })}
                className={fieldClass}
              />
            </div>
            <div>
              <label htmlFor="edit-email" className={labelClass}>Email</label>
              <input
                id="edit-email"
                type="email"
                value={draft.email}
                onChange={(e) => setDraft({ ...draft, email: e.target.value })}
                className={fieldClass}
              />
            </div>
          </div>

          <div>
            <label htmlFor="edit-website" className={labelClass}>Website</label>
            <input
              id="edit-website"
              type="url"
              value={draft.websiteUrl}
              onChange={(e) => setDraft({ ...draft, websiteUrl: e.target.value })}
              placeholder="https://"
              className={fieldClass}
            />
          </div>

          <label className="flex items-center gap-2.5 text-[13px] text-snow cursor-pointer">
            <input
              type="checkbox"
              checked={draft.verified}
              onChange={(e) => setDraft({ ...draft, verified: e.target.checked })}
              className="w-4 h-4 rounded accent-korea-blue"
            />
            Verified business
          </label>

          <div className="flex gap-2 mt-1">
            <button
              onClick={saveEdits}
              disabled={busy || !draft.name.trim()}
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

          <p className="text-[12px] text-faint">
            Address, coordinates and categories are edited elsewhere — they
            cannot be checked from this page.
          </p>
        </div>
      ) : (
        children
      )}
    </>
  );
}