'use client';

import { useState } from 'react';
import { ImageOff, ChevronLeft, ChevronRight } from 'lucide-react';
import { sized } from '@/utils/image';

/**
 * Image carousel with a thumbnail rail, shared by businesses and events.
 *
 * Client-side because the controls change which image is shown — everything
 * else on the page around it stays server-rendered. The first image is in the
 * initial HTML, so the main photo is present for a crawler and a link preview
 * even though navigation needs JavaScript.
 *
 * The rail appears only above 1024px. Below that it would be a row of small
 * squares needing a precise tap, so the arrows carry the whole job — which is
 * what they were built for before the rail existed.
 *
 * object-contain rather than cover: a poster carries text, and cropping it
 * removes the date. The letterboxing this leaves sits against the same dark
 * background as the page.
 */
export default function Gallery({ images = [], alt = '' }) {
  const [index, setIndex] = useState(0);

  const count = images.length;
  const hasMultiple = count > 1;

  // Wrap around at both ends so the arrows never dead-end.
  const showPrev = () => setIndex((i) => (i - 1 + count) % count);
  const showNext = () => setIndex((i) => (i + 1) % count);

  const arrowClass =
    'absolute top-1/2 -translate-y-1/2 p-2 rounded-full bg-night/70 text-snow ' +
    'hover:bg-night transition-colors backdrop-blur-sm';

  // Aspect ratio rather than a fixed height. A fixed height sized for a
  // portrait poster leaves a landscape photograph floating in the middle of
  // it, which is what made the image look small rather than the column being
  // narrow. Tying height to width instead means a landscape image nearly
  // fills the frame, and the frame does not jump when the carousel moves
  // between images of different shapes.
  const frameClass =
    'relative w-full aspect-[4/3] rounded-xl border border-border-dark bg-night ' +
    'flex items-center justify-center overflow-hidden';

  if (count === 0) {
    return (
      <div className={`${frameClass} mb-5`}>
        <ImageOff size={28} strokeWidth={1.5} className="text-border-dark" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2 mb-5">
      <div className={frameClass}>
        {/* Every image rendered and stacked, with only the current one opaque.
            Two things follow from this that a single swapped <img> does not
            give: the crossfade has something to fade between, and the browser
            has already fetched the next image by the time an arrow is pressed,
            so there is no blank frame while it loads.

            aria-hidden on the inactive ones so a screen reader announces one
            image rather than all of them. */}
        {images.map((image, i) => (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            key={image.id ?? i}
            src={sized(image.imageUrl, 1000)}
            alt={i === index ? (image.altText || alt) : ''}
            aria-hidden={i !== index}
            className={`absolute inset-0 w-full h-full object-contain
                        transition-opacity duration-300 ease-out
                        motion-reduce:transition-none ${
                          i === index ? 'opacity-100' : 'opacity-0'
                        }`}
          />
        ))}

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
              {index + 1} / {count}
            </span>
          </>
        )}
      </div>

      {/* Below the image rather than beside it: a column alongside takes width
          from the thing it is previewing, which in a narrow event column left
          the main image smaller than the page had room for. Absent entirely
          with one image — a strip of one thumbnail says there is more to see
          when there is not. Scrolls sideways past about eight, which is the
          point at which a fixed row would shrink them to unreadable. */}
      {hasMultiple && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {images.map((image, i) => (
            <button
              key={image.id ?? i}
              onClick={() => setIndex(i)}
              aria-label={`Image ${i + 1}`}
              aria-current={i === index}
              className={`w-16 h-16 shrink-0 rounded-lg overflow-hidden border transition-colors ${
                i === index
                  ? 'border-korea-blue'
                  : 'border-border-dark hover:border-faint'
              }`}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={sized(image.imageUrl, 120)}
                alt=""
                aria-hidden="true"
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}