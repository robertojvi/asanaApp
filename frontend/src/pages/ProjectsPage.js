import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function ProjectsPage() {
  const { canEdit } = useAuth();
  const [projects, setProjects] = useState([]);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(new Set());
  const [tasks, setTasks] = useState([]);
  const [showNewProject, setShowNewProject] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [newName, setNewName] = useState('');
  const [newTemplate, setNewTemplate] = useState('');
  const [creating, setCreating] = useState('');

  useEffect(() => {
    client.get('/projects').then((res) => setProjects(res.data));
    if (canEdit) client.get('/templates').then((res) => setTemplates(res.data));
  }, [canEdit]);

  useEffect(() => {
    if (selected.size === 0) { setTasks([]); return; }
    client.get('/tasks', { params: { project_gids: Array.from(selected).join(',') } })
      .then((res) => setTasks(res.data));
  }, [selected]);

  function toggle(gid) {
    const next = new Set(selected);
    next.has(gid) ? next.delete(gid) : next.add(gid);
    setSelected(next);
  }

  async function saveDate(taskGid, body) {
    try {
      await client.patch(`/tasks/${taskGid}`, body);
      client.get('/tasks', { params: { project_gids: Array.from(selected).join(',') } })
        .then((res) => setTasks(res.data));
    } catch (err) {
      alert('Failed to save to Asana: ' + (err.response?.data?.error || err.message));
    }
  }

  async function createProject() {
    if (!newName.trim()) return;
    setCreating('Working...');
    try {
      const res = newTemplate
        ? await client.post('/projects/duplicate', { templateGid: newTemplate, name: newName })
        : await client.post('/projects', { name: newName });
      setCreating('Created project ' + res.data.projectGid);
      setNewName('');
      client.get('/projects').then((r) => setProjects(r.data));
    } catch (err) {
      setCreating('Failed: ' + (err.response?.data?.error || err.message));
    }
  }

  return (
    <div className="projects-layout">
      <div className="sidebar">
        {canEdit && (
          <div style={{ marginBottom: 12 }}>
            <button className="btn-primary btn-block" onClick={() => setShowNewProject(!showNewProject)}>+ New project</button>
            {showNewProject && (
              <div className="new-project-panel">
                <input placeholder="Project name" value={newName} onChange={(e) => setNewName(e.target.value)} />
                <select value={newTemplate} onChange={(e) => setNewTemplate(e.target.value)}>
                  <option value="">-- blank project --</option>
                  {templates.map((t) => <option key={t.gid} value={t.gid}>{t.name}</option>)}
                </select>
                <button className="btn-primary btn-block" onClick={createProject}>Create</button>
                {creating && <div className="status-text" style={{ marginTop: 6 }}>{creating}</div>}
              </div>
            )}
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
          .filter((p) => p.project.toLowerCase().includes(search.trim().toLowerCase()))
          .map((p) => (
          <div key={p.project_gid} className="project-row" onClick={() => toggle(p.project_gid)}>
            <input type="checkbox" checked={selected.has(p.project_gid)} readOnly />
            <span className="project-name">{p.project}</span>
            <span className="project-pct">{p.pct_complete ?? 0}%</span>
          </div>
        ))}
      </div>
      <div className="content-pane">
        {selected.size === 0 ? (
          <p className="empty-state">Select one or more projects to view tasks.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Done</th><th>Task</th><th>Project</th><th>Assignee</th>
                  <th>Due date</th><th>Expected Due Date</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((t) => (
                  <TaskRow key={t.task_gid} task={t} canEdit={canEdit} onSave={saveDate} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function TaskRow({ task, canEdit, onSave }) {
  const [dueOn, setDueOn] = useState(task.due_on ? task.due_on.slice(0, 10) : '');
  const [expDate, setExpDate] = useState((task.expected_due_date || '').slice(0, 10));
  const [completed, setCompleted] = useState(!!task.completed);

  return (
    <tr className={completed ? 'row-completed' : ''}>
      <td>
        <input type="checkbox" checked={completed} disabled={!canEdit}
               onChange={(e) => {
                 setCompleted(e.target.checked);
                 onSave(task.task_gid, { completed: e.target.checked });
               }} />
      </td>
      <td>{task.task_name}</td>
      <td>{task.project}</td>
      <td>{task.assignee || ''}</td>
      <td>
        <input type="date" value={dueOn} disabled={!canEdit}
               onChange={(e) => setDueOn(e.target.value)}
               onBlur={() => canEdit && onSave(task.task_gid, { dueOn: dueOn || null })} />
      </td>
      <td>
        {task.expected_due_date_field_gid ? (
          <input type="date" value={expDate} disabled={!canEdit}
                 onChange={(e) => setExpDate(e.target.value)}
                 onBlur={() => canEdit && onSave(task.task_gid, {
                   expectedDueDateFieldGid: task.expected_due_date_field_gid,
                   expectedDueDate: expDate || null,
                 })} />
        ) : <span style={{ color: '#bbb' }}>n/a</span>}
      </td>
    </tr>
  );
}
