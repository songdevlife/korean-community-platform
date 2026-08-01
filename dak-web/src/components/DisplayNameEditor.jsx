'use client';

import { useState } from 'react';
import { Pencil, Check, X } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { updateProfile } from '@/api/users';

/**
 * Inline display-name editing on the account card.
 *
 * A name chosen at sign-up appears publicly as the author of a guide, so this
 * is the route out for anyone who used their legal name without meaning to
 * publish it. Guides reference the user rather than storing a copy, so past
 * work re-attributes itself.
 *
 * Inline rather than a settings page: there is one editable field, and a page
 * holding one field is mostly empty. If password changes and profile images
 * arrive, that is the point to move this rather than now.
 */
export default function DisplayNameEditor() {
  const { user, updateUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function startEditing() {
    setValue(user?.displayName ?? '');
    setError('');
    setEditing(true);
  }

  function cancel() {
    setEditing(false);
    setError('');
  }

  async function save() {
    const trimmed = value.trim();

    if (!trimmed) {
      setError('이름을 입력해 주세요.');
      return;
    }

    if (trimmed === user?.displayName) {
      setEditing(false);
      return;
    }

    setSaving(true);
    setError('');

    try {
      const profile = await updateProfile(trimmed);
      updateUser(profile);
      setEditing(false);
    } catch (err) {
      // The backend distinguishes a taken name from everything else. Showing
      // the generic message for both would leave someone retyping a name that
      // will never be accepted, with nothing to tell them why.
      const code = err?.response?.data?.error?.code;
      setError(
        code === 'DISPLAY_NAME_TAKEN'
          ? '이미 사용 중인 이름입니다.'
          : '이름을 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      );
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <div className="flex items-center gap-2 min-w-0">
        <p className="font-semibold text-snow truncate">{user?.displayName}</p>
        <button
          onClick={startEditing}
          aria-label="Change display name"
          className="shrink-0 text-muted hover:text-snow transition-colors cursor-pointer"
        >
          <Pencil size={14} strokeWidth={1.75} />
        </button>
      </div>
    );
  }

  return (
    <div className="min-w-0">
      <div className="flex items-center gap-2">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') save();
            if (e.key === 'Escape') cancel();
          }}
          maxLength={100}
          autoFocus
          disabled={saving}
          className="min-w-0 flex-1 bg-surface border border-border-dark rounded-lg
                     px-2.5 py-1.5 text-sm text-snow
                     focus:outline-none focus:border-faint disabled:opacity-60"
        />
        <button
          onClick={save}
          disabled={saving}
          aria-label="Save"
          className="shrink-0 text-korea-blue hover:opacity-80 transition-opacity
                     cursor-pointer disabled:opacity-40"
        >
          <Check size={16} strokeWidth={2} />
        </button>
        <button
          onClick={cancel}
          disabled={saving}
          aria-label="Cancel"
          className="shrink-0 text-muted hover:text-snow transition-colors
                     cursor-pointer disabled:opacity-40"
        >
          <X size={16} strokeWidth={2} />
        </button>
      </div>

      {error && <p className="text-[12px] text-adelaide-red mt-1.5">{error}</p>}
    </div>
  );
}