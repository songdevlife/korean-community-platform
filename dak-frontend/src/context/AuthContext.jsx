import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('dak_user');
    return stored ? JSON.parse(stored) : null;
  });

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
    <AuthContext.Provider value={{ user, saveSession, clearSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}