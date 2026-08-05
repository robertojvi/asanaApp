import { createContext, useContext, useState } from 'react';
import client from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  async function login(email, password) {
    const res = await client.post('/auth/login', { email, password });
    const { token, email: userEmail, role, fullName } = res.data;
    localStorage.setItem('token', token);
    const userData = { email: userEmail, role, fullName };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }

  const isSuperUser = user?.role === 'SUPER_USER';
  const isAdmin = user?.role === 'ADMIN';
  const canEdit = isSuperUser || isAdmin; // create/edit projects, run syncs
  const canManageUsers = isSuperUser;

  return (
    <AuthContext.Provider value={{ user, login, logout, canEdit, canManageUsers, isSuperUser, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
