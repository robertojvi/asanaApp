import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Wrap a page with this to require login, and optionally require the user
// to pass a specific check (e.g. requireEdit, requireSuperUser).
export default function ProtectedRoute({ children, require }) {
  const auth = useAuth();

  if (!auth.user) return <Navigate to="/login" replace />;
  if (require && !require(auth)) return <Navigate to="/" replace />;

  return children;
}
