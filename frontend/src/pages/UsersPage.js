import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState({ email: '', password: '', fullName: '', role: 'USER' });
  const [status, setStatus] = useState('');
  const [passwordEditId, setPasswordEditId] = useState(null);
  const [passwordForm, setPasswordForm] = useState({ newPassword: '', confirmPassword: '' });
  const [passwordStatus, setPasswordStatus] = useState('');

  function load() {
    client.get('/users').then((r) => setUsers(r.data));
  }
  useEffect(load, []);

  async function createUser(e) {
    e.preventDefault();
    setStatus('');
    try {
      await client.post('/users', form);
      setForm({ email: '', password: '', fullName: '', role: 'USER' });
      setStatus('User created');
      load();
    } catch (err) {
      setStatus('Failed: ' + (err.response?.data || err.message));
    }
  }

  async function toggleEnabled(user) {
    await client.put(`/users/${user.id}`, { enabled: !user.enabled });
    load();
  }

  async function changeRole(user, role) {
    await client.put(`/users/${user.id}`, { role });
    load();
  }

  function startPasswordEdit(user) {
    setPasswordEditId(user.id);
    setPasswordForm({ newPassword: '', confirmPassword: '' });
    setPasswordStatus('');
  }

  function cancelPasswordEdit() {
    setPasswordEditId(null);
    setPasswordForm({ newPassword: '', confirmPassword: '' });
    setPasswordStatus('');
  }

  async function savePassword(e, user) {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordStatus('Passwords do not match');
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordStatus('Password must be at least 8 characters');
      return;
    }
    try {
      await client.put(`/users/${user.id}`, { newPassword: passwordForm.newPassword });
      cancelPasswordEdit();
    } catch (err) {
      setPasswordStatus('Failed: ' + (err.response?.data || err.message));
    }
  }

  return (
    <div className="page">
      <h2 className="section-title">Users</h2>

      <form onSubmit={createUser} className="toolbar-form">
        <div className="field">
          <label>Email</label>
          <input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </div>
        <div className="field">
          <label>Password</label>
          <input required type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </div>
        <div className="field">
          <label>Full name</label>
          <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </div>
        <div className="field">
          <label>Role</label>
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="USER">User</option>
            <option value="ADMIN">Admin</option>
            <option value="SUPER_USER">Super User</option>
          </select>
        </div>
        <button type="submit" className="btn-primary">Create user</button>
      </form>
      {status && <p className="status-text">{status}</p>}

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Email</th><th>Name</th><th>Role</th><th>Enabled</th><th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isSelf = currentUser?.email?.toLowerCase() === u.email?.toLowerCase();
              return (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.fullName}</td>
                  <td>
                    <select value={u.role} onChange={(e) => changeRole(u, e.target.value)}>
                      <option value="USER">User</option>
                      <option value="ADMIN">Admin</option>
                      <option value="SUPER_USER">Super User</option>
                    </select>
                  </td>
                  <td>{u.enabled ? 'Yes' : 'No'}</td>
                  <td>
                    <button className="btn-sm" onClick={() => toggleEnabled(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>
                    {passwordEditId !== u.id && (
                      <button className="btn-sm" style={{ marginLeft: 8 }} onClick={() => startPasswordEdit(u)}>
                        {isSelf ? 'Change Password' : 'Reset Password'}
                      </button>
                    )}
                    {passwordEditId === u.id && (
                      <form onSubmit={(e) => savePassword(e, u)} className="inline-form">
                        <input
                          required
                          type="password"
                          placeholder="New password"
                          value={passwordForm.newPassword}
                          onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                          style={{ width: 120 }}
                        />
                        <input
                          required
                          type="password"
                          placeholder="Confirm"
                          value={passwordForm.confirmPassword}
                          onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                          style={{ width: 100 }}
                        />
                        <button type="submit" className="btn-sm btn-primary">Save</button>
                        <button type="button" className="btn-sm" onClick={cancelPasswordEdit}>Cancel</button>
                      </form>
                    )}
                    {passwordEditId === u.id && passwordStatus && (
                      <div className="error-text">{passwordStatus}</div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
