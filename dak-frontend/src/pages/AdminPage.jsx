import { useState, useEffect } from 'react';
import {
  fetchPendingBusinesses,
  updateBusinessStatus,
  fetchDraftUpdates,
  updateUpdateStatus,
} from '../api/admin';
import Layout from '../components/Layout';

function AdminPage() {
  const [businesses, setBusinesses] = useState([]);
  const [updates, setUpdates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadData() {
    setLoading(true);
    try {
      const [businessesData, updatesData] = await Promise.all([
        fetchPendingBusinesses(),
        fetchDraftUpdates(),
      ]);
      setBusinesses(businessesData.content);
      setUpdates(updatesData.content);
    } catch (err) {
      setError(err.response?.data?.error?.message || '관리자 데이터를 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleBusinessAction(businessId, status) {
    try {
      await updateBusinessStatus(businessId, status);
      loadData();
    } catch (err) {
      alert(err.response?.data?.error?.message || '처리에 실패했습니다.');
    }
  }

  async function handleUpdateAction(updateId, status) {
    try {
      await updateUpdateStatus(updateId, status);
      loadData();
    } catch (err) {
      alert(err.response?.data?.error?.message || '처리에 실패했습니다.');
    }
  }

  if (loading) {
    return (
      <Layout>
        <div className="p-4 text-gray-400">불러오는 중...</div>
      </Layout>
    );
  }

  if (error) {
    return (
      <Layout>
        <div className="p-4">
          <p className="text-adelaide-red">{error}</p>
          <p className="text-gray-400 text-sm mt-1">관리자 권한이 있는 계정으로 로그인했는지 확인해주세요.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-4">
        <h1 className="text-xl font-bold text-ink mb-6">관리자 대시보드</h1>

        <section className="mb-8">
          <h2 className="text-lg font-semibold text-ink mb-3">승인 대기 업체 ({businesses.length})</h2>
          {businesses.length === 0 ? (
            <p className="text-gray-400 text-sm">대기 중인 업체가 없어요.</p>
          ) : (
            <div className="flex flex-col gap-2">
              {businesses.map((business) => (
                <div key={business.id} className="bg-white rounded-xl p-4 flex items-center justify-between">
                  <span className="font-medium text-ink">{business.name}</span>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleBusinessAction(business.id, 'PUBLISHED')}
                      className="text-sm bg-korea-blue text-white px-3 py-1.5 rounded-lg"
                    >
                      승인
                    </button>
                    <button
                      onClick={() => handleBusinessAction(business.id, 'REJECTED')}
                      className="text-sm bg-gray-100 text-ink px-3 py-1.5 rounded-lg"
                    >
                      거부
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="text-lg font-semibold text-ink mb-3">
            승인 대기 Australia Updates ({updates.length})
          </h2>
          {updates.length === 0 ? (
            <p className="text-gray-400 text-sm">대기 중인 업데이트가 없어요.</p>
          ) : (
            <div className="flex flex-col gap-2">
              {updates.map((update) => (
                <div key={update.id} className="bg-white rounded-xl p-4">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-ink">{update.title}</span>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleUpdateAction(update.id, 'PUBLISHED')}
                        disabled={!update.hasCategory || !update.hasSource}
                        className="text-sm bg-korea-blue text-white px-3 py-1.5 rounded-lg disabled:opacity-40"
                      >
                        승인
                      </button>
                      <button
                        onClick={() => handleUpdateAction(update.id, 'ARCHIVED')}
                        className="text-sm bg-gray-100 text-ink px-3 py-1.5 rounded-lg"
                      >
                        보관
                      </button>
                    </div>
                  </div>
                  {(!update.hasCategory || !update.hasSource) && (
                    <p className="text-xs text-adelaide-red mt-1">
                      ⚠ {!update.hasCategory && '카테고리 미지정'} {!update.hasSource && '출처 없음'}
                      — 승인 전 완성 필요
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </Layout>
  );
}

export default AdminPage;