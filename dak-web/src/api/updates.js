'use client';

import apiClient from './client';

/**
 * Browser-side update calls. Public reads on server-rendered pages go through
 * api/server.js instead — this exists for client components that fetch after
 * mount, such as the category dropdown in the admin review queue.
 */
export async function fetchUpdateCategories() {
  const response = await apiClient.get('/update-categories');
  return response.data.data;
}