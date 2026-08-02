'use client';

import { useState, useEffect } from 'react';
import { Check } from 'lucide-react';
import { fetchEventCategories } from '@/api/events';

const FIELD_IDS = {
  title: 'event-title',
  startsAt: 'event-starts-at',
  endsAt: 'event-ends-at',
  description: 'event-description',
};

const EMPTY = {
  title: '', description: '', startsAt: '', endsAt: '',
  venueName: '', venueAddress: '', isFree: false, priceNote: '',
  organiser: '', organiserContact: '', sourceUrl: '', categoryId: '',
};

/**
 * Reads a datetime-local value as Adelaide time and returns an ISO instant.
 *
 * A datetime-local input carries no zone, so what is typed is interpreted in
 * the browser's - correct while the browser is in Adelaide and wrong the
 * moment it is not. Adelaide is +9:30 or +10:30 depending on daylight saving,
 * so the offset is found rather than assumed: read the value as if it were
 * UTC, ask what that instant reads as in Adelaide, and correct by the gap.
 */
export function adelaideToIso(localValue) {
  if (!localValue) return null;
  const asUtc = new Date(`${localValue}:00Z`);
  const inAdelaide = new Date(asUtc.toLocaleString('en-US', { timeZone: 'Australia/Adelaide' }));
  const inUtc = new Date(asUtc.toLocaleString('en-US', { timeZone: 'UTC' }));
  return new Date(asUtc.getTime() - (inAdelaide - inUtc)).toISOString();
}

/** The reverse, for populating the form when editing. */
export function isoToAdelaideLocal(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const date = d.toLocaleDateString('en-CA', { timeZone: 'Australia/Adelaide' });
  const time = d.toLocaleTimeString('en-GB', {
    timeZone: 'Australia/Adelaide', hour: '2-digit', minute: '2-digit',
  });
  return `${date}T${time}`;
}

/**
 * The event fields, shared by the create and edit screens.
 *
 * Extracted rather than written twice: twelve fields duplicated across two
 * files means every addition is two edits, and the one that gets forgotten
 * fails silently - the field simply cannot be set from that screen. The same
 * reasoning that moved the source list into its own component.
 *
 * @param {object}   [initial]   Existing values, already in form shape
 * @param {function} onSubmit    Receives the payload; throws to report failure
 * @param {string}   submitLabel
 */
