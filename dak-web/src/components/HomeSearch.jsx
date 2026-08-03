'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Search } from 'lucide-react';

/**
 * The hero search box. Client-side because it holds input state and navigates
 * on submit — but it is the only interactive part of the home page, so the
 * listings around it stay server-rendered.
 *
 * Submits to a URL rather than filtering in place: a search is a page someone
 * can bookmark, share, or reach with the back button.
 */
export default function HomeSearch() {
  const router = useRouter();
  const [keyword, setKeyword] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    const trimmed = keyword.trim();
    if (trimmed) {
      router.push(`/search?keyword=${encodeURIComponent(trimmed)}`);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="relative">
        <Search
          size={18}
          strokeWidth={1.75}
          className="absolute left-4 top-1/2 -translate-y-1/2 text-faint pointer-events-none"
        />
        {/* 16px font on mobile stops iOS Safari zooming on focus. */}
        <input
          type="text"
          placeholder="Search events, guides, updates"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          aria-label="Search"
          className="w-full pl-11 pr-4 py-3.5 rounded-xl border border-border-dark bg-night
                     text-[16px] md:text-[15px] text-snow
                     placeholder:text-faint placeholder:font-semibold
                     outline-none focus:border-faint transition-colors [color-scheme:dark]"
        />
      </div>
    </form>
  );
}