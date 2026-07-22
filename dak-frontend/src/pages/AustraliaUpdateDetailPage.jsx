import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { fetchUpdateById } from '../api/updates';
import { saveItem, checkIsSaved } from '../api/savedItems';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';

function AustraliaUpdateDetailPage() {
  const { updateId } = useParams();
  const { user } = useAuth();
  const [update, setUpdate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState('');

  useEffect(() => {
    async function loadUpdate() {
      try {
        const data = await fetchUpdateById(updateId);
        setUpdate(data);

        if (user) {
          const alreadySaved = await checkIsSaved('AUSTRALIA_UPDATE', data.id);
          setSaved(alreadySaved);
        }
      } catch (error) {
        if (error.response?.status === 404) setNotFound(true);
        console.error('업데이트 조회 실패:', error);
      } finally {
        setLoading(false);
      }
    }
    loadUpdate();
  }, [updateId, user]);

  async function handleSave() {
    if (!user) {
      setSaveError('로그인이 필요해요.');
      return;
    }
    try {
      await saveItem('AUSTRALIA_UPDATE', update.id);
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

  if (notFound || !update) {
    return (
      <Layout>
        <div className="p-4">
          <p className="text-gray-400">업데이트를 찾을 수 없어요.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-4">
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <div className="flex items-center gap-2 mb-3">
            {update.category && (
              <span className="text-xs bg-cream border border-gray-200 px-2 py-0.5 rounded-full text-ink">
                {update.category.name}
              </span>
            )}
            <span className="text-xs text-gray-400">{update.geographicScope}</span>
          </div>

          <h1 className="text-2xl font-bold text-ink">{update.title}</h1>

          {update.aiGenerated && (
            <div className="mt-2 text-xs text-korea-blue bg-blue-50 rounded-lg px-3 py-2">
              ✨ 이 요약은 AI의 도움을 받아 작성되었으며, 관리자가 검토·승인했습니다.
            </div>
          )}

          <p className="text-ink mt-4 whitespace-pre-line">{update.koreanSummary}</p>

          {update.sources?.length > 0 && (
            <div className="mt-6 border-t border-gray-100 pt-4">
              <h2 className="text-sm font-semibold text-ink mb-2">출처</h2>
              <ul className="flex flex-col gap-1">
                {update.sources.map((source) => (
                  <li key={source.id}><a
                    
                      href={source.sourceUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-sm text-korea-blue"
                    >
                      🔗 {source.sourceTitle || source.sourceName}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          )}

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

export default AustraliaUpdateDetailPage;