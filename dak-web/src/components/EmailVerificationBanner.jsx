'use client';

import { useState } from 'react';
import { MailWarning } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { resendVerification } from '@/api/auth';

/**
 * Prompts an unverified user to confirm their address.
 *
 * Nothing is blocked by being unverified - an account that cannot be used is a
 * worse outcome than one whose address is unconfirmed. But an unreachable
 * address is a locked account the moment its password is forgotten, since
 * reset is the only route back in, so the prompt is worth showing until it is
 * dealt with.
 */
export default function EmailVerificationBanner() {
  const { user } = useAuth();
  const [state, setState] = useState('idle');

  if (!user || user.emailVerified) return null;

  async function handleResend() {
    setState('sending');
    try {
      await resendVerification();
      setState('sent');
    } catch {
      setState('failed');
    }
  }

  return (
    <div className="rounded-2xl border border-border-dark bg-night p-4 mb-4 flex items-start gap-3">
      <span className="w-9 h-9 shrink-0 rounded-lg bg-surface border border-border-dark
                       flex items-center justify-center">
        <MailWarning size={16} strokeWidth={1.75} className="text-adelaide-red" />
      </span>

      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-snow">이메일 주소가 확인되지 않았습니다</p>

        {state === 'sent' ? (
          <p className="text-[13px] text-muted mt-0.5 leading-relaxed">
            확인 메일을 다시 보내드렸습니다. 메일이 보이지 않으면 스팸함도 확인해 주세요.
          </p>
        ) : (
          <>
            <p className="text-[13px] text-muted mt-0.5 leading-relaxed">
              비밀번호를 잊으셨을 때 이 주소로만 재설정이 가능합니다.
            </p>
            <button
              onClick={handleResend}
              disabled={state === 'sending'}
              className="text-[13px] text-korea-blue hover:underline mt-1.5
                         cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {state === 'sending' ? '보내는 중...' : '확인 메일 다시 받기'}
            </button>
            {state === 'failed' && (
              <p className="text-[12px] text-adelaide-red mt-1">
                보내지 못했습니다. 잠시 후 다시 시도해 주세요.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}