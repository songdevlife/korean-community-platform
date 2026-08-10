/**
 * Shared control styles for the admin screens.
 *
 * These were duplicated in every admin page while they lived as local
 * constants, which is how two queues end up with buttons that do not match.
 */

export const primaryBtn =
  'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg font-medium ' +
  'bg-korea-blue text-white hover:bg-korea-blue/85 transition-colors ' +
  'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-korea-blue';

export const secondaryBtn =
  'flex items-center gap-1.5 text-[13px] px-3 py-1.5 rounded-lg ' +
  'border border-border-dark text-muted hover:text-snow hover:border-faint transition-colors ' +
  'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:text-muted ' +
  'disabled:hover:border-border-dark';

export const selectClass =
  'text-[13px] px-2.5 py-1.5 rounded-lg bg-surface border border-border-dark ' +
  'text-snow outline-none focus:border-faint transition-colors [color-scheme:dark]';

export const fieldClass =
  'w-full px-3 py-2 rounded-lg bg-surface border border-border-dark text-snow ' +
  'text-[14px] outline-none focus:border-faint transition-colors [color-scheme:dark]';

export const cardClass =
  'rounded-xl border border-border-dark bg-night p-3.5';

export const emptyStateClass =
  'rounded-xl border border-border-dark bg-night p-6 text-center';