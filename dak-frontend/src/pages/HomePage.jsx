import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { fetchBusinesses, fetchBusinessCategories } from '../api/businesses';
import { fetchGuides } from '../api/guides';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import CategoryChip from '../components/CategoryChip';
import BusinessCard from '../components/BusinessCard';
import GuideCard from '../components/GuideCard';
import UpdatesSidePanel from '../components/UpdatesSidePanel';
import PageMeta from '../components/PageMeta';

// Home is a preview, not a listing: a handful of cards to show what the site
// holds, with everything else behind View all.
const FEATURED_COUNT = 3;

// Guides lead the page: 02 Product Vision treats search as the primary
// acquisition channel, and a guide is what a search arrives on.
const FEATURED_GUIDE_COUNT = 3;

function HomePage() {
  const [categories, setCategories] = useState([]);
  const [businesses, setBusinesses] = useState([]);
  const [guides, setGuides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    async function loadData() {
      try {
        const [categoriesData, businessesData, guidesData] = await Promise.all([
          fetchBusinessCategories(),
          fetchBusinesses({ pageSize: FEATURED_COUNT }),
          fetchGuides({ pageSize: FEATURED_GUIDE_COUNT }),
        ]);
        setCategories(categoriesData ?? []);
        setBusinesses(businessesData?.content ?? []);
        setGuides(guidesData?.content ?? []);
      } catch (error) {
        console.error('Failed to load home data:', error);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  function handleSearch(e) {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/search?keyword=${encodeURIComponent(searchKeyword)}`);
    }
  }

  return (
    <Layout>
      
      <PageShell aside={<UpdatesSidePanel />}>
      <PageMeta
          path="/"
          description="Korean-language local information for Adelaide: find Korean-speaking businesses, services and community updates in South Australia."
        />
        {/* Page header row. Sign-up CTA is a secondary conversion path —
            the primary one is the save action on detail pages (06 UI/UX 7). */}
        <div className="flex items-center justify-between gap-4 mb-4">
          <p className="text-sm text-muted truncate">
            {user ? `Welcome back, ${user.displayName}` : 'Discover Korean Adelaide'}
          </p>
          {!user && (
            <Link
              to="/login"
              className="shrink-0 px-4 py-1.5 rounded-full bg-snow text-night
                         text-[13px] font-medium hover:bg-white transition-colors duration-300"
            >
              Sign up
            </Link>
          )}
        </div>

        {/* Hero block. Wraps the headline and search in their own raised
            panel, per the home wireframe. */}
        <section className="bg-night rounded-2xl p-5 md:p-6 mb-5">
          <h1 className="text-base md:text-lg font-semibold text-snow text-center mb-3">
            What are you looking for?
          </h1>

          {/* 16px font on mobile stops iOS Safari zooming on focus. */}
          <form onSubmit={handleSearch}>
            <div className="relative">
              <Search
                size={18}
                strokeWidth={1.75}
                className="absolute left-4 top-1/2 -translate-y-1/2 text-faint pointer-events-none"
              />
              <input
                type="text"
                placeholder="Search businesses, guides, places"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                aria-label="Search"
                className="w-full pl-11 pr-4 py-3.5 rounded-xl border border-border-dark bg-night
                           text-[16px] md:text-[15px] text-snow
                           placeholder:text-faint placeholder:font-semibold
                           outline-none focus:border-faint transition-colors [color-scheme:dark]"
              />
            </div>
          </form>
        </section>

        {/* Chips scroll horizontally on mobile, wrap onto rows on desktop. */}
        {categories.length > 0 && (
          <div
            className="flex gap-2 mb-6 overflow-x-auto pb-2 -mx-4 px-4
                       md:mx-0 md:px-0 md:pb-0 md:overflow-visible md:flex-wrap"
          >
            {categories.map((category) => (
              <Link
                key={category.id}
                to={`/directory?category=${encodeURIComponent(category.name)}`}
                className="shrink-0"
              >
                <CategoryChip name={category.name} />
              </Link>
            ))}
          </div>
        )}

        {/* Guides preview, above the directory. Hidden entirely when nothing is
            published rather than showing an empty state: a guest has no way to
            add one, so the message would be noise. */}
        {(loading || guides.length > 0) && (
          <>
            <div className="flex items-baseline justify-between gap-4 mb-3">
              <h2 className="text-lg font-semibold text-snow">Local guides</h2>
              <Link
                to="/guides"
                className="text-sm text-muted hover:text-snow transition-colors shrink-0"
              >
                View all
              </Link>
            </div>

            {loading ? (
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {[0, 1, 2].map((i) => (
                  <div
                    key={i}
                    className="h-32 rounded-2xl bg-night border border-border-dark animate-pulse"
                  />
                ))}
              </div>
            ) : (
              /* Single column on mobile, unlike the business grid: a guide card
                 carries a title and summary, which are unreadable at a third
                 width on a phone. */
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {guides.map((guide) => (
                  <Link key={guide.id} to={`/guides/${guide.slug}`} className="min-w-0">
                    {/* No onFilter: home has nowhere to apply a category
                        filter, so the tag renders as a plain label. */}
                    <GuideCard guide={guide} />
                  </Link>
                ))}
              </div>
            )}
          </>
        )}

        <div className="flex items-baseline justify-between gap-4 mb-3 mt-8">
          <h2 className="text-lg font-semibold text-snow">Featured businesses</h2>
          <Link
            to="/directory"
            className="text-sm text-muted hover:text-snow transition-colors shrink-0"
          >
            View all
          </Link>
        </div>

        {loading ? (
          /* Skeletons rather than a text message, so the layout doesn't
             jump once the real cards arrive. */
          <div className="grid gap-3 grid-cols-3">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="rounded-2xl bg-night border border-border-dark overflow-hidden animate-pulse"
              >
                <div className="aspect-[3/2] bg-surface" />
                <div className="p-3">
                  <div className="h-3.5 w-3/4 rounded bg-surface mb-2" />
                  <div className="h-3 w-1/2 rounded bg-surface" />
                </div>
              </div>
            ))}
          </div>
        ) : businesses.length === 0 ? (
          <div className="rounded-xl border border-border-dark bg-night p-8 text-center">
            <p className="text-muted text-sm">No businesses listed yet.</p>
          </div>
        ) : (
          /* Three across at every width. Home shows a sample rather than a
             listing, so the cards stay small. */
          <div className="grid gap-3 grid-cols-3">
            {businesses.map((business) => (
              <Link key={business.id} to={`/businesses/${business.slug}`} className="min-w-0">
                <BusinessCard business={business} compact />
              </Link>
            ))}
          </div>
        )}
      </PageShell>
    </Layout>
  );
}

export default HomePage;