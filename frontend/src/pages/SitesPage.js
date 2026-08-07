import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

function siteLabel(s) {
  return s.venueName ? `${s.venueName} - ${s.subvenueName}` : (s.subvenueName || '');
}

export default function SitesPage() {
  const { canEdit } = useAuth();
  const [sites, setSites] = useState([]);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(null);
  const [locations, setLocations] = useState([]);
  const [devicesByLocation, setDevicesByLocation] = useState({});
  const [syncing, setSyncing] = useState('');

  function loadSites() {
    client.get('/sites').then((res) => setSites(res.data));
  }

  useEffect(() => { loadSites(); }, []);

  useEffect(() => {
    if (!selected) { setLocations([]); setDevicesByLocation({}); return; }
    client.get(`/sites/${selected.subvenueId}/locations`).then((res) => {
      setLocations(res.data);
      const locationIds = res.data.map((l) => l.id);
      if (locationIds.length === 0) { setDevicesByLocation({}); return; }
      client.get('/sites/devices', { params: { location_ids: locationIds.join(',') } }).then((devRes) => {
        const grouped = {};
        devRes.data.forEach((d) => {
          if (!grouped[d.locationId]) grouped[d.locationId] = [];
          grouped[d.locationId].push(d);
        });
        setDevicesByLocation(grouped);
      });
    });
  }, [selected]);

  async function syncNow() {
    setSyncing('Syncing... this pulls all 243 sites from the internal dashboard and can take a few minutes');
    try {
      const res = await client.post('/sites/sync');
      const { sitesSynced, locationsSynced, devicesSynced, errors } = res.data;
      const errorSuffix = errors && errors.length > 0 ? ` (${errors.length} error(s): ${errors.slice(0, 3).join('; ')})` : '';
      setSyncing(`Synced ${sitesSynced} sites, ${locationsSynced} locations, ${devicesSynced} devices${errorSuffix}`);
      loadSites();
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
          placeholder="Search sites..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ marginBottom: 8, width: '100%', boxSizing: 'border-box' }}
        />
        {sites
          .filter((s) => (s.subvenueName || '').toLowerCase().includes(search.trim().toLowerCase())
            || (s.venueName || '').toLowerCase().includes(search.trim().toLowerCase()))
          .slice()
          .sort((a, b) => siteLabel(a).localeCompare(siteLabel(b)))
          .map((s) => (
          <div
            key={s.subvenueId}
            className={`project-row${selected?.subvenueId === s.subvenueId ? ' selected' : ''}`}
            onClick={() => setSelected(s)}
          >
            <span className="project-name">{siteLabel(s)}</span>
            {s.asanaProjectGid && <span title="Has an Asana project" style={{ marginLeft: 6, color: '#8bc34a' }}>●</span>}
          </div>
        ))}
        {sites.length === 0 && (
          <p className="hint-text" style={{ marginTop: 12 }}>
            No sites yet. {canEdit ? 'Click "Sync Now" to pull from the site dashboard.' : 'Ask an admin to run a sync.'}
          </p>
        )}
      </div>
      <div className="content-pane">
        {!selected ? (
          <p className="empty-state">Select a site to view its details.</p>
        ) : (
          <div>
            <h2>{selected.venueName} {selected.subvenueName ? `- ${selected.subvenueName}` : ''}</h2>
            <table className="data-table" style={{ marginBottom: 24 }}>
              <tbody>
                <tr>
                  <td><b>Asana Project</b></td>
                  <td>
                    {selected.asanaProjectGid ? (
                      <a href={`https://app.asana.com/0/${selected.asanaProjectGid}`} target="_blank" rel="noreferrer">
                        {selected.asanaProjectGid}
                      </a>
                    ) : <span className="hint-text">No linked Asana project</span>}
                  </td>
                </tr>
                <tr><td><b>Address</b></td><td>{selected.address}</td></tr>
                <tr><td><b>Website</b></td><td>{selected.website}</td></tr>
                <tr><td><b>Front Desk Phone</b></td><td>{selected.frontDeskPhone}</td></tr>
                <tr><td><b>Operating Season</b></td><td>{selected.operatingSeason}</td></tr>
                <tr><td><b>Office Hours</b></td><td>{selected.officeHours}</td></tr>
                <tr><td><b>Managers</b></td><td>{selected.managers}</td></tr>
                <tr><td><b>ISP</b></td><td>{selected.isp}</td></tr>
                <tr><td><b>Electric Utility</b></td><td>{selected.electricUtility}</td></tr>
                <tr><td><b>Launch Status</b></td><td>{selected.launchStatus}</td></tr>
              </tbody>
            </table>

            <h3>Jira Location/Device List</h3>
            {locations.length === 0 ? (
              <p className="empty-state">No locations recorded for this site.</p>
            ) : (
              locations.map((loc) => (
                <div key={loc.id} style={{ marginBottom: 20 }}>
                  <h4>{loc.name} <span style={{ fontWeight: 'normal', color: '#888' }}>({loc.type}{loc.latitude ? ` — ${loc.latitude}, ${loc.longitude}` : ''})</span></h4>
                  {loc.notes && <p className="hint-text">{loc.notes}</p>}
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Status</th><th>Device</th><th>Manufacturer</th><th>Model</th>
                          <th>Mgmt IP</th><th>MAC</th><th>Serial</th><th>Powered By</th>
                          <th>Connects To</th><th>Notes</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(devicesByLocation[loc.id] || []).map((d) => (
                          <tr key={d.id}>
                            <td>{d.status}</td>
                            <td>{d.deviceName}</td>
                            <td>{d.manufacturer}</td>
                            <td>{d.model}</td>
                            <td>{d.managementIp}</td>
                            <td>{d.managementMac}</td>
                            <td>{d.serialNumber}</td>
                            <td>{d.poweredBy}</td>
                            <td>{d.connectsTo}</td>
                            <td>{d.notes}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
