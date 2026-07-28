'use client';

import Link from 'next/link';
import { Plus } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

/**
 * Submission entry point. Shown to any signed-in user rather than admins only —
 * listings land in PENDING and are reviewed before they appear, so the queue is
 * the control rather than a role.
 */
export default function AdminNewBusinessLink() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <Link
      href="/businesses/new"
      className="flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg border
                 border-border-dark text-muted hover:text-snow hover:border-faint
                 transition-colors shrink-0"
    >
      <Plus size={13} strokeWidth={2} />
      List your business
    </Link>
  );
}