'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Check, Info, CircleCheck } from 'lucide-react';
import { fetchBusinessCategories, createBusiness } from '@/api/businesses';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

// The API names fields as the DTO declares them; the inputs carry prefixed ids
// to avoid colliding with anything else on the page. This is the join between
// the two, and it has to be updated when either side changes.
const FIELD_IDS = {
  name: 'b-name',
  shortDescription: 'b-short',
  description: 'b-desc',
  phone: 'b-phone',
  email: 'b-email',
  websiteUrl: 'b-website',
  addressLine: 'b-address',
  suburb: 'b-suburb',
  state: 'b-state',
  postcode: 'b-postcode',
};

/**
 * Business submission. A client component in full: form state throughout, and
/**
 * Business submission. A client component in full: form state throughout, and
 * nothing here a crawler should see — a listing becomes public through the
 * directory once approved, not through this page.
 */
export default function BusinessCreatePage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();

  const [form, setForm] = useState({
    name: '',
    shortDescription: '',
    description: '',
    phone: '',
    email: '',
    websiteUrl: '',
    addressLine: '',
    suburb: '',
    state: 'SA',
    postcode: '',
  });
  const [categoryIds, setCategoryIds] = useState([]);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  // Field-level messages keyed by field name. The API returns these in
  // error.details; this form is long enough that a single line at the top could
  // sit several screens away from the input it refers to.
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  // Holds the submitted listing once accepted. A submission goes to PENDING and
  // so appears nowhere public — redirecting to the directory would show the
  // submitter a page their business is absent from, which reads as failure.
  const [submitted, setSubmitted] = useState(null);

  useEffect(() => {
    fetchBusinessCategories()
      .then((data) => setCategories(data ?? []))
      .catch((err) => console.error('Failed to load categories:', err));
  }, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function toggleCategory(id) {
    setCategoryIds((ids) =>
      ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]
    );
  }

  async function handleSubmit() {
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      // Empty strings are sent as null: the backend treats absent and blank
      // differently, and a listing showing an empty phone line is worse than
      // one showing no phone line.
      const payload = {
        ...Object.fromEntries(
          Object.entries(form).map(([k, v]) => [k, v.trim() || null])
        ),
        categoryIds,
      };
      const created = await createBusiness(payload);
      setSubmitted(created ?? { name: form.name });
    } catch (err) {
      const apiError = err.response?.data?.error;
      const details = apiError?.details ?? [];

      if (details.length > 0) {
        setFieldErrors(
          Object.fromEntries(details.map((d) => [d.field, d.message]))
        );
        setError('');

        // The submit button sits at the bottom of a long form, so the field
        // that failed is often above the fold. Marking it is not enough if
        // nobody scrolls back up to see the mark.
        const firstField = details[0].field;
        const target =
          document.getElementById(FIELD_IDS[firstField] ?? '') ?? null;
        target?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else {
        setError(apiError?.message || 'Could not submit this listing.');
      }
    } finally {
      setBusy(false);
    }
  }

  // Waits for the session before deciding. Rendering the logged-out state while
  // it is still unknown would flash a login prompt at someone already signed in.
  if (authLoading) {
    return (
      <PageShell>
        <div className="h-40 rounded-2xl bg-night border border-border-dark animate-pulse" />
      </PageShell>
    );
  }

  // Listings are attributed to nobody at present, so an anonymous submission
  // could not be corrected by whoever made it. Requiring a login also keeps the
  // review queue from filling with spam.
  if (!user) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <p className="text-snow font-medium mb-1">Log in to list your business</p>
          <p className="text-muted text-sm mb-5">
            Listings are reviewed before they appear, so we need to know who submitted them.
          </p>
          <Link
            href="/login"
            className="inline-block px-5 py-2.5 rounded-xl bg-korea-blue text-white text-sm font-medium
                       hover:bg-korea-blue/85 transition-colors"
          >
            Log in
          </Link>
        </div>
      </PageShell>
    );
  }

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[16px] md:text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  const labelClass = 'block text-[12px] font-medium text-muted mb-1.5';

  // Confirmation replaces the form rather than appearing as a dismissable
  // overlay: what happens next needs explaining, and a toast that vanishes
  // takes the explanation with it.
  if (submitted) {
    return (
      <PageShell>
        <div className="rounded-2xl border border-border-dark bg-night px-6 py-12 text-center">
          <CircleCheck size={28} strokeWidth={1.5} className="text-korea-blue mx-auto mb-3" />

          <h1 className="text-lg font-semibold text-snow mb-1">
            Thank you for your submission
          </h1>
          <p className="text-muted text-[15px] mb-5">등록해주셔서 감사합니다</p>

          <p className="text-muted text-sm mb-4">
            <span className="text-snow">{submitted.name}</span>
          </p>

          {/* English first, matching the rest of the interface, with Korean
              directly beneath rather than as a footnote. The directory is only
              as useful as the listings in it, and each one costs an owner their
              time — saying so is not decoration. */}
          <p className="text-faint text-[13px] mb-3 leading-relaxed max-w-md mx-auto">
            We will check the details before it appears in the directory,
            usually within a few days. Nothing further is needed from you.
          </p>
          <p className="text-faint text-[13px] mb-2 leading-relaxed max-w-md mx-auto">
            내용을 확인한 뒤 업소록에 업로드됩니다. 보통 며칠 정도 걸리며,
            그때까지는 목록에 보이지 않습니다. 따로 하실 일은 없습니다.
          </p>
          <p className="text-faint text-[13px] mb-6 leading-relaxed max-w-md mx-auto">
            애들레이드 한인 여러분께 도움이 되는 정보를 나눠주셔서 감사합니다.
          </p>

          {/* Card treatment rather than solid buttons: these two are choices to
              browse toward, not the primary action the form already completed. */}
          <div className="flex flex-wrap gap-3 justify-center">
            <Link
              href="/directory"
              className="group relative overflow-hidden rounded-2xl border border-border-dark
                         bg-night px-6 py-3 text-sm font-medium text-muted
                         hover:text-snow hover:border-faint hover:-translate-y-1
                         transition-all duration-300"
            >
              <span
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                           bg-gradient-to-r from-transparent via-korea-blue to-transparent
                           group-hover:opacity-100 transition-opacity duration-300"
              />
              Back to businesses
            </Link>

            <button
              onClick={() => {
                setSubmitted(null);
                setForm({
                  name: '', shortDescription: '', description: '', phone: '',
                  email: '', websiteUrl: '', addressLine: '', suburb: '',
                  state: 'SA', postcode: '',
                });
                setCategoryIds([]);
              }}
              className="group relative overflow-hidden rounded-2xl border border-border-dark
                         bg-night px-6 py-3 text-sm font-medium text-muted
                         hover:text-snow hover:border-faint hover:-translate-y-1
                         transition-all duration-300"
            >
              <span
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                           bg-gradient-to-r from-transparent via-korea-blue to-transparent
                           group-hover:opacity-100 transition-opacity duration-300"
              />
              List another
            </button>
          </div>
        </div>
      </PageShell>
    );
  }

  const canSubmit = form.name.trim() && categoryIds.length > 0 && !busy;

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

      <h1 className="text-xl font-bold text-snow mb-2">List your business</h1>

      {/* Says up front that this is not instant. A submitter who expects their
          listing to appear immediately will otherwise assume it failed. */}
      <div className="flex items-start gap-2 rounded-xl border border-border-dark bg-night px-4 py-3 mb-6">
        <Info size={16} strokeWidth={2} className="text-muted shrink-0 mt-0.5" />
        <p className="text-[13px] text-muted leading-relaxed">
          Listings are checked before they go live, usually within a few days.
          Only the name and at least one category are required — everything else
          can be added later.
        </p>
      </div>

      {error && <p className="text-adelaide-red text-[13px] mb-4">{error}</p>}

      <div className="flex flex-col gap-4">
        <div>
          <label htmlFor="b-name" className={labelClass}>
            Business name <span className="text-adelaide-red">*</span>
          </label>
          <input
            id="b-name"
            type="text"
            value={form.name}
            onChange={(e) => update('name', e.target.value)}
            maxLength={200}
            className={fieldClass}
          />
          {fieldErrors.name && (
            <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.name}</p>
          )}
        </div>

        <div>
          <span className={labelClass}>
            Categories <span className="text-adelaide-red">*</span>
          </span>
          <div className="flex flex-wrap gap-2">
            {categories.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => toggleCategory(c.id)}
                aria-pressed={categoryIds.includes(c.id)}
                className={`px-4 py-1.5 rounded-full text-[13px] border transition-all duration-300 ${
                  categoryIds.includes(c.id)
                    ? 'bg-snow text-night border-snow font-medium'
                    : 'bg-transparent text-muted border-border-dark hover:text-snow hover:border-faint'
                }`}
              >
                {c.name}
              </button>
            ))}
          </div>
          {fieldErrors.categoryIds && (
            <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.categoryIds}</p>
          )}
        </div>

        <div>
          <label htmlFor="b-short" className={labelClass}>
            Short description — one line, shown on cards
          </label>
          <input
            id="b-short"
            type="text"
            value={form.shortDescription}
            onChange={(e) => update('shortDescription', e.target.value)}
            maxLength={300}
            className={fieldClass}
          />
          {fieldErrors.shortDescription && (
            <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.shortDescription}</p>
          )}
        </div>

        <div>
          <label htmlFor="b-desc" className={labelClass}>Description</label>
          <textarea
            id="b-desc"
            value={form.description}
            onChange={(e) => update('description', e.target.value)}
            rows={6}
            className={`${fieldClass} resize-y leading-relaxed`}
          />
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="b-phone" className={labelClass}>Phone</label>
            <input
              id="b-phone"
              type="tel"
              autoComplete="tel"
              value={form.phone}
              onChange={(e) => update('phone', e.target.value)}
              className={fieldClass}
            />
          </div>
          <div>
            <label htmlFor="b-email" className={labelClass}>Email</label>
            <input
              id="b-email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={(e) => update('email', e.target.value)}
              className={fieldClass}
            />
          </div>
        </div>

        <div>
          <label htmlFor="b-website" className={labelClass}>Website</label>
          <input
            id="b-website"
            type="url"
            value={form.websiteUrl}
            onChange={(e) => update('websiteUrl', e.target.value)}
            placeholder="https://"
            className={`${fieldClass} placeholder:text-faint`}
          />
        </div>

        <div>
          <label htmlFor="b-address" className={labelClass}>Street address</label>
          <input
            id="b-address"
            type="text"
            autoComplete="street-address"
            value={form.addressLine}
            onChange={(e) => update('addressLine', e.target.value)}
            className={fieldClass}
          />
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <div className="col-span-2 sm:col-span-1">
            <label htmlFor="b-suburb" className={labelClass}>Suburb</label>
            <input
              id="b-suburb"
              type="text"
              value={form.suburb}
              onChange={(e) => update('suburb', e.target.value)}
              placeholder="e.g. Glenelg"
              className={`${fieldClass} placeholder:text-faint`}
            />
          </div>
          <div>
            <label htmlFor="b-state" className={labelClass}>State</label>
            <input
              id="b-state"
              type="text"
              value={form.state}
              onChange={(e) => update('state', e.target.value)}
              className={fieldClass}
            />
          </div>
          <div>
            <label htmlFor="b-postcode" className={labelClass}>Postcode</label>
            <input
              id="b-postcode"
              type="text"
              inputMode="numeric"
              value={form.postcode}
              onChange={(e) => update('postcode', e.target.value)}
              className={fieldClass}
            />
          </div>
        </div>

        <div className="flex gap-2 mt-1">
          <button
            onClick={handleSubmit}
            disabled={!canSubmit}
            className="flex items-center gap-1.5 text-sm px-4 py-2.5 rounded-xl font-medium
                       bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <Check size={15} strokeWidth={2.5} />
            {busy ? 'Submitting…' : 'Submit for review'}
          </button>
          <button
            onClick={() => router.back()}
            className="text-sm px-4 py-2.5 rounded-xl border border-border-dark
                       text-muted hover:text-snow hover:border-faint transition-colors"
          >
            Cancel
          </button>
        </div>
      </div>
    </PageShell>
  );
}