import Link from 'next/link';

/**
 * Category chip as a link rather than a button. The home page chips navigate to
 * a filtered directory, so a real anchor is both simpler and crawlable — where
 * CategoryChip takes an onClick and belongs inside a client component.
 */
export default function CategoryChipLink({ name }) {
  return (
    <Link
      href={`/directory?category=${encodeURIComponent(name)}`}
      className="shrink-0 px-4 py-1.5 rounded-full text-[13px] whitespace-nowrap border
                 bg-transparent text-muted border-border-dark
                 hover:text-snow hover:border-faint hover:-translate-y-0.5
                 transition-all duration-300"
    >
      {name}
    </Link>
  );
}