import { useEffect, useState } from 'react';
import client from '../api/client';

export default function ReportsPage() {
  const [byCorporate, setByCorporate] = useState([]);
  const [overdue, setOverdue] = useState([]);

  useEffect(() => {
    client.get('/reports/progress-by-corporate').then((r) => setByCorporate(r.data));
    client.get('/reports/overdue-tasks').then((r) => setOverdue(r.data));
  }, []);

  return (
    <div className="page">
      <h2 className="section-title">Progress by Corporate Brand</h2>
      <div className="table-wrap" style={{ marginBottom: 32 }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Corporate</th><th>Projects</th><th>Total tasks</th>
              <th>Completed</th><th>% Complete</th>
            </tr>
          </thead>
          <tbody>
            {byCorporate.map((row) => (
              <tr key={row.corporate}>
                <td>{row.corporate}</td>
                <td>{row.project_count}</td>
                <td>{row.total_tasks}</td>
                <td>{row.completed_tasks}</td>
                <td>{row.pct_complete}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 className="section-title">Overdue Tasks ({overdue.length})</h2>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Task</th><th>Project</th><th>Assignee</th><th>Due</th>
            </tr>
          </thead>
          <tbody>
            {overdue.slice(0, 100).map((row) => (
              <tr key={row.gid}>
                <td>{row.name}</td>
                <td>{row.project}</td>
                <td>{row.assignee}</td>
                <td>{row.due_on}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
