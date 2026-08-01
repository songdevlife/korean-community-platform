'use client';

import apiClient from './client';

/**
 * Changes the signed-in user's display name.
 *
 * Returns the updated profile, which the caller writes back into the session
 * so the name changes everywhere it is shown without a reload.
 */
export async function updateProfile(displayName) {
  const response = await apiClient.patch('/users/me', { displayName });
  return response.data.data;
}