import axios from 'axios';

// Defaults to calling the backend on whatever host the page was loaded
// from (browser JS can't use "localhost" for this - that resolves to the
// viewer's own machine, not the server). This means it keeps working
// automatically if the server's LAN IP changes, with no rebuild needed.
// REACT_APP_API_URL still wins when explicitly set (e.g. a non-default
// backend port, or pointing the frontend at a different host entirely).
const defaultApiUrl = `http://${window.location.hostname}:8080/api`;

const client = axios.create({
  baseURL: process.env.REACT_APP_API_URL || defaultApiUrl,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default client;
