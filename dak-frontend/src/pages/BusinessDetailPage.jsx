import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../api/client';
import { saveItem, checkIsSaved } from '../api/savedItems';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';

function BusinessDetailPage() {
  const { slug } = useParams();
  const { user } = useAuth();
  const [business, setBusiness] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState('');

  useEffect(() => {
    async function loadBusiness() {
      try {
        const response = await apiClient.get(`/businesses/${slug}`);
        const businessData = response.data.data;
        setBusiness(businessData);

        if (user) {
          const alreadySaved = await checkIsSaved('BUSINESS', businessData.id);
          setSaved(alreadySaved);
        }
      } catch (error) {
        if (error.response?.status === 404) {
          setNotFound(true);
        }
        console.error('업체 조회 실패:', error);
      } finally {
        setLoading(false);
      }
    }
    loadBusiness();
  }, [slug, user]);

  async function handleSave() {
    if (!user) {
      setSaveError('로그인이 필요해요.');
      return;
    }
    try {
      await saveItem('BUSINESS', business.id);
      setSaved(true);
      setSaveError('');
    } catch (error) {
      if (error.response?.status === 409) {
        setSaved(true);
      } else {
        setSaveError('저장에 실패했어요.');
      }
    }
  }

  if (loading) {
    return (
      <Layout>
        <div className="p-4 text-gray-400">불러오는 중...</div>
      </Layout>
    );
  }

  if (notFound || !business) {
    return (
      <Layout>
        <div className="p-4">
          <p className="text-gray-400">업체를 찾을 수 없어요.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-4">
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-ink">{business.name}</h1>
            {business.verified && (
              <span className="text-xs bg-korea-blue text-white px-2 py-1 rounded-full">
                인증됨
              </span>
            )}
          </div>

          {business.shortDescription && (
            <p className="text-gray-500 mt-1">{business.shortDescription}</p>
          )}

          <div className="mt-4 flex flex-wrap gap-2">
            {business.categories?.map((category) => (
              <span
                key={category.id}
                className="text-xs bg-cream border border-gray-200 px-3 py-1 rounded-full text-ink"
              >
                {category.name}
              </span>
            ))}
          </div>

          {business.description && (
            <p className="text-ink mt-4 whitespace-pre-line">{business.description}</p>
          )}

          <div className="mt-6 border-t border-gray-100 pt-4 flex flex-col gap-2 text-sm text-ink">
            {business.addressLine && <p>📍 {business.addressLine}, {business.suburb}</p>}
            {business.phone && <p>📞 {business.phone}</p>}
            {business.email && <p>✉️ {business.email}</p>}
            {business.websiteUrl && (
              <a href={business.websiteUrl} target="_blank" rel="noreferrer" className="text-korea-blue">
                🔗 웹사이트 방문
              </a>
            )}
          </div>

          <div className="mt-6 border-t border-gray-100 pt-4">
            <button
              onClick={handleSave}
              disabled={saved}
              className={`text-sm px-4 py-2 rounded-xl font-medium ${
                saved ? 'bg-gray-100 text-gray-400' : 'bg-korea-blue text-white'
              }`}
            >
              {saved ? '★ 저장됨' : '☆ 즐겨찾기 저장'}
            </button>
            {saveError && <p className="text-adelaide-red text-xs mt-2">{saveError}</p>}
          </div>
        </div>
      </div>
    </Layout>
  );
}

export default BusinessDetailPage;