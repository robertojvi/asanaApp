import { useEffect, useState } from 'react';
import client from '../api/client';

export default function SyncPage() {
  const [config, setConfig] = useState(null);
  const [cron, setCron] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [status, setStatus] = useState('');

  function loadConfig() {
    client.get('/sync/config').then((r) => {
      setConfig(r.data);
      setCron(r.data.cronExpression);
      setEnabled(r.data.enabled);
    });
  }

  useEffect(loadConfig, []);

  async function saveSchedule() {
    setStatus('Saving...');
    await client.put('/sync/config', { cronExpression: cron, enabled });
    setStatus('Schedule updated');
    loadConfig();
  }

  async function runNow() {
    setStatus('Running sync now - this may take a while for 200 projects...');
    try {
      const res = await client.post('/sync/run-now');
      setStatus(`Done: ${res.data.succeeded} succeeded, ${res.data.failed} failed`);
      loadConfig();
    } catch (err) {
      setStatus('Failed: ' + (err.response?.data?.error || err.message));
    }
  }

  return (
    <div className="page page-narrow">
      <h2 className="section-title">Sync Settings</h2>
      {config && (
        <p className="meta-text">
          Last run: {config.lastRunAt || 'never'}<br />
          Status: {config.lastRunStatus || 'n/a'}
        </p>
      )}

      <label className="form-group">
        Cron expression (Spring format: sec min hour day month weekday)
        <input value={cron} onChange={(e) => setCron(e.target.value)} />
        <div className="hint-text">Default: 0 0 3 * * * (3am daily)</div>
      </label>

      <label className="checkbox-row">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} /> Scheduled sync enabled
      </label>

      <button className="btn-primary" onClick={saveSchedule} style={{ marginRight: 10 }}>Save schedule</button>
      <button onClick={runNow}>Run sync now</button>

      {status && <p className="status-text">{status}</p>}
    </div>
  );
}
