const MINUTE = 60;
const HOUR = MINUTE * 60;
const DAY = HOUR * 24;
const WEEK = DAY * 7;

// Anything created within this window is surfaced as new. Three days is short
// enough that the badge stays meaningful when several items arrive at once,
// and long enough that a weekend visitor still sees midweek additions.
const NEW_WINDOW_DAYS = 3;

/**
 * Short relative time label, e.g. "2 days ago".
 * Kept deliberately simple; swap for date-fns if formatting needs grow
 * beyond a handful of call sites.
 */
export function timeAgo(isoString) {
  if (!isoString) return '';

  const then = new Date(isoString);
  if (Number.isNaN(then.getTime())) return '';

  const seconds = Math.floor((Date.now() - then.getTime()) / 1000);

  // Guard against clock skew producing "in -3 seconds"
  if (seconds < MINUTE) return 'Just now';
  if (seconds < HOUR) {
    const m = Math.floor(seconds / MINUTE);
    return `${m} minute${m === 1 ? '' : 's'} ago`;
  }
  if (seconds < DAY) {
    const h = Math.floor(seconds / HOUR);
    return `${h} hour${h === 1 ? '' : 's'} ago`;
  }
  if (seconds < WEEK) {
    const d = Math.floor(seconds / DAY);
    return `${d} day${d === 1 ? '' : 's'} ago`;
  }
  if (seconds < DAY * 30) {
    const w = Math.floor(seconds / WEEK);
    return `${w} week${w === 1 ? '' : 's'} ago`;
  }

  // Older than a month: show the actual date instead of a vague label
  return then.toLocaleDateString('en-AU', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

/**
 * Whether an item is recent enough to carry a "new" badge.
 * Returns false for missing or unparseable dates rather than throwing, so a
 * bad value simply means no badge.
 */
export function isNew(isoString) {
  if (!isoString) return false;

  const then = new Date(isoString);
  if (Number.isNaN(then.getTime())) return false;

  const seconds = Math.floor((Date.now() - then.getTime()) / 1000);
  return seconds >= 0 && seconds < DAY * NEW_WINDOW_DAYS;
}