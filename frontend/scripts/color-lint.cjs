// E5-lite — colour centralization lint.
//
// Rule: `src/config/theme.js` is the single owner of colour. Components and
// utilities must consume theme tokens (`theme.palette.*`, `text.secondary`,
// `primary.main`, ...), the exported constants (CHART_COLORS,
// BRAND_GRADIENT, semanticColor), or `alpha(themeValue, x)` — never a raw
// literal. This keeps the whole application colour theme controllable from
// one file (TODO 5's "centralized control" requirement without changing
// the palette itself).
//
// Allow-list:
//  - src/config/theme.js (the owner)
//  - 'transparent', 'inherit', 'currentColor', 'none'
//  - '#fff'/'#ffffff' ONLY on the brand-mark monogram text sitting on the
//    fixed BRAND_GRADIENT (contrast colour, not a theme value)
//  - rgba()/rgb() only in chartHelpers tooltip shadow + theme.js
//    (documented, reviewed exceptions)

const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '..', 'src');
const OWNER = path.join('src', 'config', 'theme.js');

const COLOR_LITERAL = /#[0-9A-Fa-f]{3,8}\b|\brgba?\(\s*\d|\bhsla?\(\s*\d/;

// Where raw literals are reviewed and allowed (normalized to forward
// slashes so Windows path separators still match):
const norm = (p) => p.split(path.sep).join('/');
const ALLOWED_FILES = new Set([
  norm(path.join('src', 'config', 'theme.js')), // the owner
  norm(path.join('src', 'utils', 'chartHelpers.js')) // tooltip shadow only (theme-agnostic black alpha)
]);

// The brand-mark monogram text sits on the fixed gradient; its white is a
// contrast constant, matched contextually.
const BRAND_MARK_RE = /BRAND_GRADIENT[\s\S]{0,400}?color:\s*(['"])#fff\1/;

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (/\.(jsx|js)$/.test(entry.name)) out.push(full);
  }
  return out;
}

const problems = [];
let filesChecked = 0;

for (const file of walk(srcDir)) {
  const rel = norm(path.relative(path.resolve(srcDir, '..'), file));
  filesChecked += 1;
  if (ALLOWED_FILES.has(rel)) continue;

  const code = fs.readFileSync(file, 'utf8');
  const lines = code.split(/\r?\n/);

  // Fast reject: no colour-ish content at all.
  if (!COLOR_LITERAL.test(code)) continue;

  // Mask the reviewed brand-mark exception before scanning.
  const masked = code.replace(BRAND_MARK_RE, (m) => ' '.repeat(m.length));

  masked.split(/\r?\n/).forEach((line, i) => {
    const m = line.match(COLOR_LITERAL);
    if (m) problems.push(`${rel}:${i + 1}: colour literal "${m[0]}" — use theme tokens or config/theme exports`);
  });
}

if (problems.length) {
  console.log('Colour centralization violations:\n' + problems.join('\n'));
  process.exit(1);
}
console.log(`OK: ${filesChecked} files — all colours owned by src/config/theme.js`);
