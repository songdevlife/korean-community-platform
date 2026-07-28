'use client';

/**
 * Client component because it takes an onClick handler. It is always rendered
 * from inside another client component (DirectoryFilters), so this is a
 * formality rather than a real boundary — but marking it makes the requirement
 * explicit rather than implicit in its callers.
 */
function CategoryChip({ name, active = false, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`shrink-0 px-4 py-1.5 rounded-full text-[13px] whitespace-nowrap border
                  transition-all duration-300 ${
        active
          ? 'bg-snow text-night border-snow font-medium'
          : 'bg-transparent text-muted border-border-dark hover:text-snow hover:border-faint hover:-translate-y-0.5'
      }`}
    >
      {name}
    </button>
  );
}

export default CategoryChip;