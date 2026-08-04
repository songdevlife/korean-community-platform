/**
 * A Cloudinary URL with transformation parameters inserted.
 *
 * The same uploaded file serves the card and the detail page at different
 * sizes, because Cloudinary applies the transformation on request rather than
 * requiring separate uploads. Posters arrive at whatever size the organiser
 * made them — the one for the first listed event is 2048 wide — and sending
 * that to a card three hundred pixels across is most of a megabyte wasted on
 * pixels nobody sees.
 *
 * Anything that is not a Cloudinary URL is returned unchanged, so a link to
 * an image hosted elsewhere still works.
 */
export function sized(url, width) {
    if (!url) return null;
    if (!url.includes('res.cloudinary.com') || !url.includes('/upload/')) return url;
  
    // q_auto lets Cloudinary choose a quality that looks the same and weighs
    // less; f_auto serves WebP or AVIF where the browser accepts it.
    return url.replace('/upload/', `/upload/w_${width},q_auto,f_auto/`);
  }