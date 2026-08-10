'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';

/**
 * Back control for the admin screens.
 *
 * Defaults to browser history. Pass href where a fixed destination is better —
 * a section page returning to the dashboard should not depend on how the admin
 * arrived there.
 */
export default function AdminBackButton({ href, label = 'Back' }) {
  const router = useRouter();

  const className =
    'flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors mb-5';

  if (href) {
    return (
      <Link href={href} aria-label={label} className={className}>
        <ArrowLeft size={18} strokeWidth={1.75} />
        <span className="hidden sm:inline">{label}</span>
      </Link>
    );
  }

  return (
    <button onClick={() => router.back()} aria-label={label} className={className}>
      <ArrowLeft size={18} strokeWidth={1.75} />
      <span className="hidden sm:inline">{label}</span>
    </button>
  );
}