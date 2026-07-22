import apiClient from './client';

export async function login(email, password) {
  const response = await apiClient.post('/auth/login', { email, password });
  return response.data.data;
}

export async function register(email, password, displayName) {
  const response = await apiClient.post('/auth/register', { email, password, displayName });
  return response.data.data;
}