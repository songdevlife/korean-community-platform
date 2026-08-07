import Link from 'next/link';
import { getBusinesses, getEvents, getGuides, getUpdates } from '@/api/server';
import PageShell from '@/components/PageShell';
import SearchForm from '@/components/SearchForm';
import SearchLogger from '@/components/SearchLogger';
import { BusinessResultRow, ArticleResultRow } from '@/components/SearchResultRow';
import { timeAgo, isNew, eventDate, eventTime } from '@/utils/date';

export const metadata = {
  title: 'Search',
  // The query space is unbounded and every permutation is a near-duplicate of
  // the others, so results are kept out of the index. Server-rendering them is
  // still worth it: a shared search link opens to actual results, and the page
  // works without JavaScript.
  robots: { index: false, follow: true },
};

export default async function SearchPage({ searchParams }) {
  const params = await searchParams;
  const keyword = params?.keyword ?? '';
  const activeType = params?.type || 'all';

  const wantBusinesses = activeType === 'all' || activeType === 'businesses';
  const wantGuides = activeType === 'all' || activeType === 'guides';
  const wantUpdates = activeType === 'all' || activeType === 'updates';
  const wantEvents = activeType === 'all' || activeType === 'events';

  // allSettled rather than all: one failing type should not blank the others.
  const [businessResult, guideResult, updateResult, eventResult] = await Promise.allSettled([
    wantBusinesses ? getBusinesses({ keyword: keyword || undefined }) : null,
    wantGuides ? getGuides({ keyword: keyword || undefined }) : null,
    wantUpdates ? getUpdates({ keyword: keyword || undefined }) : null,
    // Upcoming only, as the listing is. A past event is a worse search result
    // than none: it answers the question and then wastes the trip.
    wantEvents ? getEvents({ keyword: keyword || undefined }) : null,
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

  const businesses = unpack(businessResult, 'businesses');
  const guides = unpack(guideResult, 'guides');
  const updates = unpack(updateResult, 'updates');
  const events = unpack(eventResult, 'events');

  const totalCount =
    businesses.totalElements + guides.totalElements +
    updates.totalElements + events.totalElements;
  const hasResults =
    businesses.content.length > 0 || guides.content.length > 0 ||
    updates.content.length > 0 || events.content.length > 0;

  return (
    <PageShell>
      <SearchLogger keyword={keyword} resultCount={totalCount} />
      <h1 className="text-xl font-bold text-snow mb-4">Search</h1>

      <SearchForm keyword={keyword} activeType={activeType} />

      <h2 className="text-lg font-semibold text-snow mb-1">
        {keyword ? `Results for "${keyword}"` : 'Everything on DAK'}
      </h2>
      <p className="text-sm text-muted mb-4">
        {totalCount} result{totalCount === 1 ? '' : 's'}
      </p>

      {!hasResults ? (
        <div className="rounded-2xl border border-border-dark bg-night p-8 text-center">
          <p className="text-muted text-sm">
            {keyword ? `No results for "${keyword}".` : 'Nothing to show.'}
          </p>
          <p className="text-faint text-[13px] mt-1.5">
            Try a different keyword or browse the{' '}
            <Link href="/directory" className="text-korea-blue hover:underline">
              directory
            </Link>
            .
          </p>
        </div>
      ) : (
        /* Grouped by type rather than interleaved: the three carry different
           information, and a mixed list would force a reader to work out what
           each row is before deciding whether it answers their question. Guides
           lead — someone searching a topic usually wants the explanation before
           the business listing. */
           <div className="flex flex-col gap-6">
           {/* Events lead when there are any. Everything else on the site keeps
               until tomorrow; an event has a date, and one this week is the
               only result on the page that stops being useful if it is scrolled
               past. */}
           {events.content.length > 0 && (
             <section>
               <h3 className="text-[15px] font-semibold text-snow mb-2.5">
                 Events <span className="text-muted font-normal">({events.totalElements})</span>
               </h3>
               <div className="grid gap-2.5">
                 {events.content.map((event) => (
                   <Link key={event.id} href={`/events/${event.slug}`} className="block">
                     <ArticleResultRow
                       kind="event"
                       title={event.title}
                       summary={`${eventDate(event.startsAt)} ${eventTime(event.startsAt)}${
                         event.venueName ? ` · ${event.venueName}` : ''
                       }`}
                       meta={event.category?.name}
                     />
                   </Link>
                 ))}
               </div>
             </section>
           )}
 
           {guides.content.length > 0 && (
            <section>
              <h3 className="text-[15px] font-semibold text-snow mb-2.5">
                Guides <span className="text-muted font-normal">({guides.totalElements})</span>
              </h3>
              <div className="grid gap-2.5">
                {guides.content.map((guide) => (
                  <Link key={guide.id} href={`/guides/${guide.slug}`} className="block">
                    <ArticleResultRow
                      kind="guide"
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

          {businesses.content.length > 0 && (
            <section>
              <h3 className="text-[15px] font-semibold text-snow mb-2.5">
                Businesses{' '}
                <span className="text-muted font-normal">({businesses.totalElements})</span>
              </h3>
              <div className="grid gap-2.5">
                {businesses.content.map((business) => (
                  <Link
                    key={business.id}
                    href={`/businesses/${business.slug}`}
                    className="block"
                  >
                    <BusinessResultRow business={business} />
                  </Link>
                ))}
              </div>
            </section>
          )}

          {updates.content.length > 0 && (
            <section>
              <h3 className="text-[15px] font-semibold text-snow mb-2.5">
                Australia updates{' '}
                <span className="text-muted font-normal">({updates.totalElements})</span>
              </h3>
              <div className="grid gap-2.5">
                {updates.content.map((update) => (
                  <Link
                    key={update.id}
                    href={`/australia-updates/${update.slug}`}
                    className="block"
                  >
                    <ArticleResultRow
                      kind="update"
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
  );
}