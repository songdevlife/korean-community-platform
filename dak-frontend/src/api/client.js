import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
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