// Login helper. Use from setup() and reuse the token across all VUs.

import http from 'k6/http';
import { check } from 'k6';
import { BASE, IDENTIFIER, PASSWORD } from './env.js';

export function login() {
  const res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ identifier: IDENTIFIER, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { op: 'login' } }
  );
  const ok = check(res, { 'login 200': r => r.status === 200 });
  if (!ok) throw new Error(`login failed: ${res.status} ${res.body}`);
  return res.json('accessToken');
}

export function authHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type':  'application/json',
  };
}
