import apiClient from './client';

export async function fetchPendingBusinesses() {
  const response = await apiClient.get('/admin/businesses', { params: { status: 'PENDING' } });
  return response.data.data;
}

export async function updateBusinessStatus(businessId, status) {
  const response = await apiClient.patch(`/admin/businesses/${businessId}/status`, { status });
  return response.data.data;
}

export async function fetchDraftUpdates() {
  const response = await apiClient.get('/admin/australia-updates', { params: { status: 'DRAFT' } });
  return response.data.data;
}

export async function updateUpdateStatus(updateId, status) {
  const response = await apiClient.patch(`/admin/australia-updates/${updateId}/status`, { status });
  return response.data.data;
}