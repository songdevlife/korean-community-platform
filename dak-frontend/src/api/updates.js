import apiClient from './client';

export async function fetchUpdates(params = {}) {
  const response = await apiClient.get('/australia-updates', { params });
  return response.data.data;
}

export async function fetchUpdateById(id) {
  const response = await apiClient.get(`/australia-updates/${id}`);
  return response.data.data;
}

export async function fetchUpdateCategories() {
  const response = await apiClient.get('/update-categories');
  return response.data.data;
}