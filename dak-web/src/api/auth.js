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