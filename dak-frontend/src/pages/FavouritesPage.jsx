import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchSavedItems, removeSavedItem } from '../api/savedItems';
import Layout from '../components/Layout';

function resourceLink(item) {
  if (item.resourceType === 'BUSINESS') return `/businesses/${item.slugOrId}`;
  if (item.resourceType === 'AUSTRALIA_UPDATE') return `/australia-updates/${item.slugOrId}`;
  return '#';
}

function FavouritesPage() {
  const [savedItems, setSavedItems] = useState([]);
  const [loading, setLoading] = useState(true);

  function loadItems() {
    setLoading(true);
    fetchSavedItems()
      .then(setSavedItems)
      .catch(console.error)
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadItems();
  }, []);

  async function handleRemove(savedItemId) {
    try {
      await removeSavedItem(savedItemId);
      loadItems();
    } catch (error) {
      console.error('삭제 실패:', error);
    }
  }

  return (
    <Layout>
      <div className="p-4">
        <h1 className="text-xl font-bold text-ink mb-6">즐겨찾기</h1>

        {loading ? (
          <p className="text-gray-400">불러오는 중...</p>
        ) : savedItems.length === 0 ? (
          <p className="text-gray-400">아직 저장한 항목이 없어요.</p>
        ) : (
          <div className="grid gap-3">
            {savedItems.map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-xl p-4 flex items-center justify-between border border-gray-100"
              >
                <Link to={resourceLink(item)} className="flex-1">
                  <p className="font-medium text-ink">{item.title}</p>
                  <p className="text-xs text-gray-400">{item.resourceType}</p>
                </Link>
                <button
                  onClick={() => handleRemove(item.id)}
                  className="text-sm text-adelaide-red ml-3"
                >
                  삭제
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}

export default FavouritesPage;