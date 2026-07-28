'use client';

import { useState, useEffect } from 'react';
import { Copy, Check } from 'lucide-react';

/**
 * Copies a value to the clipboard and briefly confirms it.
 *
 * Sits beside contact details rather than replacing them: tapping a phone
 * number on mobile is expected to start a call, so copy is a separate action.
 */
function CopyButton({ value, label = 'Copy' }) {
  const [copied, setCopied] = useState(false);

  // Reset the confirmation after a moment, cancelling on unmount so the
  // timer can't fire against a removed component.
  useEffect(() => {
    if (!copied) return;
    const timer = setTimeout(() => setCopied(false), 2500);
    return () => clearTimeout(timer);
  }, [copied]);

  async function handleCopy(e) {
    // The button often sits inside a link; without this the click would
    // also trigger the parent's navigation.
    e.preventDefault();
    e.stopPropagation();

    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
    } catch (err) {
      // Clipboard access can be blocked (insecure origin, permissions).
      console.error('Copy failed:', err);
    }
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      aria-label={copied ? 'Copied' : label}
      className="relative w-6 h-6 -m-1 rounded-md text-muted hover:text-snow
                 transition-colors shrink-0"
    >
      {/* Both icons are stacked and cross-faded, so neither jumps position
          as the state flips. */}
      <span
        className={`absolute inset-0 flex items-center justify-center
                    transition-all duration-500 ${
          copied ? 'opacity-0 scale-50' : 'opacity-100 scale-100'
        }`}
      >
        <Copy size={14} strokeWidth={1.75} />
      </span>

      <span
        className={`absolute inset-0 flex items-center justify-center text-soft-green
                    transition-all duration-500 ${
          copied ? 'opacity-100 scale-100' : 'opacity-0 scale-50'
        }`}
      >
        <Check size={14} strokeWidth={2.5} />
      </span>
    </button>
  );
}

export default CopyButton;