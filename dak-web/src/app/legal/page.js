import Link from 'next/link';
import { FileText } from 'lucide-react';
import PageShell from '@/components/PageShell';
import { LEGAL_DOCUMENTS } from '@/utils/legal';

export const metadata = {
  title: 'Legal',
};

export default function LegalIndexPage() {
  return (
    <PageShell>
      <h1 className="text-xl font-bold text-snow mb-2">Legal</h1>
      <p className="text-sm text-muted mb-6">
        The policies that govern how DAK operates and how your information is handled.
      </p>

      <div className="grid gap-2.5">
        {LEGAL_DOCUMENTS.map((doc) => (
          <Link
            key={doc.slug}
            href={`/legal/${doc.slug}`}
            className="rounded-xl border border-border-dark bg-night p-4
                       flex items-start gap-3 hover:border-faint transition-colors"
          >
            <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                             flex items-center justify-center">
              <FileText size={16} strokeWidth={1.75} className="text-muted" />
            </span>
            <span className="min-w-0">
              <span className="block text-sm font-medium text-snow">{doc.title}</span>
              <span className="block text-[13px] text-muted mt-0.5">{doc.blurb}</span>
            </span>
          </Link>
        ))}
      </div>
    </PageShell>
  );
}