export default function EventForm({ initial, onSubmit, submitLabel = 'Create', onCancel }) {
  const [draft, setDraft] = useState({ ...EMPTY, ...initial });
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    fetchEventCategories()
      .then((data) => setCategories(data ?? []))
      .catch((err) => console.error('Failed to load event categories:', err));
  }, []);

  async function handleSubmit() {
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      await onSubmit({
        title: draft.title.trim(),
        description: draft.description.trim() || null,
        startsAt: adelaideToIso(draft.startsAt),
        endsAt: adelaideToIso(draft.endsAt),
        venueName: draft.venueName.trim() || null,
        venueAddress: draft.venueAddress.trim() || null,
        isFree: draft.isFree,
        priceNote: draft.isFree ? null : draft.priceNote.trim() || null,
        organiser: draft.organiser.trim() || null,
        organiserContact: draft.organiserContact.trim() || null,
        sourceUrl: draft.sourceUrl.trim() || null,
        categoryId: draft.categoryId || null,
      });
    } catch (err) {
      const apiError = err.response?.data?.error;
      const details = apiError?.details ?? [];
      if (details.length > 0) {
        setFieldErrors(Object.fromEntries(details.map((d) => [d.field, d.message])));
        document.getElementById(FIELD_IDS[details[0].field] ?? '')
          ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else {
        setError(apiError?.message || 'That did not work.');
      }
      setBusy(false);
    }
  }

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';
  const labelClass = 'block text-[12px] font-medium text-muted mb-1.5';
  const errorClass = 'text-adelaide-red text-[12px] mt-1.5';
  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';

  const set = (patch) => setDraft({ ...draft, ...patch });

  return (
    <>
      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex flex-col gap-3">
        <div>
          <label htmlFor="event-title" className={labelClass}>Title</label>
          <input
            id="event-title" type="text" value={draft.title}
            onChange={(e) => set({ title: e.target.value })}
            maxLength={300} className={fieldClass}
          />
          {fieldErrors.title && <p className={errorClass}>{fieldErrors.title}</p>}
        </div>

        <div>
          <label htmlFor="event-category" className={labelClass}>
            Category — required before publishing
          </label>
          <select
            id="event-category" value={draft.categoryId}
            onChange={(e) => set({ categoryId: e.target.value })}
            className={fieldClass}
          >
            <option value="" className="bg-surface">Select a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id} className="bg-surface">{c.name}</option>
            ))}
          </select>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="event-starts-at" className={labelClass}>
              Starts — Adelaide time
            </label>
            <input
              id="event-starts-at" type="datetime-local" value={draft.startsAt}
              onChange={(e) => set({ startsAt: e.target.value })}
              className={fieldClass}
            />
            {fieldErrors.startsAt && <p className={errorClass}>{fieldErrors.startsAt}</p>}
          </div>
          <div>
            <label htmlFor="event-ends-at" className={labelClass}>Ends — optional</label>
            <input
              id="event-ends-at" type="datetime-local" value={draft.endsAt}
              onChange={(e) => set({ endsAt: e.target.value })}
              className={fieldClass}
            />
            {fieldErrors.endsAt && <p className={errorClass}>{fieldErrors.endsAt}</p>}
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="event-venue-name" className={labelClass}>Venue name</label>
            <input
              id="event-venue-name" type="text" value={draft.venueName}
              onChange={(e) => set({ venueName: e.target.value })}
              maxLength={200} className={fieldClass}
            />
          </div>
          <div>
            <label htmlFor="event-venue-address" className={labelClass}>Venue address</label>
            <input
              id="event-venue-address" type="text" value={draft.venueAddress}
              onChange={(e) => set({ venueAddress: e.target.value })}
              maxLength={300} className={fieldClass}
            />
          </div>
        </div>

        <div>
          <label className={labelClass}>Price</label>
          <label className="flex items-center gap-2 text-[14px] text-snow mb-2 cursor-pointer w-fit">
            <input
              type="checkbox" checked={draft.isFree}
              onChange={(e) => set({ isFree: e.target.checked })}
              className="w-4 h-4 accent-korea-blue cursor-pointer"
            />
            무료
          </label>
          {/* Disabled rather than hidden: a note typed and then made
              unreachable is easier to follow than one that vanishes. Dropped
              on submit either way. */}
          <input
            type="text" value={draft.priceNote}
            onChange={(e) => set({ priceNote: e.target.value })}
            disabled={draft.isFree} maxLength={100} placeholder="$15, 학생 $10"
            className={`${fieldClass} placeholder:text-faint disabled:opacity-40`}
          />
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="event-organiser" className={labelClass}>Organiser</label>
            <input
              id="event-organiser" type="text" value={draft.organiser}
              onChange={(e) => set({ organiser: e.target.value })}
              maxLength={200} className={fieldClass}
            />
          </div>
          <div>
          <label htmlFor="event-organiser-contact" className={labelClass}>
            Contact — a link where possible, and only with permission
          </label>
            <input
              id="event-organiser-contact" type="text" value={draft.organiserContact}
              onChange={(e) => set({ organiserContact: e.target.value })}
              maxLength={300} placeholder="https://instagram.com/... , 오픈채팅 링크, 이메일"
              className={`${fieldClass} placeholder:text-faint`}
            />
          </div>
        </div>

        <div>
          <label htmlFor="event-source-url" className={labelClass}>
            Source URL — where this was found
          </label>
          <input
            id="event-source-url" type="url" value={draft.sourceUrl}
            onChange={(e) => set({ sourceUrl: e.target.value })}
            maxLength={500} placeholder="https://www.facebook.com/events/..."
            className={`${fieldClass} placeholder:text-faint`}
          />
        </div>

        <div>
          <label htmlFor="event-description" className={labelClass}>Description</label>
          <textarea
            id="event-description" value={draft.description}
            onChange={(e) => set({ description: e.target.value })}
            rows={8} className={`${fieldClass} resize-y leading-relaxed`}
          />
          {fieldErrors.description && <p className={errorClass}>{fieldErrors.description}</p>}
        </div>

        <div className="flex gap-2 mt-1">
          <button
            onClick={handleSubmit}
            disabled={busy || !draft.title.trim() || !draft.startsAt}
            className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium
                       bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <Check size={14} strokeWidth={2.5} />
            {busy ? 'Saving...' : submitLabel}
          </button>
          <button onClick={onCancel} className={secondaryBtn}>Cancel</button>
        </div>
      </div>
    </>
  );
}