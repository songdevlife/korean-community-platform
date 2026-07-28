'use client';

import Link from 'next/link';
import { Plus } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

/**
 * Admin-only entry to guide creation. Isolated into its own client component so
 * the page around it stays a server component — checking a role requires the
 * session, which only exists in the browser.
 */
export default function AdminNewGuideLink() {
  const { user } = useAuth();

  if (user?.role !== 'ADMINISTRATOR') return null;

  return (
    <Link
      href="/admin/guides/new"
      className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border
                 border-border-dark text-muted hover:text-snow hover:border-faint
                 transition-colors shrink-0"
    >
      <Plus size={13} strokeWidth={2} />
      글쓰기
    </Link>
  );
}