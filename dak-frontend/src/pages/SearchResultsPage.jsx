import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Search, BadgeCheck, ImageOff, BookOpen, Newspaper } from 'lucide-react';
import { fetchBusinesses } from '../api/businesses';
import { fetchGuides } from '../api/guides';
import { fetchUpdates } from '../api/updates';
import { isNew, timeAgo } from '../utils/date';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import NewBadge from '../components/NewBadge';
import PageMeta from '../components/PageMeta';

// Result types the search covers. 'all' is the default; the others let someone
// who knows what kind of thing they want skip past the rest.
const TYPES = [
  { value: 'all', label: 'All' },
  { value: 'businesses', label: 'Businesses' },
  { value: 'guides', label: 'Guides' },
  { value: 'updates', label: 'Updates' },
];

/**
 * Horizontal result row: small thumbnail left, text right. Search results are
 * scanned rather than browsed, so a compact row fits more per screen than the
 * card grid used on Directory.
 */
function SearchResultRow({ business }) {
  return (
    <article
      className="flex gap-3.5 p-3 rounded-2xl border border-border-dark bg-night
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      {/* Thumbnail. Falls back to an icon when the business has no images. */}
      <div className="w-16 h-16 shrink-0 rounded-xl bg-surface flex items-center justify-center overflow-hidden">
        {business.thumbnailUrl ? (
          <img
            src={business.thumbnailUrl}
            alt=""
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <ImageOff size={18} strokeWidth={1.5} className="text-border-dark" />
        )}
      </div>

      <div className="min-w-0 flex-1 self-center">
        <div className="flex items-start gap-1.5">
          <h3 className="font-semibold text-snow text-[15px] leading-snug line-clamp-1">
            {business.name}
          </h3>
          {business.verified && (
            <BadgeCheck
              size={15}
              strokeWidth={2}
              className="text-korea-blue shrink-0 mt-0.5"
              aria-label="Verified business"
            />
          )}
          {isNew(business.createdAt) && <NewBadge />}
        </div>

        {business.shortDescription && (
          <p className="text-[13px] text-muted mt-0.5 line-clamp-1">
            {business.shortDescription}
          </p>
        )}
        {business.suburb && (
          <p className="text-[12px] text-faint mt-0.5">{business.suburb}</p>
        )}
      </div>
    </article>
  );
}

/**
 * Guides and updates share a row shape: an icon where a business has its
 * thumbnail, since neither carries an image. The icon also tells a reader what
 * kind of result they are looking at without a text label.
 */
function ArticleResultRow({ icon: Icon, title, summary, meta, isNewItem }) {
  return (
    <article
      className="flex gap-3.5 p-3 rounded-2xl border border-border-dark bg-night
                 hover:border-faint hover:-translate-y-1 transition-all duration-300"
    >
      <div className="w-16 h-16 shrink-0 rounded-xl bg-surface flex items-center justify-center">
        <Icon size={20} strokeWidth={1.5} className="text-muted" />
      </div>

      <div className="min-w-0 flex-1 self-center">
        <div className="flex items-start gap-1.5">
          <h3 className="font-semibold text-snow text-[15px] leading-snug line-clamp-1">
            {title}
          </h3>
          {isNewItem && <NewBadge />}
        </div>

        {summary && (
          <p className="text-[13px] text-muted mt-0.5 line-clamp-1">{summary}</p>
        )}
        {meta && <p className="text-[12px] text-faint mt-0.5">{meta}</p>}
      </div>
    </article>
  );
}

function SearchResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get('keyword') || '';
  const activeType = searchParams.get('type') || 'all';

  // Local input state so typing doesn't refire the query on every keystroke;
  // the URL is only updated on submit.
  const [inputValue, setInputValue] = useState(keyword);
  const [businesses, setBusinesses] = useState([]);
  const [guides, setGuides] = useState([]);
  const [updates, setUpdates] = useState([]);
  const [counts, setCounts] = useState({ businesses: 0, guides: 0, updates: 0 });
  const [loading, setLoading] = useState(true);

  // Keep the input in sync when the URL changes from outside this page
  // (browser back/forward, or a search submitted from Home).
  useEffect(() => {
    setInputValue(keyword);
  }, [keyword]);

  useEffect(() => {
    async function loadResults() {
      setLoading(true);

      // Three separate endpoints rather than one search API: each resource has
      // its own keyword parameter already, and a unified endpoint would have to
      // reconcile three different result shapes server-side for no gain here.
      // Promise.allSettled, not all — one failing type should not blank the
      // other two.
      const wantBusinesses = activeType === 'all' || activeType === 'businesses';
      const wantGuides = activeType === 'all' || activeType === 'guides';
      const wantUpdates = activeType === 'all' || activeType === 'updates';

      const [businessResult, guideResult, updateResult] = await Promise.allSettled([
        wantBusinesses
          ? fetchBusinesses({ keyword: keyword || undefined })
          : Promise.resolve(null),
        wantGuides ? fetchGuides({ keyword: keyword || undefined }) : Promise.resolve(null),
        wantUpdates ? fetchUpdates({ keyword: keyword || undefined }) : Promise.resolve(null),
      ]);

      function unpack(result, label) {
        if (result.status === 'rejected') {
          console.error(`Search failed for ${label}:`, result.reason);
          return { content: [], totalElements: 0 };
        }
        return {
          content: result.value?.content ?? [],
          totalElements: result.value?.totalElements ?? 0,
        };
      }

      const b = unpack(businessResult, 'businesses');
      const g = unpack(guideResult, 'guides');
      const u = unpack(updateResult, 'updates');

      setBusinesses(b.content);
      setGuides(g.content);
      setUpdates(u.content);
      setCounts({
        businesses: b.totalElements,
        guides: g.totalElements,
        updates: u.totalElements,
      });
      setLoading(false);
    }
    loadResults();
  }, [keyword, activeType]);

  // Both the keyword and the active type live in the URL, so each change must
  // preserve the other. Refining a search should not reset which tab you are on.
  function updateParams(changes) {
    const next = {
      ...(keyword && { keyword }),
      ...(activeType !== 'all' && { type: activeType }),
      ...changes,
    };

    Object.keys(next).forEach((key) => {
      if (!next[key]) delete next[key];
    });

    setSearchParams(next);
  }

  function handleSubmit(e) {
    e.preventDefault();
    updateParams({ keyword: inputValue.trim() });
  }

  const totalCount = counts.businesses + counts.guides + counts.updates;
  const hasResults = businesses.length > 0 || guides.length > 0 || updates.length > 0;

  return (
    <Layout>
      <PageShell>
      <PageMeta
          title={keyword ? `Search: ${keyword}` : 'Search'}
          path="/search"
          noIndex
        />
        <h1 className="text-xl font-bold text-snow mb-4">Search</h1>

        {/* Search stays on the page so results can be refined without
            navigating back to Home. */}
        <form onSubmit={handleSubmit} className="mb-4">
          <div className="relative">
            <Search
              size={18}
              strokeWidth={1.75}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-faint pointer-events-none"
            />
            <input
              type="search"
              placeholder="Search businesses, guides, places"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              aria-label="Search"
              className="w-full pl-11 pr-4 py-3.5 rounded-xl border border-border-dark bg-night
                         text-[16px] md:text-[15px] text-snow placeholder:text-faint
                         outline-none focus:border-faint transition-colors [color-scheme:dark]"
            />
          </div>
        </form>

        {/* Type tabs, matching the chip pattern used on Guides and AU Updates.
            Search carries no other filters: Directory and Guides each hold
            their own category controls, and duplicating them here would mean
            maintaining the same filter in three places. */}
        <div
          className="flex gap-2 mb-5 overflow-x-auto pb-2 -mx-4 px-4
                     md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
        >
          {TYPES.map((type) => (
            <button
              key={type.value}
              type="button"
              onClick={() => updateParams({ type: type.value === 'all' ? '' : type.value })}
              aria-pressed={activeType === type.value}
              className={`shrink-0 px-4 py-1.5 rounded-full text-[13px] border
                          transition-all duration-300 ${
                activeType === type.value
                  ? 'bg-snow text-night border-snow font-medium'
                  : 'bg-transparent text-muted border-border-dark hover:text-snow hover:border-faint hover:-translate-y-0.5'
              }`}
            >
              {type.label}
            </button>
          ))}
        </div>

        <h2 className="text-lg font-semibold text-snow mb-1">
          {keyword ? `Results for "${keyword}"` : 'Everything on DAK'}
        </h2>
        <p className="text-sm text-muted mb-4">
          {loading ? '\u00A0' : `${totalCount} result${totalCount === 1 ? '' : 's'}`}
        </p>

        {loading ? (
          <div className="grid gap-2.5">
            {[0, 1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="h-[88px] rounded-2xl bg-night border border-border-dark animate-pulse"
              />
            ))}
          </div>
        ) : !hasResults ? (
          <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
            <p className="text-muted text-sm">
              {keyword ? `No results for "${keyword}".` : 'Nothing to show.'}
            </p>
            <p className="text-faint text-[13px] mt-1.5">
              Try a different keyword or browse the{' '}
              <Link to="/directory" className="text-korea-blue hover:underline">
                directory
              </Link>
              .
            </p>
          </div>
        ) : (
          /* Grouped by type rather than interleaved: the three carry different
             information, and a mixed list would force a reader to work out what
             each row is before deciding whether it answers their question.
             Guides lead — someone searching a topic usually wants the
             explanation before the business listing. */
          <div className="flex flex-col gap-6">
            {guides.length > 0 && (
              <section>
                <div className="flex items-baseline justify-between gap-4 mb-2.5">
                  <h3 className="text-[15px] font-semibold text-snow">
                    Guides <span className="text-muted font-normal">({counts.guides})</span>
                  </h3>
                  {activeType === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ type: 'guides' })}
                      className="text-[13px] text-muted hover:text-snow transition-colors shrink-0"
                    >
                      See all
                    </button>
                  )}
                </div>
                <div className="grid gap-2.5">
                  {guides.map((guide) => (
                    <Link key={guide.id} to={`/guides/${guide.slug}`} className="block">
                      <ArticleResultRow
                        icon={BookOpen}
                        title={guide.title}
                        summary={guide.summary}
                        meta={guide.category?.name}
                        isNewItem={isNew(guide.publishedAt)}
                      />
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {businesses.length > 0 && (
              <section>
                <div className="flex items-baseline justify-between gap-4 mb-2.5">
                  <h3 className="text-[15px] font-semibold text-snow">
                    Businesses <span className="text-muted font-normal">({counts.businesses})</span>
                  </h3>
                  {activeType === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ type: 'businesses' })}
                      className="text-[13px] text-muted hover:text-snow transition-colors shrink-0"
                    >
                      See all
                    </button>
                  )}
                </div>
                <div className="grid gap-2.5">
                  {businesses.map((business) => (
                    <Link key={business.id} to={`/businesses/${business.slug}`} className="block">
                      <SearchResultRow business={business} />
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {updates.length > 0 && (
              <section>
                <div className="flex items-baseline justify-between gap-4 mb-2.5">
                  <h3 className="text-[15px] font-semibold text-snow">
                    Australia updates{' '}
                    <span className="text-muted font-normal">({counts.updates})</span>
                  </h3>
                  {activeType === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ type: 'updates' })}
                      className="text-[13px] text-muted hover:text-snow transition-colors shrink-0"
                    >
                      See all
                    </button>
                  )}
                </div>
                <div className="grid gap-2.5">
                  {updates.map((update) => (
                    <Link
                      key={update.id}
                      to={`/australia-updates/${update.id}`}
                      className="block"
                    >
                      <ArticleResultRow
                        icon={Newspaper}
                        title={update.title}
                        summary={update.koreanSummary}
                        meta={update.createdAt ? timeAgo(update.createdAt) : null}
                        isNewItem={isNew(update.createdAt)}
                      />
                    </Link>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default SearchResultsPage;