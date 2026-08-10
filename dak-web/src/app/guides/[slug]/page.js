import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Calendar, Sparkles } from 'lucide-react';
import { getGuideBySlug } from '@/api/server';
import PageShell from '@/components/PageShell';
import Markdown from '@/components/Markdown';
import SaveButton from '@/components/SaveButton';
import GuideAdminBar from '@/components/GuideAdminBar';
import { timeAgo } from '@/utils/date';
import { DEFAULT_OG_IMAGE } from '@/utils/og';

/**
 * Guide detail. The page this rebuild exists for: a stranger arriving from a
 * search result must receive the article in the HTML, not an empty div that
 * fills in later.
 *
 * The page itself is a server component and async — data is awaited before
 * anything renders, so the response body already contains the text. SaveButton
 * and GuideAdminBar are the only client components, and the latter takes the
 * article as children so a non-admin sees exactly what the server rendered.
 */

// Next resolves this before rendering and puts the result in <head>. It runs on
// the server, so the values reach a crawler and a link-preview scraper — which
// the previous react-helmet-async approach could not do.
export async function generateMetadata({ params }) {
  const { slug } = await params;
  const guide = await getGuideBySlug(slug);

  if (!guide) {
    return { title: 'Guide not found' };
  }

  return {
    title: guide.title,
    description: guide.summary,
    alternates: { canonical: `/guides/${guide.slug}` },
    openGraph: {
      title: guide.title,
      description: guide.summary,
      type: 'article',
      publishedTime: guide.publishedAt,
      modifiedTime: guide.updatedAt,
      // Named again because the parent's openGraph is replaced wholesale.
      // Without it KakaoTalk scrapes the header logo img from the page body.
      images: [DEFAULT_OG_IMAGE],
      siteName: 'Discover Adelaide Korea',
      locale: 'ko_KR',
    },
  };
}

export default async function GuidePage({ params }) {
  const { slug } = await params;
  const guide = await getGuideBySlug(slug);

  // A DRAFT or ARCHIVED guide returns 404 from the public endpoint, so this
  // covers both an unknown slug and an unpublished one.
  if (!guide) notFound();

  // Article rather than NewsArticle: a guide describes a process that holds
  // until the rules change, where an update reports something that happened on
  // a date. dateModified carries real weight here — these cover regulations
  // that shift, and a revision date is how a crawler tells a page that was
  // re-checked from one merely left up.
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: guide.title,
    inLanguage: 'ko',
    ...(guide.summary && { description: guide.summary }),
    ...(guide.publishedAt && { datePublished: guide.publishedAt }),
    ...(guide.updatedAt &&
      guide.updatedAt !== guide.publishedAt && { dateModified: guide.updatedAt }),
    ...(guide.category && { articleSection: guide.category.name }),
    author: { '@type': 'Organization', name: 'DAK — Discover Adelaide Korea' },
    publisher: { '@type': 'Organization', name: 'DAK — Discover Adelaide Korea' },
  };

  return (
    <PageShell>
      {/* JSON-LD in the server-rendered body, so it is present without JS. */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
      />

      <div className="flex items-center justify-between mb-5">
        <Link
          href="/guides"
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">back</span>
        </Link>

        {/* Hidden above 1024px, where the rail carries its own control. */}
        <div className="lg:hidden">
          <SaveButton resourceType="GUIDE" resourceId={guide.id} variant="icon" />
        </div>
      </div>

      {/* Two columns on desktop: article plus metadata rail */}
      <div className="flex flex-col lg:flex-row gap-6">

        <article className="flex-1 min-w-0">
          <GuideAdminBar guide={guide}>
            {guide.category && (
              <div className="flex flex-wrap items-center gap-2 mb-3">
                <span className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-snow">
                  {guide.category.name}
                </span>
              </div>
            )}

            <h1 className="text-2xl font-bold text-snow leading-tight mb-4">
              {guide.title}
            </h1>

            {guide.summary && (
              <div className="flex items-start gap-2 rounded-xl border border-border-dark bg-night px-4 py-3 mb-6">
                <Sparkles size={16} strokeWidth={2} className="text-muted shrink-0 mt-0.5" />
                <div className="min-w-0">
                  <p className="text-[13px] text-faint mb-1">
                    요약입니다. 바쁘신 분들은 요점만 보고 가세요.
                  </p>
                  <p className="text-[14px] text-muted leading-relaxed">{guide.summary}</p>
                </div>
              </div>
            )}

            <Markdown>{guide.body}</Markdown>
          </GuideAdminBar>
        </article>

        {/* Metadata rail, desktop only */}
        <aside className="hidden lg:flex lg:flex-col w-72 shrink-0 gap-3">
          <div className="rounded-xl border border-border-dark p-4">
            <div className="flex items-center gap-2 text-[13px] text-muted">
              <Calendar size={14} strokeWidth={1.75} className="shrink-0" />
              {guide.publishedAt ? `Published ${timeAgo(guide.publishedAt)}` : 'Not published'}
            </div>
          </div>

          <SaveButton resourceType="GUIDE" resourceId={guide.id} />
        </aside>

      </div>
    </PageShell>
  );
}