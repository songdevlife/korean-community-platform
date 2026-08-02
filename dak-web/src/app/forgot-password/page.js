'use client';

import { useState } from 'react';
import Link from 'next/link';
import { requestPasswordReset } from '@/api/auth';
import PageShell from '@/components/PageShell';

/**
 * Asks for the address a reset link should go to.
 *
 * The confirmation is deliberately vague about whether anything was sent. The
 * backend answers identically for a registered address, an unregistered one,
 * and a repeat inside the cooldown, and saying more here would undo that -
 * "no account with that address" is a working account checker.
 */
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);

    try {
      await requestPasswordReset(email);
      setSent(true);
    } catch (err) {
      // Only reachable through a malformed address or an unreachable server;
      // the endpoint itself does not fail on unknown accounts.
      setError(
        err.response?.data?.error?.message ||
          '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      );
    } finally {
      setBusy(false);
    }
  }

  const inputClass =
    'w-full px-[14px] py-[13px] rounded-[10px] border border-border-dark bg-night ' +
    'text-[16px] md:text-[15px] text-snow placeholder:text-faint outline-none ' +
    'hover:border-faint focus:border-faint transition-colors duration-300 [color-scheme:dark]';

  const labelClass = 'block text-[13px] font-semibold mb-2 text-snow';

  return (
    <PageShell>
      <div className="w-full max-w-[420px] mx-auto py-6 md:py-10">

        <div className="text-center mb-6">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/logo-mark-dark.png"
            alt=""
            aria-hidden="true"
            className="w-12 h-12 mx-auto mb-2.5 object-contain opacity-60"
          />
          <h1 className="text-xl font-bold text-snow">비밀번호 재설정</h1>
          <p className="text-[13px] text-muted mt-1">
            {sent
              ? '메일을 확인해 주세요.'
              : '가입하신 이메일 주소를 입력해 주세요.'}
          </p>
        </div>

        {sent ? (
          <div className="rounded-[10px] border border-border-dark bg-night p-5 text-center">
            <p className="text-[14px] text-snow leading-relaxed mb-2">
              해당 주소로 가입된 계정이 있다면<br />
              재설정 링크를 보내드렸습니다.
            </p>
            <p className="text-[13px] text-muted leading-relaxed">
              링크는 30분간 유효합니다.<br />
              메일이 보이지 않으면 스팸함도 확인해 주세요.
            </p>
            <Link
              href="/login"
              className="inline-block mt-5 px-5 py-2.5 rounded-xl bg-korea-blue text-white
                         text-sm font-medium hover:bg-korea-blue/85 transition-colors"
            >
              로그인으로 돌아가기
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="mb-[18px]">
              <label htmlFor="email" className={labelClass}>이메일</label>
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

            {error && <p className="text-adelaide-red text-[13px] mb-3">{error}</p>}

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
              {busy ? '보내는 중...' : '재설정 링크 받기'}
            </button>

            <p className="text-center text-[13px] text-muted mt-[22px]">
              비밀번호가 기억나셨나요?{' '}
              <Link href="/login" className="text-snow font-semibold hover:underline">
                로그인
              </Link>
            </p>
          </form>
        )}
      </div>
    </PageShell>
  );
}