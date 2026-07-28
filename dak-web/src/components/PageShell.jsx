/**
 * Standard page frame: a lighter content column inset from the darker page
 * background, optionally flanked by a right-hand rail.
 *
 * Extracted after the same wrapper markup had been copied into seven pages.
 * Detail pages use it too — the two-column article/metadata split lives
 * inside `children`, not here.
 *
 * @param {React.ReactNode} children  Main content.
 * @param {React.ReactNode} [aside]   Right rail, shown at >=1024px only.
 *                                    Below that the nav sidebar already claims
 *                                    enough width that a third column cramps
 *                                    the content.
 */
function PageShell({ children, aside = null }) {
    return (
      <div className="flex gap-0 lg:gap-4 p-0 lg:p-4 min-h-screen items-stretch">
        <main className="flex-1 min-w-0 bg-surface lg:rounded-2xl p-4 md:p-6 animate-page-enter">
          {children}
        </main>
  
        {aside && <div className="hidden lg:block">{aside}</div>}
      </div>
    );
  }
  
  export default PageShell;