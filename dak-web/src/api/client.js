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

/**
 * Refreshes a spent access token and replays the request that found it spent.
 *
 * The backend has issued refresh tokens since the authentication work, and
 * nothing was spending them: an access token lasts thirty minutes, a guide
 * takes longer than that to write, and the first save afterwards returned 401
 * with the text still only in the browser. This is the missing half.
 *
 * One refresh at a time. A page that fires four requests at once would
 * otherwise start four refreshes, three of which present a token the first has
 * already rotated away — and a rotated refresh token is a signed-out session.
 * The first caller does the work; the rest wait on the same promise.
 */
let refreshing = null;

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('dak_refresh_token');
  if (!refreshToken) return null;

  // A bare axios call, not apiClient: the interceptor below would catch a
  // failed refresh and try to refresh it, which is a loop.
  const { data } = await axios.post(
    `${apiClient.defaults.baseURL}/auth/refresh`,
    { refreshToken }
  );

  const tokens = data?.data ?? data;
  localStorage.setItem('dak_access_token', tokens.accessToken);
  if (tokens.refreshToken) {
    localStorage.setItem('dak_refresh_token', tokens.refreshToken);
  }
  return tokens.accessToken;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;

    // 403 as well as 401. Spring's filter chain does not authenticate an
    // expired token and then refuses the request at the authorisation step,
    // which is a 403 — so the status that actually arrives when a token runs
    // out is the same one an ordinary user gets for reaching an admin route.
    // The two are indistinguishable from here.
    //
    // Retried once only, which is what makes the conflation safe: a refusal
    // that survives a fresh token is a real refusal, and the second failure
    // reaches the caller as it did before.
    const status = error.response?.status;
    if ((status !== 401 && status !== 403) || original?._retried) {
      return Promise.reject(error);
    }
    original._retried = true;

    try {
      refreshing = refreshing ?? refreshAccessToken().finally(() => { refreshing = null; });
      const token = await refreshing;
      if (!token) return Promise.reject(error);

      original.headers.Authorization = `Bearer ${token}`;
      return apiClient(original);
    } catch {
      // The refresh token is spent or revoked, which is a real sign-out. Clear
      // both so the next page load knows it, and let the original failure
      // through rather than redirecting from here — a form with unsaved work
      // in it should not be navigated away from by an interceptor.
      localStorage.removeItem('dak_access_token');
      localStorage.removeItem('dak_refresh_token');
      return Promise.reject(error);
    }
  }
);

export default apiClient;