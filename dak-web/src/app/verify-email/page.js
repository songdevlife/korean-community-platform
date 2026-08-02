'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { verifyEmail } from '@/api/auth';
import PageShell from '@/components/PageShell';

/**
 * Confirms an address as soon as the page opens.
 *
 * No button: the person already acted by clicking the link in their email, and
 * asking them to click a second one would be asking twice for the same thing.
 */
function VerifyEmail() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const [state, setState] = useState('working');

  // Effects run twice in development under StrictMode, and the token is
  // single-use - the second call would fail against a token the first one
  // just spent, and report an expired link on a successful verification.
  const attempted = useRef(false);

  useEffect(() => {
    if (!token) {
      setState('missing');
      return;
    }

    if (attempted.current) return;
    attempted.current = true;

    verifyEmail(token)
      .then(() => setState('done'))
      .catch(() => setState('failed'));
  }, [token]);

  const card = 'rounded-[10px] border border-border-dark bg-night p-5 text-center';
  const button =
    'inline-block mt-5 px-5 py-2.5 rounded-xl bg-korea-blue text-white ' +
    'text-sm font-medium hover:bg-korea-blue/85 transition-colors';

  if (state === 'working') {
    return (
      <div className={card}>
        <p className="text-[14px] text-muted">확인하는 중입니다...</p>
      </div>
    );
  }

  if (state === 'done') {
    return (
      <div className={card}>
        <p className="text-[14px] text-snow mb-2">이메일 주소가 확인되었습니다.</p>
        <p className="text-[13px] text-muted leading-relaxed">
          비밀번호를 잊으셨을 때<br />이 주소로 재설정하실 수 있습니다.
        </p>
        <Link href="/" className={button}>홈으로</Link>
      </div>
    );
  }

  // A missing token and a spent one get the same screen. Both mean "this link
  // did not work, ask for another", and the account page is where that button
  // lives either way.
  return (
    <div className={card}>
      <p className="text-[14px] text-snow mb-2">
        {state === 'missing' ? '유효하지 않은 링크입니다.' : '확인하지 못했습니다.'}
      </p>
      <p className="text-[13px] text-muted leading-relaxed">
        링크가 만료되었거나 이미 사용되었을 수 있습니다.<br />
        마이페이지에서 확인 메일을 다시 받으실 수 있습니다.
      </p>
      <Link href="/dashboard" className={button}>마이페이지로</Link>
    </div>
  );
}

export default function VerifyEmailPage() {
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
          <h1 className="text-xl font-bold text-snow">이메일 주소 확인</h1>
        </div>

        <Suspense fallback={null}>
          <VerifyEmail />
        </Suspense>
      </div>
    </PageShell>
  );
}