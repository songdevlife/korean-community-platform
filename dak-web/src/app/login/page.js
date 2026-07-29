'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { login, register } from '@/api/auth';
import { useAuth } from '@/context/AuthContext';
import PageShell from '@/components/PageShell';

// Mirrors app.registration.enabled on the backend, which is what actually
// refuses. This only decides whether to offer a form that would be rejected.
// Both have to change together when registration opens.
const REGISTRATION_OPEN =
  process.env.NEXT_PUBLIC_REGISTRATION_OPEN !== 'false';

/**
 * Login and sign-up in one page, toggled by a mode switch.

/**
 * Login and sign-up in one page, toggled by a mode switch.
 *
 * Entirely a client component, unlike the public pages. There is nothing here
 * for a crawler — the form is state from top to bottom, and the page is
 * deliberately kept out of the index. Splitting a server shell around it would
 * add a file to gain nothing.
 */
export default function LoginPage() {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  // Field-level messages keyed by field name. The API already returns these in
  // error.details; showing only the summary told someone their form was wrong
  // without saying which part.
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const { saveSession } = useAuth();
  const router = useRouter();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      const authResponse =
        mode === 'login'
          ? await login(email, password)
          : await register(email, password, displayName);
      saveSession(authResponse);
      router.push('/');
    } catch (err) {
      const apiError = err.response?.data?.error;
      const details = apiError?.details ?? [];

      if (details.length > 0) {
        // Each field carries its own message, so the summary above the button
        // would only repeat what is already beside the input.
        setFieldErrors(
          Object.fromEntries(details.map((d) => [d.field, d.message]))
        );
        setError('');
      } else {
        setError(apiError?.message || 'Something went wrong.');
      }
      setBusy(false);
    }
  }

  // 16px on mobile prevents iOS Safari from auto-zooming on focus.
  // Border lightens on hover as well as focus, matching the card treatment
  // used elsewhere — but the field does not lift, since it is somewhere to
  // type rather than somewhere to travel to.
  const inputClass =
    'w-full px-[14px] py-[13px] rounded-[10px] border border-border-dark bg-night ' +
    'text-[16px] md:text-[15px] text-snow placeholder:text-faint outline-none ' +
    'hover:border-faint focus:border-faint transition-colors duration-300 [color-scheme:dark]';

  const labelClass = 'block text-[13px] font-semibold mb-2 text-snow';

  const tabClass = (active) =>
    `py-3 text-[15px] cursor-pointer transition-colors duration-200 ${
      active
        ? 'bg-night text-snow font-semibold'
        : 'bg-transparent text-muted font-normal'
    }`;

  return (
    <PageShell>
      {/* Sits inside the standard shell rather than taking over the screen:
          logging in is a step within a browsing session, not a gateway to
          the site, and keeping the nav visible preserves that context. */}
      <div className="w-full max-w-[420px] mx-auto py-6 md:py-10">

        {/* Reduced from the full-screen version — the sidebar already carries
            the brand, so this only needs to identify the page. */}
        <div className="text-center mb-6">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/logo-mark-dark.png"
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

{/* Mode switch. Hidden entirely while registration is closed — a single
            disabled tab is a control that explains nothing. */}
        {REGISTRATION_OPEN && (
          <div className="grid grid-cols-2 border border-border-dark rounded-[10px] overflow-hidden mb-6">
            <button type="button" onClick={() => setMode('login')} className={tabClass(mode === 'login')}>
              Log in
            </button>
            <button type="button" onClick={() => setMode('signup')} className={tabClass(mode === 'signup')}>
              Sign up
            </button>
          </div>
        )}

        {!REGISTRATION_OPEN && (
          <p className="text-[13px] text-faint text-center mb-6">
            New accounts are not open yet.
          </p>
        )}

        <form onSubmit={handleSubmit}>
          {mode === 'signup' && (
            <div className="mb-[18px]">
              <label htmlFor="name" className={labelClass}>Display name</label>
              <input
                id="name"
                type="text"
                placeholder="Your name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                autoComplete="name"
                className={inputClass}
                required
              />
              {fieldErrors.displayName && (
                <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.displayName}</p>
              )}
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
            {fieldErrors.email && (
              <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.email}</p>
            )}
          </div>

          <div className="mb-[18px]">
            <label htmlFor="pw" className={labelClass}>Password</label>
            {/* current-password when logging in so a password manager offers the
                saved one; new-password on sign-up so it offers to generate
                instead. */}
            <input
              id="pw"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              className={inputClass}
              required
            />
            {fieldErrors.password && (
              <p className="text-adelaide-red text-[12px] mt-1.5">{fieldErrors.password}</p>
            )}
            {/* Stated up front rather than only after a rejected attempt. */}
            {mode === 'signup' && !fieldErrors.password && (
              <p className="text-faint text-[12px] mt-1.5">At least 8 characters.</p>
            )}
          </div>

          {/* Password reset needs email delivery, which does not exist yet.
              Stated plainly rather than shown as an inert "Forgot password?",
              which tells someone who cannot get in nothing at all. Restore the
              link here when the reset endpoint and mail delivery land. */}
          {mode === 'login' && (
            <p className="text-right text-[13px] text-faint -mt-[6px] mb-5">
              Password reset is coming soon.
            </p>
          )}

          {error && <p className="text-adelaide-red text-[13px] mb-3">{error}</p>}

          {/* White fill as before, with the card interaction kept: the surface
              lifts and a gradient hairline fades in along the top edge. */}
          <button
            type="submit"
            disabled={busy}
            className="group relative w-full overflow-hidden py-[14px] rounded-[10px]
                       bg-snow text-night text-[15px] cursor-pointer
                       hover:bg-white hover:-translate-y-1 transition-all duration-300
                       disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:translate-y-0"
          >
            <span
              aria-hidden="true"
              className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-0
                         bg-gradient-to-r from-transparent via-korea-blue to-transparent
                         group-hover:opacity-100 transition-opacity duration-300"
            />
            {busy
              ? 'Please wait…'
              : mode === 'login'
                ? 'Log in'
                : 'Create account'}
          </button>

          <p className={`text-center text-[13px] text-muted mt-[22px] ${REGISTRATION_OPEN ? '' : 'hidden'}`}>
          {mode === 'login' ? (
              <>
                Don&apos;t have an account?{' '}
                <button
                  type="button"
                  onClick={() => setMode('signup')}
                  className="text-[13px] text-snow font-semibold hover:underline cursor-pointer"
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
                  className="text-[13px] text-snow font-semibold hover:underline cursor-pointer"
                >
                  Log in
                </button>
              </>
            )}
          </p>
        </form>
      </div>
    </PageShell>
  );
}