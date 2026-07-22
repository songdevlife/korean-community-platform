import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../api/auth';
import { useAuth } from '../context/AuthContext';

function LoginPage() {
  const [mode, setMode] = useState('login'); // 'login' | 'signup'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');

  const { saveSession } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const authResponse =
        mode === 'login'
          ? await login(email, password)
          : await register(email, password, displayName);
      saveSession(authResponse);
      navigate('/');
    } catch (err) {
      const message = err.response?.data?.error?.message || '오류가 발생했습니다.';
      setError(message);
    }
  }

  return (
    <div className="min-h-screen bg-cream flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-sm p-6 w-full max-w-sm">
        <div className="flex mb-6 border-b border-gray-200">
          <button
            className={`flex-1 pb-3 font-medium ${mode === 'login' ? 'text-korea-blue border-b-2 border-korea-blue' : 'text-gray-400'}`}
            onClick={() => setMode('login')}
          >
            로그인
          </button>
          <button
            className={`flex-1 pb-3 font-medium ${mode === 'signup' ? 'text-korea-blue border-b-2 border-korea-blue' : 'text-gray-400'}`}
            onClick={() => setMode('signup')}
          >
            회원가입
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          {mode === 'signup' && (
            <input
              type="text"
              placeholder="이름"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="p-3 rounded-xl border border-gray-200"
              required
            />
          )}
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="p-3 rounded-xl border border-gray-200"
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="p-3 rounded-xl border border-gray-200"
            required
          />

          {error && <p className="text-adelaide-red text-sm">{error}</p>}

          <button
            type="submit"
            className="bg-korea-blue text-white rounded-xl p-3 font-semibold mt-2"
          >
            {mode === 'login' ? '로그인' : '회원가입'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default LoginPage;