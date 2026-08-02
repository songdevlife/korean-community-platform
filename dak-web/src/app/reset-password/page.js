'use client';

import { Suspense, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { resetPassword } from '@/api/auth';
import PageShell from '@/components/PageShell';

/**
 * Sets a new password against a token from the emailed link.
 *
 * Unlike the request page, this reports failure plainly. The token is already
 * a secret held by whoever has the link, so saying it has expired reveals
 * nothing - and someone staring at a rejected form needs to know whether to
 * ask for a new link or retype their password.
 *
 * Nothing here signs anyone in. Completing a reset revokes every session the
 * account holds, including any this browser was carrying, so the only correct
 * destination afterwards is the login form.
 */
function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  const inputClass =
    'w-full px-[14px] py-[13px] rounded-[10px] border border-border-dark bg-night ' +
    'text-[16px] md:text-[15px] text-snow placeholder:text-faint outline-none ' +
    'hover:border-faint focus:border-faint transition-colors duration-300 [color-scheme:dark]';

  const labelClass = 'block text-[13px] font-semibold mb-2 text-snow';

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    // Checked here rather than only on the server: the two fields exist to
    // catch a typo, and a round trip to report one is a round trip wasted.
    if (password !== confirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    setBusy(true);

    try {
      await resetPassword(token, password);
      setDone(true);
    } catch (err) {
      const code = err.response?.data?.error?.code;
      setError(
        code === 'INVALID_RESET_TOKEN' || code === 'EXPIRED_RESET_TOKEN'
          ? '이 링크는 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요.'
          : err.response?.data?.error?.message ||
            '비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      );
      setBusy(false);
    }
  }

  // A link opened without a token, or with the query string stripped by a mail
  // client. Says so rather than showing a form that cannot succeed.
  if (!token) {
    return (
      <div className="rounded-[10px] border border-border-dark bg-night p-5 text-center">
        <p className="text-[14px] text-snow mb-2">유효하지 않은 링크입니다.</p>
        <p className="text-[13px] text-muted leading-relaxed">
          메일의 링크를 다시 확인하시거나<br />재설정을 다시 요청해 주세요.
        </p>
        <Link
          href="/forgot-password"
          className="inline-block mt-5 px-5 py-2.5 rounded-xl bg-korea-blue text-white
                     text-sm font-medium hover:bg-korea-blue/85 transition-colors"
        >
          재설정 다시 요청
        </Link>
      </div>
    );
  }

  if (done) {
    return (
      <div className="rounded-[10px] border border-border-dark bg-night p-5 text-center">
        <p className="text-[14px] text-snow mb-2">비밀번호가 변경되었습니다.</p>
        <p className="text-[13px] text-muted leading-relaxed">
          보안을 위해 기존에 로그인되어 있던<br />
          모든 기기에서 로그아웃되었습니다.
        </p>
        <button
          onClick={() => router.push('/login')}
          className="inline-block mt-5 px-5 py-2.5 rounded-xl bg-korea-blue text-white
                     text-sm font-medium hover:bg-korea-blue/85 transition-colors cursor-pointer"
        >
          로그인하기
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-[18px]">
        <label htmlFor="pw" className={labelClass}>새 비밀번호</label>
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
        <p className="text-faint text-[12px] mt-1.5">8자 이상 입력해 주세요.</p>
      </div>

      <div className="mb-[18px]">
        <label htmlFor="pw2" className={labelClass}>새 비밀번호 확인</label>
        <input
          id="pw2"
          type="password"
          placeholder="••••••••"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          autoComplete="new-password"
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
        {busy ? '변경하는 중...' : '비밀번호 변경'}
      </button>
    </form>
  );
}

export default function ResetPasswordPage() {
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
          <h1 className="text-xl font-bold text-snow">새 비밀번호 설정</h1>
        </div>

        {/* useSearchParams opts its subtree out of server rendering, so the
            boundary keeps that to the form rather than the page. */}
        <Suspense fallback={null}>
          <ResetPasswordForm />
        </Suspense>
      </div>
    </PageShell>
  );
}