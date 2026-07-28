'use client';

import { useEffect, useRef } from 'react';

/**
 * Records the search that produced the page around it, then renders nothing.
 *
 * A client component rather than a call inside the server page: posting during
 * a server render is a side effect during render, which React is free to run
 * more than once. Here it runs after paint, exactly once per distinct result.
 *
 * Uses plain fetch rather than the axios client so no Authorization header is
 * attached. That is the point — the log is anonymous by construction, not by
 * the backend choosing to discard the identity it was sent.
 */
export default function SearchLogger({ keyword, resultCount }) {
  const lastLogged = useRef(null);

  useEffect(() => {
    if (!keyword) return;

    // Guards against React's development double-invoke and against a re-render
    // that did not change the search.
    const signature = `${keyword}|${resultCount}`;
    if (lastLogged.current === signature) return;
    lastLogged.current = signature;

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/search-logs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ searchTerm: keyword, resultCount }),
      // Lets the request survive the reader navigating away immediately, which
      // is common on a zero-result page — the case most worth recording.
      keepalive: true,
    }).catch(() => {
      // Logging must never affect the page. A failure here is not the reader's
      // problem and there is nothing useful to tell them.
    });
  }, [keyword, resultCount]);

  return null;
}