import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import PageShell from '@/components/PageShell';
import Markdown from '@/components/Markdown';
import { LEGAL_DOCUMENTS, findLegalDocument, readLegalDocument } from '@/utils/legal';

// Nine files that change a couple of times a year. Rendering them at build time
// means no filesystem read per request and no dependence on the backend being
// up — a privacy policy should still be readable when the API is not.
export function generateStaticParams() {
  return LEGAL_DOCUMENTS.map(({ slug }) => ({ slug }));
}

export async function generateMetadata({ params }) {
  const { slug } = await params;
  const doc = findLegalDocument(slug);

  if (!doc) return { title: 'Not found' };

  return {
    title: doc.title,
    description: doc.blurb,
    robots: { index: false, follow: false },
  };
}

export default async function LegalDocumentPage({ params }) {
  const { slug } = await params;
  const doc = findLegalDocument(slug);
  const body = readLegalDocument(slug);

  // Covers an unknown slug and a listed document whose file is missing. The
  // second would otherwise render an empty page that looks deliberate.
  if (!doc || !body) notFound();

  return (
    <PageShell>
      <Link
        href="/legal"
        className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5"
      >
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span className="hidden sm:inline">Legal</span>
      </Link>

      {/* The markdown carries its own h1, so none is added here. */}
      <article className="max-w-[720px]">
        <Markdown>{body}</Markdown>
      </article>
    </PageShell>
  );
}