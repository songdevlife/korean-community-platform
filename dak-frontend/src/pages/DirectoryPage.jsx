import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchBusinesses, fetchBusinessCategories } from '../api/businesses';
import Layout from '../components/Layout';
import CategoryChip from '../components/CategoryChip';
import BusinessCard from '../components/BusinessCard';

function DirectoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeCategory = searchParams.get('category') || '';

  const [categories, setCategories] = useState([]);
  const [businesses, setBusinesses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBusinessCategories().then(setCategories).catch(console.error);
  }, []);

  useEffect(() => {
    setLoading(true);
    fetchBusinesses({ category: activeCategory || undefined })
      .then((data) => setBusinesses(data.content))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [activeCategory]);

  function handleCategoryClick(categoryName) {
    if (activeCategory === categoryName) {
      setSearchParams({});
    } else {
      setSearchParams({ category: categoryName });
    }
  }

  return (
    <Layout>
      <div className="p-4">
        <h1 className="text-xl font-bold text-ink mb-4">업체 디렉토리</h1>

        <div className="flex gap-2 overflow-x-auto pb-2 mb-4">
          {categories.map((category) => (
            <CategoryChip
              key={category.id}
              name={category.name}
              active={activeCategory === category.name}
              onClick={() => handleCategoryClick(category.name)}
            />
          ))}
        </div>

        {loading ? (
          <p className="text-gray-400">불러오는 중...</p>
        ) : businesses.length === 0 ? (
          <p className="text-gray-400">해당 카테고리에 등록된 업체가 없어요.</p>
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

export default DirectoryPage;