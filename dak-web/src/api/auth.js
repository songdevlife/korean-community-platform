'use client';

import apiClient from './client';

export async function login(email, password) {
  const response = await apiClient.post('/auth/login', { email, password });
  return response.data.data;
}

export async function register(email, password, displayName) {
  const response = await apiClient.post('/auth/register', { email, password, displayName });
  return response.data.data;
}

/**
 * Revokes the session behind a refresh token.
 *
 * Clearing localStorage on its own only makes the browser forget the token; the
 * session stays valid on the server until it expires, so a token captured from
 * a shared machine still works after the person believes they have signed out.
 * The backend has carried a revoked flag and checked it since the auth API was
 * built — nothing was calling this.
 *
 * Returns 204 with no body.
 */
export async function logout(refreshToken) {
  await apiClient.post('/auth/logout', { refreshToken });
}

/**
 * Asks for a reset link.
 *
 * Returns 204 whatever happened - an unknown address, one that asked a moment
 * ago, and a real send are indistinguishable. The caller cannot tell whether
 * an email went out, which is the point: any other answer would make this a
 * way to test who has an account here.
 */
export async function requestPasswordReset(email) {
  await apiClient.post('/auth/forgot-password', { email });
}

/**
 * Spends a reset token and sets the new password.
 *
 * Unlike the request above, this reports failure plainly: someone holding a
 * link needs to know whether it has expired.
 */
export async function resetPassword(token, newPassword) {
  await apiClient.post('/auth/reset-password', { token, newPassword });
}

/**
 * Confirms an address from the link in a verification email.
 *
 * Open to anyone holding the token - the link is followed from an inbox, not
 * from a signed-in session, and often in a different browser from the one that
 * registered.
 */
export async function verifyEmail(token) {
  await apiClient.post('/auth/verify-email', { token });
}

/**
 * Sends another verification link to the signed-in user's address.
 *
 * Requires a session, unlike the two above: this is the "didn't get it" button
 * on the account page, so the address is whoever is signed in rather than
 * whatever was typed into a form.
 */
export async function resendVerification() {
  await apiClient.post('/auth/resend-verification');
}