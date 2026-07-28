import apiClient from './client';

/**
 * Directory listing and search share this endpoint — 05 API Spec 9.2 notes the
 * two may reuse the same logic, and their filters are identical.
 *
 * Empty strings are stripped rather than sent: an empty `category` would be
 * treated as a filter value by the backend rather than as "no filter".
 */
export async function fetchBusinesses(params = {}) {
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, value]) =>
      value !== undefined && value !== null && value !== ''
    )
  );

  const response = await apiClient.get('/businesses', { params: cleanParams });
  return response.data.data;
}

export async function fetchBusinessCategories() {
  const response = await apiClient.get('/business-categories');
  return response.data.data;
}

export async function createBusiness(payload) {
  const response = await apiClient.post('/businesses', payload);
  return response.data.data;
}