'use client';

import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

/**
 * Session state, held in localStorage and mirrored into React state.
 *
 * The previous build read localStorage inside useState's initialiser. That runs
 * during render, which on the server means reading a global that does not exist
 * — the render throws before anything reaches the browser.
 *
 * Reading in an effect instead solves that and a second, quieter problem: the
 * server has no way to know who is signed in, so its HTML is always the
 * signed-out version. If the first client render disagreed, React would discard
 * the server markup and warn about a hydration mismatch. Starting signed-out and
 * correcting immediately afterwards keeps both renders identical.
 *
 * The visible cost is that account UI appears a moment after the page does.
 * `loading` is exposed so a consumer can hold back a redirect until the session
 * is actually known, rather than bouncing a signed-in user to the login page.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('dak_user');
      if (stored) setUser(JSON.parse(stored));
    } catch (err) {
      // Corrupted JSON would otherwise break every page that reads the session.
      // Clearing it costs one re-login and leaves the app usable.
      console.error('Stored session was unreadable, clearing it:', err);
      localStorage.removeItem('dak_user');
      localStorage.removeItem('dak_access_token');
      localStorage.removeItem('dak_refresh_token');
    } finally {
      setLoading(false);
    }
  }, []);

  function saveSession(authResponse) {
    localStorage.setItem('dak_access_token', authResponse.accessToken);
    localStorage.setItem('dak_refresh_token', authResponse.refreshToken);
    localStorage.setItem('dak_user', JSON.stringify(authResponse.user));
    setUser(authResponse.user);
  }

  function clearSession() {
    localStorage.removeItem('dak_access_token');
    localStorage.removeItem('dak_refresh_token');
    localStorage.removeItem('dak_user');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, saveSession, clearSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}