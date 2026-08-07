import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function JiraProjectsPage() {
  const { canEdit } = useAuth();
  const [projects, setProjects] = useState([]);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(new Set());
  const [issues, setIssues] = useState([]);
  const [syncing, setSyncing] = useState('');

  function loadProjects() {
    client.get('/jira/projects').then((res) => setProjects(res.data));
  }

  useEffect(() => { loadProjects(); }, []);

  useEffect(() => {
    if (selected.size === 0) { setIssues([]); return; }
    client.get('/jira/issues', { params: { project_ids: Array.from(selected).join(',') } })
      .then((res) => setIssues(res.data));
  }, [selected]);

  function toggle(id) {
    const next = new Set(selected);
    next.has(id) ? next.delete(id) : next.add(id);
    setSelected(next);
  }

  async function syncNow() {
    setSyncing('Syncing...');
    try {
      const res = await client.post('/jira/sync');
      setSyncing(`Synced ${res.data.projectsSynced} projects, ${res.data.issuesSynced} issues`);
      loadProjects();
    } catch (err) {
      setSyncing('Failed: ' + (err.response?.data?.error || err.message));
    }
  }

  return (
    <div className="projects-layout">
      <div className="sidebar">
        {canEdit && (
          <div style={{ marginBottom: 12 }}>
            <button className="btn-primary btn-block" onClick={syncNow}>Sync Now</button>
            {syncing && <div className="status-text" style={{ marginTop: 6 }}>{syncing}</div>}
          </div>
        )}
        <input
          type="text"
          className="project-search"
          placeholder="Search projects..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ marginBottom: 8, width: '100%', boxSizing: 'border-box' }}
        />
        {projects
          .filter((p) => p.name.toLowerCase().includes(search.trim().toLowerCase()))
          .map((p) => (
          <div key={p.id} className="project-row" onClick={() => toggle(p.id)}>
            <input type="checkbox" checked={selected.has(p.id)} readOnly />
            <span className="project-name">{p.key} - {p.name}</span>
          </div>
        ))}
        {projects.length === 0 && (
          <p className="hint-text" style={{ marginTop: 12 }}>
            No Jira projects yet. {canEdit ? 'Click "Sync Now" to pull from Jira.' : 'Ask an admin to run a sync.'}
          </p>
        )}
      </div>
      <div className="content-pane">
        {selected.size === 0 ? (
          <p className="empty-state">Select one or more projects to view issues.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Key</th><th>Summary</th><th>Status</th><th>Type</th>
                  <th>Priority</th><th>Assignee</th><th>Due date</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((i) => (
                  <tr key={i.id}>
                    <td>{i.key}</td>
                    <td>{i.summary}</td>
                    <td>{i.statusName}</td>
                    <td>{i.issueType}</td>
                    <td>{i.priority}</td>
                    <td>{i.assigneeName || ''}</td>
                    <td>{i.dueDate || ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
