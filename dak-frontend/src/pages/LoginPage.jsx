import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import Layout from '../components/Layout';
import PageShell from '../components/PageShell';
import logoMark from '../assets/logo-mark-dark.png';
import UpdatesSidePanel from '../components/UpdatesSidePanel';
import PageMeta from '../components/PageMeta';

function LoginPage() {
  const [mode, setMode] = useState('login');
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
      const message = err.response?.data?.error?.message || 'Something went wrong.';
      setError(message);
    }
  }

  // 16px on mobile prevents iOS Safari from auto-zooming on focus
  const inputClass =
    'w-full px-[14px] py-[13px] rounded-[10px] border border-border-dark bg-night ' +
    'text-[16px] md:text-[15px] text-snow placeholder:text-faint outline-none ' +
    'focus:border-faint transition-colors duration-200 [color-scheme:dark]';

  const labelClass = 'block text-[13px] font-semibold mb-2 text-snow';

  const tabClass = (active) =>
    `py-3 text-[15px] transition-colors duration-200 ${
      active
        ? 'bg-night text-snow font-semibold'
        : 'bg-transparent text-muted font-normal'
    }`;

  return (
    <Layout>
      <PageShell aside={<UpdatesSidePanel />}>

        {/* Sits inside the standard shell rather than taking over the screen:
            logging in is a step within a browsing session, not a gateway to
            the site, and keeping the nav visible preserves that context. */}
        <div className="w-full max-w-[420px] mx-auto py-6 md:py-10">

          {/* Reduced from the full-screen version — the sidebar already carries
              the brand, so this only needs to identify the page. */}
          <div className="text-center mb-6">
            <img
              src={logoMark}
              alt=""
              aria-hidden="true"
              className="w-12 h-12 mx-auto mb-2.5 object-contain opacity-60"
            />
            <h1 className="text-xl font-bold text-snow">
              {mode === 'login' ? 'Log in' : 'Create an account'}
            </h1>
            <p className="text-[13px] text-muted mt-1">
              {mode === 'login'
                ? 'Save businesses and updates for later.'
                : 'It takes less than a minute.'}
            </p>
          </div>

          {/* Mode switch */}
          <div className="grid grid-cols-2 border border-border-dark rounded-[10px] overflow-hidden mb-6">
            <button type="button" onClick={() => setMode('login')} className={tabClass(mode === 'login')}>
              Log in
            </button>
            <button type="button" onClick={() => setMode('signup')} className={tabClass(mode === 'signup')}>
              Sign up
            </button>
          </div>

          <form onSubmit={handleSubmit} autoComplete="off">
            {mode === 'signup' && (
              <div className="mb-[18px]">
                <label htmlFor="name" className={labelClass}>Display name</label>
                <input
                  id="name"
                  type="text"
                  placeholder="Your name"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  autoComplete="off"
                  className={inputClass}
                  required
                />
              </div>
            )}

            <div className="mb-[18px]">
              <label htmlFor="email" className={labelClass}>Email</label>
              <input
                id="email"
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                className={inputClass}
                required
              />
            </div>

            <div className="mb-[18px]">
              <label htmlFor="pw" className={labelClass}>Password</label>
              <input
                id="pw"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                className={inputClass}
                required
              />
            </div>

            {/* Disabled until the password reset endpoint is implemented (see doc 07) */}
            {mode === 'login' && (
              <span className="block text-right text-[13px] text-muted cursor-not-allowed -mt-[6px] mb-5">
                Forgot password?
              </span>
            )}

            {error && <p className="text-adelaide-red text-[13px] mb-3">{error}</p>}

            <button
              type="submit"
              className="w-full py-[14px] rounded-[10px] bg-snow text-night text-[15px]
                         hover:bg-white transition-colors duration-200"
            >
              {mode === 'login' ? 'Log in' : 'Create account'}
            </button>

            <p className="text-center text-[13px] text-muted mt-[22px]">
              {mode === 'login' ? (
                <>
                  Don't have an account?{' '}
                  <button
                    type="button"
                    onClick={() => setMode('signup')}
                    className="text-[13px] text-snow font-semibold hover:underline"
                  >
                    Sign up
                  </button>
                </>
              ) : (
                <>
                  Already have an account?{' '}
                  <button
                    type="button"
                    onClick={() => setMode('login')}
                    className="text-[13px] text-snow font-semibold hover:underline"
                  >
                    Log in
                  </button>
                </>
              )}
            </p>
          </form>
        </div>
      </PageShell>
    </Layout>
  );
}

export default LoginPage;