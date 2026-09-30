// E9 P0 — same-origin packaging: copies the built SPA (dist/) into the
// backend's static resources so Spring Boot serves shell + /api from one
// origin (the PWA same-origin prerequisite; see SpaWebConfig).
// Usage: node scripts/deploy-to-backend.cjs   (run `pnpm build` first)
const fs = require('fs');
const path = require('path');

const dist = path.resolve(__dirname, '..', 'dist');
const target = path.resolve(__dirname, '..', '..', 'backend', 'src', 'main', 'resources', 'static');

if (!fs.existsSync(path.join(dist, 'index.html'))) {
  console.error('dist/index.html not found — run `pnpm build` first.');
  process.exit(1);
}

fs.rmSync(target, { recursive: true, force: true });
fs.cpSync(dist, target, { recursive: true });
console.log(`Copied ${dist} -> ${target}`);
console.log('Now rebuild/restart the backend (mvn spring-boot:run) and open http://localhost:8080');
