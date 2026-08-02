/**
 * The host of a URL, without www, for showing where a link goes.
 *
 * A bare "원문 보기" says nothing about the destination; the domain is the
 * cheapest way to say it, and unlike a stored source name it needs nothing
 * typed at entry time.
 */
export function displayHost(url) {
    if (!url) return null;
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      // Not a URL at all. The caller decides whether to render it as plain text.
      return null;
    }
  }
  
  /** Whether a value can be turned into a link rather than shown as text. */
  export function isUrl(value) {
    if (!value) return false;
    try {
      const { protocol } = new URL(value);
      return protocol === 'http:' || protocol === 'https:';
    } catch {
      return false;
    }
  }