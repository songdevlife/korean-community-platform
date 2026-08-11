'use client';

import { useParams } from 'next/navigation';
import { renderGuideCard } from '@/api/admin';
import PageShell from '@/components/PageShell';
import AdminGuard from '@/components/admin/AdminGuard';
import AdminBackButton from '@/components/admin/AdminBackButton';
import CardPreview from '@/components/admin/CardPreview';

export default function GuideCardPage() {
  const { guideId } = useParams();

  return (
    <AdminGuard>
      <PageShell>
        <AdminBackButton href="/admin/guides" label="Guides" />

        <h1 className="text-xl font-bold text-snow mb-1">Social card</h1>

        <CardPreview
          contentId={guideId}
          render={renderGuideCard}
          filePrefix="dak-guide"
        />
      </PageShell>
    </AdminGuard>
  );
}