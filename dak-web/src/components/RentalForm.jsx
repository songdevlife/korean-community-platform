'use client';

import { useState } from 'react';
import { Check, Plus, X, Lock } from 'lucide-react';

const FIELD_IDS = {
  title: 'rental-title',
  slug: 'rental-slug',
  suburb: 'rental-suburb',
  rentMin: 'rental-rent-min',
  consentStatus: 'rental-consent-status',
  sourceUrl: 'rental-source-url',
};

const EMPTY = {
  title: '', slug: '', description: '', suburb: '',
  listingType: 'SHARE_ROOM', roomTypes: [],
  rentMin: '', rentMax: '', bondWeeks: '',
  billsIncluded: 'UNKNOWN', billsNote: '',
  availableFrom: '', minTermMonths: '', furnished: false, roomsLet: '',
  genderPreference: '', couplesAllowed: '', petsAllowed: '', smokingAllowed: '',
  inspectionNote: '', contactLanguage: 'UNKNOWN',
  consentStatus: 'NONE', consentNote: '', sourceUrl: '',
  contact: '', imageUrls: [''],
};

const LISTING_TYPES = [
  { value: 'SHARE_ROOM', label: '방 임대' },
  { value: 'WHOLE_PROPERTY', label: '집 전체' },
  { value: 'LEASE_TRANSFER', label: '계약 승계' },
  { value: 'STUDENT_ACCOMMODATION', label: '학생 숙소' },
];

const ROOM_TYPES = [
  { value: 'SINGLE', label: '단독방' },
  { value: 'SHARED', label: '셰어룸' },
  { value: 'WHOLE', label: '집 전체' },
];

const BILLS = [
  { value: 'UNKNOWN', label: '정보 없음' },
  { value: 'INCLUDED', label: '포함' },
  { value: 'EXCLUDED', label: '별도' },
  { value: 'OPTIONAL', label: '선택 가능' },
];

// UNKNOWN is the default and stays selectable, because an external listing
// has never been spoken to and choosing one of the other three would be a
// guess. A listing wrongly marked English-only stops a Korean speaker
// writing to someone who would have answered.
const CONTACT_LANGUAGES = [
  { value: 'UNKNOWN', label: '확인 안 됨 — 표시하지 않음' },
  { value: 'KOREAN', label: '한국어 가능' },
  { value: 'ENGLISH', label: '영어만' },
  { value: 'BOTH', label: '한국어·영어' },
];

const CONSENT = [
  {
    value: 'NONE',
    label: 'NONE — 동의 없음',
    hint: '사실만 게재합니다. 연락처와 사진은 넣을 수 없고, 원문 링크가 필수입니다.',
  },
  {
    value: 'LINK_ONLY',
    label: 'LINK_ONLY — 링크만',
    hint: '동의 없이 게재하되 원문으로 연결합니다. 연락처와 사진은 넣을 수 없습니다.',
  },
  {
    value: 'FULL',
    label: 'FULL — 게시자 동의 받음',
    hint: '연락처와 사진을 게재할 수 있습니다. 받은 날짜와 경로를 아래에 기록하세요.',
  },
];

/**
 * The rental fields, shared by the create and edit screens.
 *
 * The form's job beyond collecting values is to make the consent rule visible
 * before it is broken. The server refuses a contact or an image where consent
 * is not FULL, and a form that let an administrator type one and then rejected
 * it would teach them to work around the rule rather than with it. So the
 * fields are disabled, and the reason is printed where the input would be.
 *
 * @param {object}   [initial]   Existing values, already in form shape
 * @param {function} onSubmit    Receives the payload; throws to report failure
 * @param {string}   submitLabel
 * @param {boolean}  [showSlug]  Create only - a published address is not edited
 */
