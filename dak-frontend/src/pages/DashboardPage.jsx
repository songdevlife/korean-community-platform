import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchSavedItems } from '../api/savedItems';
import Layout from '../components/Layout';

function DashboardPage() {
  const { user } = useAuth();
  const [savedItems, setSavedItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchSavedItems()
      .then((items) => setSavedItems(items.slice(0, 4)))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (!user) {
    return (
      <Layout>
        <div className="p-4">
          <p className="text-gray-400">로그인이 필요합니다.</p>
          <Link to="/login" className="text-korea-blue text-sm">로그인하러 가기 →</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-4 max-w-2xl">
        <h1 className="text-xl font-bold text-ink mb-6">My Dashboard</h1>

        <div className="bg-white rounded-2xl shadow-sm p-6 mb-6 flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-korea-blue text-white flex items-center justify-center text-xl font-bold">
            {user.displayName?.[0] || '?'}
          </div>
          <div>
            <p className="font-semibold text-ink">{user.displayName}</p>
            <p className="text-sm text-gray-400">{user.email}</p>
          </div>
        </div>

        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold text-ink">즐겨찾기</h2>
          <Link to="/favourites" className="text-sm text-korea-blue">전체보기 →</Link>
        </div>

        {loading ? (
          <p className="text-gray-400 text-sm">불러오는 중...</p>
        ) : savedItems.length === 0 ? (
          <p className="text-gray-400 text-sm">아직 저장한 항목이 없어요.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {savedItems.map((item) => (
              <div key={item.id} className="bg-white rounded-xl p-3 border border-gray-100">
                <p className="text-sm font-medium text-ink truncate">{item.title}</p>
                <p className="text-xs text-gray-400">{item.resourceType}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}

export default DashboardPage;