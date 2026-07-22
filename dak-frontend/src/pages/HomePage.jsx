import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchBusinesses, fetchBusinessCategories } from '../api/businesses';
import Layout from '../components/Layout';
import CategoryChip from '../components/CategoryChip';
import BusinessCard from '../components/BusinessCard';

function HomePage() {
  const [categories, setCategories] = useState([]);
  const [businesses, setBusinesses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    async function loadData() {
      try {
        const [categoriesData, businessesData] = await Promise.all([
          fetchBusinessCategories(),
          fetchBusinesses(),
        ]);
        setCategories(categoriesData);
        setBusinesses(businessesData.content);
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
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
      <div className="p-4">
        <form onSubmit={handleSearch} className="mb-4">
          <input
            type="text"
            placeholder="업체, 정보 검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="w-full p-3 rounded-xl border border-gray-200 bg-white"
          />
        </form>

        <div className="flex gap-2 overflow-x-auto pb-2 mb-4">
          {categories.map((category) => (
            <Link key={category.id} to={`/directory?category=${encodeURIComponent(category.name)}`}>
              <CategoryChip name={category.name} />
            </Link>
          ))}
        </div>

        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-ink">추천 업체</h2>
          <Link to="/directory" className="text-sm text-korea-blue">
            전체보기 →
          </Link>
        </div>
        {loading ? (
          <p className="text-gray-400">불러오는 중...</p>
        ) : businesses.length === 0 ? (
          <p className="text-gray-400">아직 등록된 업체가 없어요.</p>
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

export default HomePage;