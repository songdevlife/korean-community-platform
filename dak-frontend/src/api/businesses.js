import apiClient from './client';

export async function fetchBusinesses(params = {}) {
  const response = await apiClient.get('/businesses', { params });
  return response.data.data; // ApiResponse의 { success, data } 중 data만 꺼냄
}

export async function fetchBusinessCategories() {
  const response = await apiClient.get('/business-categories');
  return response.data.data;
}