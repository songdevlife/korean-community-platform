import apiClient from './client';

export async function fetchSavedItems(type) {
  const response = await apiClient.get('/users/me/saved-items', { params: { type } });
  return response.data.data;
}

export async function checkIsSaved(resourceType, resourceId) {
  const response = await apiClient.get('/users/me/saved-items/check', {
    params: { resourceType, resourceId },
  });
  return response.data.data;
}

export async function saveItem(resourceType, resourceId) {
  const response = await apiClient.post('/users/me/saved-items', { resourceType, resourceId });
  return response.data.data;
}

export async function removeSavedItem(savedItemId) {
  await apiClient.delete(`/users/me/saved-items/${savedItemId}`);
}