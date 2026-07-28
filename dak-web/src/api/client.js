'use client';

import axios from 'axios';

/**
 * Browser-side API client. Carries the access token, so it is only usable where
 * a signed-in user exists — that means client components, never a server render.
 *
 * Public reads that need to reach a crawler go through api/server.js instead.
 */
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
});

// Runs before every request: attach the access token if we have one.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('dak_access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;