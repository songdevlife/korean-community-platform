'use client';

import apiClient from './client';

/**
 * Browser-side guide calls. Public reads on server-rendered pages go through
 * api/server.js instead — these exist for client components that fetch after
 * mount, such as the category list inside an admin edit form.
 */
export async function fetchGuides({ categoryId, keyword, page = 0, pageSize = 20 } = {}) {
  const params = { page, pageSize };
  if (categoryId) params.categoryId = categoryId;
  if (keyword) params.keyword = keyword;

  const response = await apiClient.get('/guides', { params });
  return response.data.data;
}

export async function fetchGuideBySlug(slug) {
  const response = await apiClient.get(`/guides/${slug}`);
  return response.data.data;
}

export async function fetchGuideCategories() {
  const response = await apiClient.get('/guide-categories');
  return response.data.data;
}