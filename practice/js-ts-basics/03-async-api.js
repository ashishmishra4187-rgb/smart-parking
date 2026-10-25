const BASE = "http://localhost:8080";

async function health() {
  const res = await fetch(`${BASE}/api/health`);
  console.log("health:", res.status, await res.json());
}

async function login() {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: process.env.PARKING_EMAIL,
      password: process.env.PARKING_PASSWORD,
    }),
  });
  if (!res.ok) throw new Error(`Login failed with status ${res.status}`);
  const data = await res.json();
  return data.token; // never print the token
}

async function recommendations(token) {
  const url = `${BASE}/api/parking/lots/2/recommendations?vehicleId=${process.env.PARKING_VEHICLE_ID}`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  console.log("recommendations status:", res.status);
  if (!res.ok) return;
  const list = await res.json();
  for (const r of list) {
    console.log(`${r.code} score ${r.score} - ${r.reasons[0]}`);
  }
}

async function main() {
  await health();
  const token = await login();
  await recommendations(token);
}

main().catch((err) => console.error("Failed:", err.message));