/**
 * Marks recently added items. Rendered by the caller only when isNew() passes,
 * so this component has no date logic of its own.
 *
 * aria-label rather than bare text: "NEW" read aloud beside a heading is
 * ambiguous, whereas "Recently added" is not.
 */
function NewBadge() {
    return (
      <span
        aria-label="Recently added"
        className="shrink-0 px-1.5 py-0.5 rounded-full bg-adelaide-red text-white
                   text-[10px] font-bold leading-none tracking-wide uppercase"
      >
        New
      </span>
    );
  }
  
  export default NewBadge;