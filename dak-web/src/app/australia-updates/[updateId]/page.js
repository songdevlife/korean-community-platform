import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Sparkles, ExternalLink, Calendar } from 'lucide-react';
import { getUpdateById } from '@/api/server';
import PageShell from '@/components/PageShell';
import SaveButton from '@/components/SaveButton';
import { timeAgo } from '@/utils/date';
import UpdateAdminBar from '@/components/UpdateAdminBar';

/**
 * Australia Update detail.
 *
 * What DAK publishes here is its own Korean-language account of the facts, with
 * a link to the original — never the source article's text. The separation is
 * enforced on the backend (12 Entry 22): extracted source text lives on an
 * admin-only DTO and has no field on the public response, so it cannot reach
 * this page even by mistake.
 */
export async function generateMetadata({ params }) {
  const { updateId } = await params;
  const update = await getUpdateById(updateId);

  if (!update) {
    return { title: 'Update not found' };
  }

  // Collapsed to one line and cut at a word boundary: a preview card renders
  // paragraph breaks as gaps, and a truncated word reads as a fault.
  const description = update.koreanSummary
    ?.replace(/\s+/g, ' ')
    .slice(0, 160)
    .replace(/\S*$/, '')
    .trim();

  return {
    title: update.title,
    description,
    alternates: { canonical: `/australia-updates/${update.id}` },
    openGraph: {
      title: update.title,
      description,
      type: 'article',
      publishedTime: update.createdAt,
    },
  };
}

export default async function AustraliaUpdatePage({ params }) {
  const { updateId } = await params;
  const update = await getUpdateById(updateId);

  // Covers both an unknown id and one that is DRAFT or ARCHIVED — the public
  // endpoint returns 404 for all three.
  if (!update) notFound();

  const sources = update.sources ?? [];

  // NewsArticle rather than Article: an update reports something that happened
  // on a date, where a guide describes a process that holds until the rules
  // change. Citing the sources makes the relationship 03 MVP 12 requires —
  // that a reader retains access to the original — explicit in the markup as
  // well as in the interface.
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'NewsArticle',
    headline: update.title,
    inLanguage: 'ko',
    datePublished: update.createdAt,
    ...(update.koreanSummary && {
      description: update.koreanSummary.replace(/\s+/g, ' ').slice(0, 200),
    }),
    ...(update.category && { articleSection: update.category.name }),
    ...(sources.length > 0 && { citation: sources.map((s) => s.sourceUrl) }),
    publisher: { '@type': 'Organization', name: 'DAK — Discover Adelaide Korea' },
  };

  return (
    <PageShell>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
      />

      <div className="flex items-center justify-between mb-5">
        <Link
          href="/australia-updates"
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">Back</span>
        </Link>

        {/* Hidden above 1024px, where the rail carries its own control. */}
        <div className="lg:hidden">
          <SaveButton
            resourceType="AUSTRALIA_UPDATE"
            resourceId={update.id}
            variant="icon"
          />
        </div>
      </div>

      {/* Two columns on desktop: article plus metadata rail */}
      <div className="flex flex-col lg:flex-row gap-6">

      <article className="flex-1 min-w-0">
          <UpdateAdminBar update={update}>
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

            <h1 className="text-2xl font-bold text-snow leading-tight mb-4">
              {update.title}
            </h1>

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
          </UpdateAdminBar>

          {/* Sources stay outside the admin wrapper — they are not editable here
              and should remain visible while the form is open. */}
{sources.length > 0 && (
            <div className="lg:hidden mt-8 pt-5 border-t border-border-dark">
              <h2 className="text-sm font-semibold text-snow mb-3">Sources</h2>
              <ul className="flex flex-col gap-2.5 mb-4">
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

              {/* Age matters more here than on a guide: these cover prices,
                  alerts and rule changes, where a reader needs to know whether
                  they are reading something from this week or last quarter. */}
              <div className="flex items-center gap-2 text-[13px] text-muted">
                <Calendar size={14} strokeWidth={1.75} className="shrink-0" />
                Published {timeAgo(update.createdAt)}
              </div>
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

          <SaveButton resourceType="AUSTRALIA_UPDATE" resourceId={update.id} />
        </aside>

      </div>
    </PageShell>
  );
}