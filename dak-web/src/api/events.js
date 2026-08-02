'use client';

import apiClient from './client';

export async function fetchEventCategories() {
  const response = await apiClient.get('/events/categories');
  return response.data.data;
}