'use client';

import { useState, useEffect } from 'react';
import { Check, Plus, X } from 'lucide-react';
import { fetchEventCategories } from '@/api/events';

const FIELD_IDS = {
  title: 'event-title',
  slug: 'event-slug',
  startsAt: 'event-starts-at',
  endsAt: 'event-ends-at',
  description: 'event-description',
};

const EMPTY = {
  title: '', slug: '', description: '', startsAt: '', endsAt: '',
  venueName: '', venueAddress: '', isFree: false, priceNote: '',
  organiser: '', organiserContact: '', sourceUrl: '', imageUrls: [''],
  categoryId: '',
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
export default function EventForm({
  initial, onSubmit, submitLabel = 'Create', onCancel, showSlug = false,
}) {
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
        // Only where the form offered it. The update endpoint has no slug
        // field, so sending one there would be silently discarded - and a
        // value that appears to save but does not is worse than no field.
        ...(showSlug && { slug: draft.slug.trim() || null }),
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
        // Blank rows dropped rather than sent. The backend drops them too, but
        // an empty array here means "remove every image" on update, so what is
        // sent has to be what was meant.
        imageUrls: draft.imageUrls.map((u) => u.trim()).filter(Boolean),
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

    const MAX_IMAGES = 10;
  
    const setImageUrl = (index, value) =>
      set({ imageUrls: draft.imageUrls.map((u, i) => (i === index ? value : u)) });
  
    // Never removes the last row: a form with no input at all gives nothing to
    // type into, and "add an image" as the only control reads as a feature
    // rather than a field.
    const removeImageUrl = (index) =>
      set({
        imageUrls: draft.imageUrls.length === 1
          ? ['']
          : draft.imageUrls.filter((_, i) => i !== index),
      });
  
    const addImageUrl = () =>
      draft.imageUrls.length < MAX_IMAGES && set({ imageUrls: [...draft.imageUrls, ''] });

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

        {/* Set once, at creation. Changing it later would break every link
            already shared, and the update endpoint deliberately refuses one -
            which is why this is hidden on the edit screen rather than
            disabled there.

            English, because the title almost never is: a Korean title reduces
            to nothing under slugify and falls back to a date, which is valid
            and says nothing to a reader or a search engine. */}
        {showSlug && (
          <div>
            <label htmlFor="event-slug" className={labelClass}>
              Slug — English, lowercase, hyphens. The start date is appended
              automatically.
            </label>
            <input
              id="event-slug" type="text" value={draft.slug}
              onChange={(e) => set({ slug: e.target.value })}
              maxLength={200} placeholder="free-english-conversation-class"
              className={`${fieldClass} placeholder:text-faint`}
            />
            {fieldErrors.slug && <p className={errorClass}>{fieldErrors.slug}</p>}
            <p className="text-faint text-[12px] mt-1.5 leading-relaxed">
              비워 두면 날짜만으로 주소가 만들어집니다. 검색에 도움이 되므로
              가능하면 채워 주세요.
            </p>
          </div>
        )}

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
          <label htmlFor="event-image-url-0" className={labelClass}>
            Poster URLs — only with permission
          </label>

          <div className="flex flex-col gap-2">
            {draft.imageUrls.map((url, index) => (
              <div key={index} className="flex items-center gap-2">
                <input
                  id={`event-image-url-${index}`} type="url" value={url}
                  onChange={(e) => setImageUrl(index, e.target.value)}
                  maxLength={500}
                  placeholder={index === 0
                    ? 'https://res.cloudinary.com/... (첫 번째가 대표 이미지)'
                    : 'https://res.cloudinary.com/...'}
                  className={`${fieldClass} placeholder:text-faint`}
                />
                <button
                  type="button"
                  onClick={() => removeImageUrl(index)}
                  aria-label={`Remove image ${index + 1}`}
                  className="shrink-0 p-2 rounded-lg border border-border-dark text-muted
                             hover:text-adelaide-red hover:border-adelaide-red transition-colors"
                >
                  <X size={14} strokeWidth={2} />
                </button>
              </div>
            ))}
          </div>

          {draft.imageUrls.length < MAX_IMAGES && (
            <button type="button" onClick={addImageUrl} className={`${secondaryBtn} mt-2`}>
              <Plus size={14} strokeWidth={2} />
              이미지 추가
            </button>
          )}

          {/* The facts of an event are not copyright; the poster is. Listing
              an event needs no permission, using its artwork does. The second
              line is the lesson from the first photograph uploaded here, which
              turned out to be twenty attendees rather than a poster. */}
          <p className="text-faint text-[12px] mt-2 leading-relaxed">
            주최자의 허락을 받은 이미지만 사용하세요. 참석자의 얼굴이 식별되는 사진은
            주최자의 허락과 별개이므로 사용하지 않습니다.
          </p>
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