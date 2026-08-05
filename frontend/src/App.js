import { BrowserRouter, Routes, Route, NavLink, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import ProjectsPage from './pages/ProjectsPage';
import ReportsPage from './pages/ReportsPage';
import SyncPage from './pages/SyncPage';
import UsersPage from './pages/UsersPage';

function Nav() {
  const { user, logout, canEdit, canManageUsers } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <span className="navbar-brand-mark">AP</span>
        AccessParks Asana App
      </div>
      <div className="navbar-links">
        <NavLink to="/" end className="nav-link">Projects</NavLink>
        <NavLink to="/reports" className="nav-link">Reports</NavLink>
        {canEdit && <NavLink to="/sync" className="nav-link">Sync</NavLink>}
        {canManageUsers && <NavLink to="/users" className="nav-link">Users</NavLink>}
      </div>
      <div className="navbar-user">
        <span>{user.fullName || user.email}</span>
        <span className="role-badge">{user.role.replace('_', ' ')}</span>
        <button onClick={() => { logout(); navigate('/login'); }}>Log out</button>
      </div>
    </nav>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Nav />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><ProjectsPage /></ProtectedRoute>} />
          <Route path="/reports" element={<ProtectedRoute><ReportsPage /></ProtectedRoute>} />
          <Route path="/sync" element={
            <ProtectedRoute require={(auth) => auth.canEdit}><SyncPage /></ProtectedRoute>
          } />
          <Route path="/users" element={
            <ProtectedRoute require={(auth) => auth.canManageUsers}><UsersPage /></ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
