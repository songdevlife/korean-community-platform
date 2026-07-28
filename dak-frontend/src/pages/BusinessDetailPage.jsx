import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Heart, BadgeCheck, Phone, Navigation, Globe,
  MapPin, Mail, ImageOff, ChevronLeft, ChevronRight,
  Pencil, Check, Undo2,
} from 'lucide-react';
import apiClient from '../api/client';
import { updateBusiness, updateBusinessStatus } from '../api/admin';
import { saveItem, checkIsSaved, removeSavedByResource } from '../api/savedItems';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import CopyButton from '../components/CopyButton';
import PageMeta from '../components/PageMeta';
import StructuredData from '../components/StructuredData';
import { businessSchema } from '../utils/schema';

function BusinessDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [business, setBusiness] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saving, setSaving] = useState(false);
  const [imageIndex, setImageIndex] = useState(0);

  // Admin editing state. Kept separate from `business` so cancelling discards
  // changes without a refetch. Limited to the fields worth correcting while
  // looking at the page — address and coordinates cannot be verified by eye.
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(null);
  const [adminError, setAdminError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  useEffect(() => {
    async function loadBusiness() {
      try {
        const response = await apiClient.get(`/businesses/${slug}`);
        const businessData = response.data.data;
        setBusiness(businessData);
        setImageIndex(0);

        if (user) {
          const alreadySaved = await checkIsSaved('BUSINESS', businessData.id);
          setSaved(alreadySaved);
        }
      } catch (error) {
        if (error.response?.status === 404) {
          setNotFound(true);
        }
        console.error('Failed to load business:', error);
      } finally {
        setLoading(false);
      }
    }
    loadBusiness();
  }, [slug, user]);

  /**
   * Toggles rather than only saving. A control that fills in and then refuses to
   * respond reads as broken, and an accidental save would otherwise be
   * undoable only from the favourites list. Both controls on this page — the
   * mobile heart and the desktop rail button — call this.
   */
  async function toggleSave() {
    if (!user) {
      setSaveError('Please log in to save.');
      return;
    }

    setSaving(true);
    setSaveError('');

    try {
      if (saved) {
        await removeSavedByResource('BUSINESS', business.id);
        setSaved(false);
      } else {
        await saveItem('BUSINESS', business.id);
        setSaved(true);
      }
    } catch (error) {
      // 409 on save means it was already saved — the state the caller wanted
      // either way, so treat it as success rather than an error.
      if (!saved && error.response?.status === 409) {
        setSaved(true);
      } else {
        setSaveError(saved ? 'Could not remove. Please try again.' : 'Could not save. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  }

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
    setAdminError('');
  }

  async function saveEdits() {
    setAdminError('');
    setBusy(true);
    try {
      const updated = await updateBusiness(business.id, draft);
      // Merge rather than replace: the detail response is complete, but keeping
      // the merge makes the intent explicit if fields diverge later.
      setBusiness((b) => ({ ...b, ...updated }));
      setEditing(false);
    } catch (err) {
      setAdminError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setBusy(false);
    }
  }

  /**
   * Returns the listing to the review queue rather than archiving it — an admin
   * unpublishing from the listing itself is usually acting on something they
   * just noticed while reading, which is a correction rather than a retirement.
   * Archiving stays in the admin queue.
   */
  async function unpublish() {
    setAdminError('');
    setBusy(true);
    try {
      await updateBusinessStatus(business.id, 'PENDING');
      navigate('/admin');
    } catch (err) {
      setAdminError(err.response?.data?.error?.message || 'Could not unpublish.');
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <Layout>
        <PageShell>
          <div className="h-6 w-24 rounded bg-night animate-pulse mb-6" />
          <div className="h-48 rounded-xl bg-night animate-pulse mb-4" />
          <div className="h-8 w-2/3 rounded bg-night animate-pulse" />
        </PageShell>
      </Layout>
    );
  }

  if (notFound || !business) {
    return (
      <Layout>
        <PageShell>
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-6"
          >
            <ArrowLeft size={18} strokeWidth={1.75} />
            Back
          </button>
          <p className="text-muted text-sm">This business could not be found.</p>
        </PageShell>
      </Layout>
    );
  }

  const fullAddress = [business.addressLine, business.suburb].filter(Boolean).join(', ');
  const directionsUrl = fullAddress
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(fullAddress)}`
    : null;

  const images = business.images ?? [];
  const hasMultiple = images.length > 1;

  // Wrap around at both ends so the arrows never dead-end.
  const showPrev = () =>
    setImageIndex((i) => (i - 1 + images.length) % images.length);
  const showNext = () =>
    setImageIndex((i) => (i + 1) % images.length);

  const actionClass =
    'flex flex-col items-center justify-center gap-1.5 py-3 rounded-xl border border-border-dark ' +
    'text-[13px] text-snow hover:bg-night transition-colors';

  const arrowClass =
    'absolute top-1/2 -translate-y-1/2 p-2 rounded-full bg-night/70 text-snow ' +
    'hover:bg-night transition-colors backdrop-blur-sm';

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  const labelClass = 'block text-[12px] font-medium text-muted mb-1.5';

  return (
    <Layout>
      <PageShell>
      <PageMeta
          title={business.name}
          path={`/businesses/${business.slug}`}
          description={
            business.shortDescription ||
            `${business.name} in ${business.suburb || 'Adelaide'}.`
          }
          image={business.images?.[0]?.imageUrl}
        />
        <StructuredData data={businessSchema(business)} />
        {/* Top bar: back on the left, save on the right. The save control is
            hidden above 1024px, where the contact rail carries its own. */}
        <div className="flex items-center justify-between mb-5">
          <button
            onClick={() => navigate(-1)}
            aria-label="Back"
            className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
          >
            <ArrowLeft size={18} strokeWidth={1.75} />
            <span className="hidden sm:inline">Back</span>
          </button>

          <button
            onClick={toggleSave}
            disabled={saving}
            aria-label={saved ? 'Remove from favourites' : 'Save to favourites'}
            aria-pressed={saved}
            className={`lg:hidden p-2 -mr-2 rounded-lg transition-colors
                        disabled:opacity-60 disabled:cursor-not-allowed ${
              saved ? 'text-adelaide-red' : 'text-muted hover:text-snow hover:bg-night'
            }`}
          >
            <Heart size={20} strokeWidth={1.75} fill={saved ? 'currentColor' : 'none'} />
          </button>
        </div>

        {/* Save errors surface here rather than only in the rail, which is
            hidden on mobile — the log-in prompt for a guest tapping the heart
            would otherwise never be seen. */}
        {saveError && <p className="text-adelaide-red text-[13px] mb-4 lg:hidden">{saveError}</p>}

        {/* Admin controls. Shown on the listing itself so a problem spotted
            while reading can be fixed without hunting for it in the queue. */}
        {isAdmin && !editing && (
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

        {adminError && <p className="text-adelaide-red text-[13px] mb-4">{adminError}</p>}

          {/* Centred rather than left-aligned. The article is capped at 640px for
            line length and the rail is fixed at 288px, so on a wide screen the
            pair occupies barely half the viewport — anchoring them left leaves
            the remainder as dead space on one side only, which reads as a
            layout fault rather than a margin. */}
        <div className="flex flex-col lg:flex-row gap-6 lg:justify-center">

          <article className="w-full min-w-0 lg:max-w-[640px]">
            {/* Image carousel. Arrows and counter only appear when there is
                more than one image. */}
            <div className="relative w-full rounded-xl border border-border-dark bg-night
                            h-52 sm:h-64 lg:h-96
                            flex items-center justify-center overflow-hidden mb-5">
              {images.length > 0 ? (
                <>
                  <img
                    src={images[imageIndex].imageUrl}
                    alt={images[imageIndex].altText || ''}
                    className="w-full h-full object-cover"
                  />

                  {hasMultiple && (
                    <>
                      <button
                        onClick={showPrev}
                        aria-label="Previous image"
                        className={`${arrowClass} left-3`}
                      >
                        <ChevronLeft size={18} strokeWidth={2} />
                      </button>
                      <button
                        onClick={showNext}
                        aria-label="Next image"
                        className={`${arrowClass} right-3`}
                      >
                        <ChevronRight size={18} strokeWidth={2} />
                      </button>

                      <span className="absolute bottom-3 right-3 px-2 py-0.5 rounded-md
                                       bg-night/70 backdrop-blur-sm text-[12px] text-snow">
                        {imageIndex + 1} / {images.length}
                      </span>
                    </>
                  )}
                </>
              ) : (
                <ImageOff size={28} strokeWidth={1.5} className="text-border-dark" />
              )}
            </div>

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
              <>
                <div className="flex items-start gap-2 mb-1">
                  <h1 className="text-2xl font-bold text-snow leading-tight">
                    {business.name}
                  </h1>
                  {business.verified && (
                    <BadgeCheck
                      size={20}
                      strokeWidth={2}
                      className="text-korea-blue shrink-0 mt-1"
                      aria-label="Verified business"
                    />
                  )}
                </div>

                {business.shortDescription && (
                  <p className="text-sm text-muted mb-4">{business.shortDescription}</p>
                )}

                {business.categories?.length > 0 && (
                  <div className="flex flex-wrap gap-2 mb-5">
                    {business.categories.map((category) => (
                      <span
                        key={category.id}
                        className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-muted"
                      >
                        {category.name}
                      </span>
                    ))}
                  </div>
                )}

                {/* Primary actions, per the business detail wireframe */}
                <div className="grid grid-cols-3 gap-2 mb-5">
                  {business.phone ? (
                    <a href={`tel:${business.phone}`} className={actionClass}>
                      <Phone size={18} strokeWidth={1.75} />
                      Call
                    </a>
                  ) : (
                    <span className={`${actionClass} opacity-40 cursor-default`}>
                      <Phone size={18} strokeWidth={1.75} />
                      Call
                    </span>
                  )}

                  {directionsUrl ? (
                    <a href={directionsUrl} target="_blank" rel="noreferrer" className={actionClass}>
                      <Navigation size={18} strokeWidth={1.75} />
                      Directions
                    </a>
                  ) : (
                    <span className={`${actionClass} opacity-40 cursor-default`}>
                      <Navigation size={18} strokeWidth={1.75} />
                      Directions
                    </span>
                  )}

                  {business.websiteUrl ? (
                    <a href={business.websiteUrl} target="_blank" rel="noreferrer" className={actionClass}>
                      <Globe size={18} strokeWidth={1.75} />
                      Website
                    </a>
                  ) : (
                    <span className={`${actionClass} opacity-40 cursor-default`}>
                      <Globe size={18} strokeWidth={1.75} />
                      Website
                    </span>
                  )}
                </div>

                {business.description && (
                  <p className="text-[15px] leading-relaxed text-snow whitespace-pre-line">
                    {business.description}
                  </p>
                )}
              </>
            )}

            {/* Contact details render inline on mobile; the desktop rail
                shows them instead. Each row keeps its native action (call,
                email) and offers copy as a separate control. */}
            <div className="lg:hidden mt-8 pt-5 border-t border-border-dark">
              <h2 className="text-sm font-semibold text-snow mb-3">Contact</h2>
              <div className="flex flex-col gap-3">
                {fullAddress && (
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-2.5 text-sm text-snow min-w-0">
                      <MapPin size={16} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                      {fullAddress}
                    </div>
                    <CopyButton value={fullAddress} label="Copy address" />
                  </div>
                )}

                {business.phone && (
                  <div className="flex items-start justify-between gap-3">
                    <a
                      href={`tel:${business.phone}`}
                      className="flex items-start gap-2.5 text-sm text-snow min-w-0"
                    >
                      <Phone size={16} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                      {business.phone}
                    </a>
                    <CopyButton value={business.phone} label="Copy phone number" />
                  </div>
                )}

                {business.email && (
                  <div className="flex items-start justify-between gap-3">
                    <a
                      href={`mailto:${business.email}`}
                      className="flex items-start gap-2.5 text-sm text-korea-blue hover:underline break-all min-w-0"
                    >
                      <Mail size={16} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                      {business.email}
                    </a>
                    <CopyButton value={business.email} label="Copy email address" />
                  </div>
                )}
              </div>
            </div>
          </article>

          {/* Contact rail, desktop only */}
          <aside className="hidden lg:flex lg:flex-col w-72 shrink-0 gap-3">
            {(fullAddress || business.phone || business.email) && (
              <div className="rounded-xl border border-border-dark p-4 flex flex-col gap-3">
                <h2 className="text-sm font-semibold text-snow">Contact</h2>

                {fullAddress && (
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-start gap-2.5 text-[13px] text-snow min-w-0">
                      <MapPin size={15} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                      {fullAddress}
                    </div>
                    <CopyButton value={fullAddress} label="Copy address" />
                  </div>
                )}

                {business.phone && (
                  <div className="flex items-start justify-between gap-2">
                    <a
                      href={`tel:${business.phone}`}
                      className="flex items-start gap-2.5 text-[13px] text-snow hover:text-white min-w-0"
                    >
                      <Phone size={15} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                      {business.phone}
                    </a>
                    <CopyButton value={business.phone} label="Copy phone number" />
                  </div>
                )}

                {business.email && (
                  <div className="flex items-start justify-between gap-2">
                    <a
                      href={`mailto:${business.email}`}
                      className="flex items-start gap-2.5 text-[13px] text-korea-blue hover:underline break-all min-w-0"
                    >
                      <Mail size={15} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                      {business.email}
                    </a>
                    <CopyButton value={business.email} label="Copy email address" />
                  </div>
                )}
              </div>
            )}

            <button
              onClick={toggleSave}
              disabled={saving}
              className={`flex items-center justify-center gap-2 text-sm px-4 py-2.5 rounded-xl font-medium transition-colors
                          disabled:opacity-60 disabled:cursor-not-allowed ${
                saved
                  ? 'bg-night text-muted border border-border-dark hover:text-snow hover:border-faint'
                  : 'bg-korea-blue text-white hover:bg-korea-blue/85'
              }`}
            >
              <Heart size={15} strokeWidth={2} fill={saved ? 'currentColor' : 'none'} />
              {saved ? 'Saved' : 'Save to favourites'}
            </button>
            {saveError && <p className="text-adelaide-red text-xs">{saveError}</p>}
          </aside>

        </div>
      </PageShell>
    </Layout>
  );
}

export default BusinessDetailPage;