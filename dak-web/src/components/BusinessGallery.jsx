'use client';

import { useState } from 'react';
import { ImageOff, ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * Image carousel. Client-side because the arrows change which image is shown —
 * everything else on the listing page stays server-rendered around it.
 *
 * The first image is in the initial HTML, so a listing's main photo is present
 * for a crawler and a link preview even though the navigation needs JavaScript.
 */
export default function BusinessGallery({ images = [] }) {
  const [index, setIndex] = useState(0);
  const hasMultiple = images.length > 1;

  // Wrap around at both ends so the arrows never dead-end.
  const showPrev = () => setIndex((i) => (i - 1 + images.length) % images.length);
  const showNext = () => setIndex((i) => (i + 1) % images.length);

  const arrowClass =
    'absolute top-1/2 -translate-y-1/2 p-2 rounded-full bg-night/70 text-snow ' +
    'hover:bg-night transition-colors backdrop-blur-sm';

  return (
    <div className="relative w-full rounded-xl border border-border-dark bg-night
                    h-52 sm:h-64 lg:h-96
                    flex items-center justify-center overflow-hidden mb-5">
      {images.length > 0 ? (
        <>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={images[index].imageUrl}
            alt={images[index].altText || ''}
            className="w-full h-full object-cover"
          />

          {hasMultiple && (
            <>
              <button onClick={showPrev} aria-label="Previous image" className={`${arrowClass} left-3`}>
                <ChevronLeft size={18} strokeWidth={2} />
              </button>
              <button onClick={showNext} aria-label="Next image" className={`${arrowClass} right-3`}>
                <ChevronRight size={18} strokeWidth={2} />
              </button>

              <span className="absolute bottom-3 right-3 px-2 py-0.5 rounded-md
                               bg-night/70 backdrop-blur-sm text-[12px] text-snow">
                {index + 1} / {images.length}
              </span>
            </>
          )}
        </>
      ) : (
        <ImageOff size={28} strokeWidth={1.5} className="text-border-dark" />
      )}
    </div>
  );
}