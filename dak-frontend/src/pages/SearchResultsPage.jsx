import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchBusinesses } from '../api/businesses';
import Layout from '../components/Layout';
import BusinessCard from '../components/BusinessCard';

function SearchResultsPage() {
  const [searchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';

  const [businesses, setBusinesses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadResults() {
      setLoading(true);
      try {
        const data = await fetchBusinesses({ keyword });
        setBusinesses(data.content);
      } catch (error) {
        console.error('검색 실패:', error);
      } finally {
        setLoading(false);
      }
    }
    loadResults();
  }, [keyword]);

  return (
    <Layout>
      <div className="p-4">
        <h1 className="text-xl font-bold text-ink mb-4">
          "{keyword}" 검색 결과
        </h1>

        {loading ? (
          <p className="text-gray-400">검색 중...</p>
        ) : businesses.length === 0 ? (
          <p className="text-gray-400">검색 결과가 없어요.</p>
        ) : (
          <div className="grid gap-3">
            {businesses.map((business) => (
              <Link key={business.id} to={`/businesses/${business.slug}`}>
                <BusinessCard business={business} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}

export default SearchResultsPage;