export default function RentalForm({
  initial, onSubmit, submitLabel = 'Create', onCancel, showSlug = false,
}) {
  const [draft, setDraft] = useState({ ...EMPTY, ...initial });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const consented = draft.consentStatus === 'FULL';

  const set = (patch) => setDraft({ ...draft, ...patch });

  const toggleRoomType = (value) =>
    set({
      roomTypes: draft.roomTypes.includes(value)
        ? draft.roomTypes.filter((v) => v !== value)
        : [...draft.roomTypes, value],
    });

  const MAX_IMAGES = 10;

  const setImageUrl = (index, value) =>
    set({ imageUrls: draft.imageUrls.map((u, i) => (i === index ? value : u)) });

  const removeImageUrl = (index) =>
    set({
      imageUrls: draft.imageUrls.length === 1
        ? ['']
        : draft.imageUrls.filter((_, i) => i !== index),
    });

  const addImageUrl = () =>
    draft.imageUrls.length < MAX_IMAGES && set({ imageUrls: [...draft.imageUrls, ''] });

  // Empty string means "not stated", which is different from false. A listing
  // that does not mention pets should not claim they are banned.
  const triState = (value) => (value === '' ? null : value === 'true');
  const numberOrNull = (value) => (value === '' ? null : Number(value));

  async function handleSubmit() {
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      await onSubmit({
        title: draft.title.trim(),
        ...(showSlug && { slug: draft.slug.trim() || null }),
        description: draft.description.trim() || null,
        suburb: draft.suburb.trim(),
        listingType: draft.listingType,
        roomTypes: draft.roomTypes.length > 0 ? draft.roomTypes.join(',') : null,
        rentMin: numberOrNull(draft.rentMin),
        rentMax: numberOrNull(draft.rentMax),
        bondWeeks: draft.bondWeeks === '' ? null : draft.bondWeeks,
        billsIncluded: draft.billsIncluded,
        billsNote: draft.billsNote.trim() || null,
        availableFrom: draft.availableFrom || null,
        minTermMonths: numberOrNull(draft.minTermMonths),
        furnished: draft.furnished,
        roomsLet: numberOrNull(draft.roomsLet),
        genderPreference: draft.genderPreference.trim() || null,
        couplesAllowed: triState(draft.couplesAllowed),
        petsAllowed: triState(draft.petsAllowed),
        smokingAllowed: triState(draft.smokingAllowed),
        inspectionNote: draft.inspectionNote.trim() || null,
        contactLanguage: draft.contactLanguage,
        consentStatus: draft.consentStatus,
        consentNote: draft.consentNote.trim() || null,
        sourceUrl: draft.sourceUrl.trim() || null,
        // Sent only where permitted. The server refuses them otherwise, and
        // sending a value it will reject would fail a save for a field the
        // administrator could not see.
        contact: consented ? (draft.contact.trim() || null) : null,
        imageUrls: consented
          ? draft.imageUrls.map((u) => u.trim()).filter(Boolean)
          : [],
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
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark] ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';
  const labelClass = 'block text-[12px] font-medium text-muted mb-1.5';
  const errorClass = 'text-adelaide-red text-[12px] mt-1.5';
  const hintClass = 'text-faint text-[12px] mt-1.5 leading-relaxed';
  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';
  const chipClass = (active) =>
    `text-[13px] px-3 py-1.5 rounded-full border transition-colors ${
      active
        ? 'bg-surface border-faint text-snow font-medium'
        : 'border-border-dark text-muted hover:text-snow hover:border-faint'
    }`;

  return (
    <>
      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex flex-col gap-3">

        {/* Consent first, not last. It decides which of the fields below can
            be filled in at all, and an administrator who meets it after
            typing a phone number has already done the wrong work. */}
        <div className="rounded-xl border border-border-dark bg-night p-4">
          <label htmlFor="rental-consent-status" className={labelClass}>
            게시자 동의 — 먼저 정하세요
          </label>
          <select
            id="rental-consent-status" value={draft.consentStatus}
            onChange={(e) => set({ consentStatus: e.target.value })}
            className={fieldClass}
          >
            {CONSENT.map((c) => (
              <option key={c.value} value={c.value} className="bg-surface">{c.label}</option>
            ))}
          </select>
          <p className={hintClass}>
            {CONSENT.find((c) => c.value === draft.consentStatus)?.hint}
          </p>

          {consented && (
            <div className="mt-3">
              <label htmlFor="rental-consent-note" className={labelClass}>
                동의 기록 — 받은 날짜와 경로
              </label>
              <input
                id="rental-consent-note" type="text" value={draft.consentNote}
                onChange={(e) => set({ consentNote: e.target.value })}
                placeholder="2026-08-07 페이스북 DM, 스크린샷 보관"
                className={`${fieldClass} placeholder:text-faint`}
              />
            </div>
          )}
        </div>

        <div>
          <label htmlFor="rental-title" className={labelClass}>제목</label>
          <input
            id="rental-title" type="text" value={draft.title}
            onChange={(e) => set({ title: e.target.value })}
            maxLength={300} className={fieldClass}
          />
          {fieldErrors.title && <p className={errorClass}>{fieldErrors.title}</p>}
        </div>

        {showSlug && (
          <div>
            <label htmlFor="rental-slug" className={labelClass}>
              Slug — 영문 소문자와 하이픈
            </label>
            <input
              id="rental-slug" type="text" value={draft.slug}
              onChange={(e) => set({ slug: e.target.value })}
              maxLength={200} placeholder="furnished-room-prospect"
              className={`${fieldClass} placeholder:text-faint`}
            />
            <p className={hintClass}>
              비워 두면 제목에서 만들어집니다. 한국어 제목이면 날짜만 남으니 채워 주세요.
            </p>
          </div>
        )}

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="rental-suburb" className={labelClass}>지역 (suburb)</label>
            <input
              id="rental-suburb" type="text" value={draft.suburb}
              onChange={(e) => set({ suburb: e.target.value })}
              maxLength={120} placeholder="Prospect"
              className={`${fieldClass} placeholder:text-faint`}
            />
            {fieldErrors.suburb && <p className={errorClass}>{fieldErrors.suburb}</p>}
          </div>
          <div>
            <label htmlFor="rental-listing-type" className={labelClass}>매물 종류</label>
            <select
              id="rental-listing-type" value={draft.listingType}
              onChange={(e) => set({ listingType: e.target.value })}
              className={fieldClass}
            >
              {LISTING_TYPES.map((t) => (
                <option key={t.value} value={t.value} className="bg-surface">{t.label}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Several at once: a property is often offered as a single room or
            as the whole place, depending on who answers. */}
        <div>
          <label className={labelClass}>방 형태 — 해당하는 것 모두</label>
          <div className="flex flex-wrap gap-2">
            {ROOM_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => toggleRoomType(t.value)}
                className={chipClass(draft.roomTypes.includes(t.value))}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <label htmlFor="rental-rent-min" className={labelClass}>주급 ($)</label>
            <input
              id="rental-rent-min" type="number" value={draft.rentMin}
              onChange={(e) => set({ rentMin: e.target.value })}
              min={0} className={fieldClass}
            />
            {fieldErrors.rentMin && <p className={errorClass}>{fieldErrors.rentMin}</p>}
          </div>
          <div>
            <label htmlFor="rental-rent-max" className={labelClass}>상한 ($) — 범위일 때만</label>
            <input
              id="rental-rent-max" type="number" value={draft.rentMax}
              onChange={(e) => set({ rentMax: e.target.value })}
              min={0} className={fieldClass}
            />
          </div>
          <div>
            <label htmlFor="rental-bond-weeks" className={labelClass}>본드 (주)</label>
            <input
              id="rental-bond-weeks" type="number" step="0.5" value={draft.bondWeeks}
              onChange={(e) => set({ bondWeeks: e.target.value })}
              min={0} className={fieldClass}
            />
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="rental-bills" className={labelClass}>빌 (전기·수도·인터넷)</label>
            <select
              id="rental-bills" value={draft.billsIncluded}
              onChange={(e) => set({ billsIncluded: e.target.value })}
              className={fieldClass}
            >
              {BILLS.map((b) => (
                <option key={b.value} value={b.value} className="bg-surface">{b.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="rental-bills-note" className={labelClass}>빌 설명</label>
            <input
              id="rental-bills-note" type="text" value={draft.billsNote}
              onChange={(e) => set({ billsNote: e.target.value })}
              maxLength={300} placeholder="$50/주 (전기·수도·인터넷·청소)"
              className={`${fieldClass} placeholder:text-faint`}
            />
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <label htmlFor="rental-available-from" className={labelClass}>입주 가능일</label>
            <input
              id="rental-available-from" type="date" value={draft.availableFrom}
              onChange={(e) => set({ availableFrom: e.target.value })}
              className={fieldClass}
            />
          </div>
          <div>
            <label htmlFor="rental-min-term" className={labelClass}>최소 기간 (개월)</label>
            <input
              id="rental-min-term" type="number" value={draft.minTermMonths}
              onChange={(e) => set({ minTermMonths: e.target.value })}
              min={0} className={fieldClass}
            />
          </div>
          <div>
            {/* The field that earns this section its place: two or more rooms
                let brings the property under Part 7, one leaves the occupant
                outside the Act. Rarely stated, so blank is expected and the
                detail page says so rather than guessing. */}
            <label htmlFor="rental-rooms-let" className={labelClass}>세놓는 방 개수</label>
            <input
              id="rental-rooms-let" type="number" value={draft.roomsLet}
              onChange={(e) => set({ roomsLet: e.target.value })}
              min={0} className={fieldClass}
            />
          </div>
        </div>
        <p className={hintClass}>
          방 개수는 임대차법 적용 여부를 가릅니다. 2개 이상이면 rooming house,
          집주인이 사는 집에 1개면 법 적용 밖입니다. 모르면 비워 두세요.
        </p>

        <div>
          <label className="flex items-center gap-2 text-[14px] text-snow cursor-pointer w-fit">
            <input
              type="checkbox" checked={draft.furnished}
              onChange={(e) => set({ furnished: e.target.checked })}
              className="w-4 h-4 accent-korea-blue cursor-pointer"
            />
            가구 포함
          </label>
        </div>

        <div className="grid gap-3 sm:grid-cols-4">
          <div>
            <label htmlFor="rental-gender" className={labelClass}>성별 조건</label>
            <input
              id="rental-gender" type="text" value={draft.genderPreference}
              onChange={(e) => set({ genderPreference: e.target.value })}
              maxLength={20} placeholder="여성만"
              className={`${fieldClass} placeholder:text-faint`}
            />
          </div>
          {[
            ['couplesAllowed', '커플'],
            ['petsAllowed', '반려동물'],
            ['smokingAllowed', '흡연'],
          ].map(([key, label]) => (
            <div key={key}>
              <label htmlFor={`rental-${key}`} className={labelClass}>{label}</label>
              <select
                id={`rental-${key}`} value={draft[key]}
                onChange={(e) => set({ [key]: e.target.value })}
                className={fieldClass}
              >
                <option value="" className="bg-surface">언급 없음</option>
                <option value="true" className="bg-surface">가능</option>
                <option value="false" className="bg-surface">불가</option>
              </select>
            </div>
          ))}
        </div>

        <div>
          <label htmlFor="rental-inspection" className={labelClass}>인스펙션 안내</label>
          <input
            id="rental-inspection" type="text" value={draft.inspectionNote}
            onChange={(e) => set({ inspectionNote: e.target.value })}
            maxLength={300} placeholder="1월 19일 (월) 오전 9시"
            className={`${fieldClass} placeholder:text-faint`}
          />
        </div>

        <div>
          <label htmlFor="rental-contact-language" className={labelClass}>
            문의 가능 언어
          </label>
          <select
            id="rental-contact-language" value={draft.contactLanguage}
            onChange={(e) => set({ contactLanguage: e.target.value })}
            className={fieldClass}
          >
            {CONTACT_LANGUAGES.map((l) => (
              <option key={l.value} value={l.value} className="bg-surface">{l.label}</option>
            ))}
          </select>
          <p className={hintClass}>
            게시자가 직접 알려주었거나 원문에서 확인된 경우에만 고르세요.
            추측해서 표시하면 연락할 수 있는 사람을 막게 됩니다.
          </p>
        </div>

        <div>
          <label htmlFor="rental-source-url" className={labelClass}>
            원문 URL — 동의를 받지 않았다면 필수
          </label>
          <input
            id="rental-source-url" type="url" value={draft.sourceUrl}
            onChange={(e) => set({ sourceUrl: e.target.value })}
            maxLength={500} placeholder="https://www.gumtree.com.au/..."
            className={`${fieldClass} placeholder:text-faint`}
          />
          {fieldErrors.sourceUrl && <p className={errorClass}>{fieldErrors.sourceUrl}</p>}
        </div>

        {/* Locked unless consent is FULL, with the reason where the input
            would be. Disabled rather than hidden: a field that vanishes reads
            as a bug, and one that explains itself teaches the rule. */}
        <div className="rounded-xl border border-border-dark p-4">
          <div className="flex items-center gap-2 mb-3">
            {!consented && <Lock size={14} strokeWidth={2} className="text-faint" />}
            <h3 className="text-[13px] font-medium text-snow">
              연락처와 사진 {!consented && '— 동의가 있어야 입력할 수 있습니다'}
            </h3>
          </div>

          {!consented && (
            <p className="text-faint text-[12px] mb-3 leading-relaxed">
              게시자의 동의 없이 연락처나 사진을 싣지 않습니다. 매물 정보는 사실만 옮기고,
              문의는 원문 링크로 보냅니다. 동의를 받았다면 위에서 FULL로 바꾸세요.
            </p>
          )}

          <div className="mb-3">
            <label htmlFor="rental-contact" className={labelClass}>연락처</label>
            <input
              id="rental-contact" type="text" value={draft.contact}
              onChange={(e) => set({ contact: e.target.value })}
              disabled={!consented} maxLength={300}
              placeholder="0412 345 678 또는 https://..."
              className={`${fieldClass} placeholder:text-faint`}
            />
          </div>

          <label className={labelClass}>사진 URL</label>
          <div className="flex flex-col gap-2">
            {draft.imageUrls.map((url, index) => (
              <div key={index} className="flex items-center gap-2">
                <input
                  type="url" value={url}
                  onChange={(e) => setImageUrl(index, e.target.value)}
                  disabled={!consented} maxLength={500}
                  placeholder="https://res.cloudinary.com/..."
                  className={`${fieldClass} placeholder:text-faint`}
                />
                <button
                  type="button"
                  onClick={() => removeImageUrl(index)}
                  disabled={!consented}
                  aria-label={`Remove image ${index + 1}`}
                  className="shrink-0 p-2 rounded-lg border border-border-dark text-muted
                             hover:text-adelaide-red hover:border-adelaide-red transition-colors
                             disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <X size={14} strokeWidth={2} />
                </button>
              </div>
            ))}
          </div>

          {consented && draft.imageUrls.length < MAX_IMAGES && (
            <button type="button" onClick={addImageUrl} className={`${secondaryBtn} mt-2`}>
              <Plus size={14} strokeWidth={2} />
              사진 추가
            </button>
          )}

          {consented && (
            <p className="text-faint text-[12px] mt-2 leading-relaxed">
              게시자가 직접 보내준 사진만 사용하세요. 거주자가 식별되는 사진이나 다른 사람의
              소지품이 보이는 사진은 게시자의 동의와 별개로 사용하지 않습니다.
            </p>
          )}
        </div>

        <div>
          <label htmlFor="rental-description" className={labelClass}>설명</label>
          <textarea
            id="rental-description" value={draft.description}
            onChange={(e) => set({ description: e.target.value })}
            rows={8} className={`${fieldClass} resize-y leading-relaxed`}
          />
          <p className={hintClass}>
            원문을 그대로 옮기지 말고 사실만 정리해 주세요.
          </p>
        </div>

        <div className="flex gap-2 mt-1">
          <button
            onClick={handleSubmit}
            disabled={busy || !draft.title.trim() || !draft.suburb.trim() || draft.rentMin === ''}
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