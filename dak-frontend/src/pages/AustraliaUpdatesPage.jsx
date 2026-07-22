import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchUpdates } from '../api/updates';
import Layout from '../components/Layout';
import UpdateCard from '../components/UpdateCard';

function AustraliaUpdatesPage() {
  const [updates, setUpdates] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUpdates()
      .then((data) => setUpdates(data.content))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <div className="p-4">
        <h1 className="text-xl font-bold text-ink mb-4">Australia Updates</h1>

        {loading ? (
          <p className="text-gray-400">불러오는 중...</p>
        ) : updates.length === 0 ? (
          <p className="text-gray-400">아직 게시된 업데이트가 없어요.</p>
        ) : (
          <div className="grid gap-3">
            {updates.map((update) => (
              <Link key={update.id} to={`/australia-updates/${update.id}`}>
                <UpdateCard update={update} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}

export default AustraliaUpdatesPage;