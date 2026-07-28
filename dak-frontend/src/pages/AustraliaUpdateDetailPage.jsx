import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Share2, Sparkles, ExternalLink, Calendar, Heart,
  Pencil, Check, Undo2,
} from 'lucide-react';
import { fetchUpdateById } from '../api/updates';
import { updateUpdateMetadata, updateUpdateStatus } from '../api/admin';
import { saveItem, checkIsSaved, removeSavedByResource } from '../api/savedItems';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import { timeAgo } from '../utils/date';
import PageMeta from '../components/PageMeta';
import StructuredData from '../components/StructuredData';
import { updateSchema } from '../utils/schema';

function AustraliaUpdateDetailPage() {
  const { updateId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [update, setUpdate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saving, setSaving] = useState(false);

  // Admin editing state. Kept separate from `update` so cancelling discards
  // changes without a refetch.
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState({ title: '', koreanSummary: '' });
  const [adminError, setAdminError] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user?.role === 'ADMINISTRATOR';

  useEffect(() => {
    async function loadUpdate() {
      try {
        const data = await fetchUpdateById(updateId);
        setUpdate(data);
        if (user) {
          const alreadySaved = await checkIsSaved('AUSTRALIA_UPDATE', data.id);
          setSaved(alreadySaved);
        }
      } catch (error) {
        if (error.response?.status === 404) setNotFound(true);
        console.error('Failed to load update:', error);
      } finally {
        setLoading(false);
      }
    }
    loadUpdate();
  }, [updateId, user]);

  /**
   * Toggles rather than only saving. A control that fills in and then refuses to
   * respond reads as broken, and an accidental save would otherwise be
   * undoable only from the favourites list.
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
        await removeSavedByResource('AUSTRALIA_UPDATE', update.id);
        setSaved(false);
      } else {
        await saveItem('AUSTRALIA_UPDATE', update.id);
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

  async function handleShare() {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title: update.title, url });
      } catch (err) {
        // User cancelled the share sheet — nothing to do
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
      } catch (err) {
        // Clipboard blocked — silently ignore
      }
    }
  }

  function startEditing() {
    setDraft({ title: update.title ?? '', koreanSummary: update.koreanSummary ?? '' });
    setEditing(true);
    setAdminError('');
  }

  async function saveEdits() {
    setAdminError('');
    setBusy(true);
    try {
      await updateUpdateMetadata(update.id, {
        title: draft.title,
        koreanSummary: draft.koreanSummary,
      });
      setUpdate((u) => ({ ...u, title: draft.title, koreanSummary: draft.koreanSummary }));
      setEditing(false);
    } catch (err) {
      setAdminError(err.response?.data?.error?.message || 'Could not save those changes.');
    } finally {
      setBusy(false);
    }
  }

  /**
   * Returns the update to the review queue rather than archiving it — an admin
   * unpublishing from the article itself is usually correcting something, not
   * retiring it. Archiving lives in the admin queue.
   */
  async function unpublish() {
    setAdminError('');
    setBusy(true);
    try {
      await updateUpdateStatus(update.id, 'DRAFT');
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
          <div className="h-8 w-2/3 rounded bg-night animate-pulse mb-4" />
          <div className="h-40 rounded-xl bg-night animate-pulse" />
        </PageShell>
      </Layout>
    );
  }

  if (notFound || !update) {
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
          <p className="text-muted text-sm">This update could not be found.</p>
        </PageShell>
      </Layout>
    );
  }

  const sources = update.sources ?? [];

  const secondaryBtn =
    'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border ' +
    'border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
    'disabled:opacity-40 disabled:cursor-not-allowed';

  const fieldClass =
    'w-full px-3 py-2 rounded-lg bg-night border border-border-dark text-snow ' +
    'text-[15px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

  return (
    <Layout>
      <PageShell>
        <PageMeta
          title={update.title}
          path={`/australia-updates/${update.id}`}
          description={update.koreanSummary?.slice(0, 200)}
        />
        <StructuredData data={updateSchema(update)} />

        {/* Top bar: back on the left, save and share on the right. The save
            control is hidden above 1024px, where the metadata rail carries its
            own — matching the other two detail pages. */}
        <div className="flex items-center justify-between mb-5">
          <button
            onClick={() => navigate(-1)}
            aria-label="Back"
            className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
          >
            <ArrowLeft size={18} strokeWidth={1.75} />
            <span className="hidden sm:inline">Back</span>
          </button>

          <div className="flex items-center gap-1">
            <button
              onClick={toggleSave}
              disabled={saving}
              aria-label={saved ? 'Remove from favourites' : 'Save to favourites'}
              aria-pressed={saved}
              className={`lg:hidden p-2 rounded-lg transition-colors
                          disabled:opacity-60 disabled:cursor-not-allowed ${
                saved ? 'text-adelaide-red' : 'text-muted hover:text-snow hover:bg-night'
              }`}
            >
              <Heart size={20} strokeWidth={1.75} fill={saved ? 'currentColor' : 'none'} />
            </button>

            <button
              onClick={handleShare}
              aria-label="Share"
              className="p-2 -mr-2 rounded-lg text-muted hover:text-snow hover:bg-night transition-colors"
            >
              <Share2 size={18} strokeWidth={1.75} />
            </button>
          </div>
        </div>

        {/* Save errors surface here rather than only in the rail, which is
            hidden on mobile — the log-in prompt for a guest tapping the heart
            would otherwise never be seen. */}
        {saveError && <p className="text-adelaide-red text-[13px] mb-4 lg:hidden">{saveError}</p>}

        {/* Admin controls. Shown on the article itself so a problem spotted
            while reading can be fixed without hunting for it in the queue. */}
        {isAdmin && !editing && (
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

        {adminError && <p className="text-adelaide-red text-[13px] mb-4">{adminError}</p>}

        {/* Two columns on desktop: article plus metadata rail */}
        <div className="flex flex-col lg:flex-row gap-6">

          <article className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-3">
              {update.category && (
                <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
                  {update.category.name}
                </span>
              )}
              {update.geographicScope && (
                <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-muted">
                  {update.geographicScope}
                </span>
              )}
            </div>

            {editing ? (
              <div className="flex flex-col gap-3 mb-6">
                <div>
                  <label htmlFor="edit-title" className="block text-[12px] font-medium text-muted mb-1.5">
                    Title
                  </label>
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
                  <label htmlFor="edit-summary" className="block text-[12px] font-medium text-muted mb-1.5">
                    Korean summary
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
              <>
                <h1 className="text-2xl font-bold text-snow leading-tight mb-4">
                  {update.title}
                </h1>

                {/* AI disclosure banner, required wherever AI-generated content
                    appears (03 MVP AI constraints, 06 UI/UX 6.3) */}
                {update.aiGenerated && (
                  <div className="flex items-center gap-2 rounded-xl border border-border-dark bg-night px-4 py-3 mb-5">
                    <Sparkles size={16} strokeWidth={2} className="text-muted shrink-0" />
                    <span className="text-[13px] text-muted">
                      AI-generated summary from the source below, reviewed and approved by an admin.
                    </span>
                  </div>
                )}

                <p className="text-[15px] leading-relaxed text-snow whitespace-pre-line">
                  {update.koreanSummary}
                </p>
              </>
            )}

            {/* Sources render inline below the article on mobile only; the
                desktop rail shows them instead */}
            {sources.length > 0 && (
              <div className="lg:hidden mt-8 pt-5 border-t border-border-dark">
                <h2 className="text-sm font-semibold text-snow mb-3">Sources</h2>
                <ul className="flex flex-col gap-2">
                  {sources.map((source) => (
                    <li key={source.id}>
                      <a
                        href={source.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-start gap-2 text-sm text-korea-blue hover:underline"
                      >
                        <ExternalLink size={15} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                        {source.sourceTitle || source.sourceName}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </article>

          {/* Metadata rail, desktop only */}
          <aside className="hidden lg:flex lg:flex-col w-72 shrink-0 gap-3">
            {sources.length > 0 && (
              <div className="rounded-xl border border-border-dark p-4">
                <h2 className="text-sm font-semibold text-snow mb-3">Sources</h2>
                <ul className="flex flex-col gap-2.5">
                  {sources.map((source) => (
                    <li key={source.id}>
                      <a
                        href={source.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-start gap-2 text-[13px] text-korea-blue hover:underline"
                      >
                        <ExternalLink size={14} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                        {source.sourceTitle || source.sourceName}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="rounded-xl border border-border-dark p-4">
              <div className="flex items-center gap-2 text-[13px] text-muted">
                <Calendar size={14} strokeWidth={1.75} className="shrink-0" />
                Published {timeAgo(update.createdAt)}
              </div>
            </div>

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

export default AustraliaUpdateDetailPage;