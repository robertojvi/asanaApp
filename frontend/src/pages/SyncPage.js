import { useEffect, useRef, useState } from 'react';
import client from '../api/client';

export default function SyncPage() {
  const [config, setConfig] = useState(null);
  const [cron, setCron] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [status, setStatus] = useState('');
  const [progress, setProgress] = useState(null);
  const pollRef = useRef(null);

  function loadConfig() {
    client.get('/sync/config').then((r) => {
      setConfig(r.data);
      setCron(r.data.cronExpression);
      setEnabled(r.data.enabled);
    });
  }

  function stopPolling() {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }

  function pollProgress() {
    client.get('/sync/progress').then((r) => {
      setProgress(r.data);
      if (!r.data.running) {
        stopPolling();
        setStatus(`Done: ${r.data.completed - r.data.failed} succeeded, ${r.data.failed} failed`);
        loadConfig();
      }
    });
  }

  useEffect(() => {
    loadConfig();
    // in case a sync was already running before this page loaded (e.g. a refresh mid-sync)
    client.get('/sync/progress').then((r) => {
      setProgress(r.data);
      if (r.data.running) pollRef.current = setInterval(pollProgress, 1000);
    });
    return stopPolling;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function saveSchedule() {
    setStatus('Saving...');
    await client.put('/sync/config', { cronExpression: cron, enabled });
    setStatus('Schedule updated');
    loadConfig();
  }

  async function runNow() {
    setStatus('');
    setProgress({ running: true, total: 0, completed: 0, failed: 0 });
    try {
      await client.post('/sync/run-now');
      pollRef.current = setInterval(pollProgress, 1000);
    } catch (err) {
      if (err.response?.status === 409) {
        setStatus('A sync is already running');
        pollRef.current = setInterval(pollProgress, 1000);
      } else {
        setProgress(null);
        setStatus('Failed: ' + (err.response?.data?.error || err.message));
      }
    }
  }

  const pct = progress && progress.total > 0 ? Math.round((progress.completed / progress.total) * 100) : 0;

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
      <button onClick={runNow} disabled={!!progress?.running}>Run sync now</button>

      {progress?.running && (
        <div className="progress-bar-wrap">
          <div className="progress-bar-track">
            <div className="progress-bar-fill" style={{ width: `${pct}%` }} />
          </div>
          <div className="hint-text">
            {progress.total > 0
              ? `${progress.completed} / ${progress.total} projects (${pct}%)${progress.failed ? `, ${progress.failed} failed` : ''}`
              : 'Starting...'}
          </div>
        </div>
      )}

      {status && <p className="status-text">{status}</p>}
    </div>
  );
}
