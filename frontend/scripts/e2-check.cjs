// E2 verification: JSX identifier scope check across src/.
// Flags any <Component /> whose name is neither imported nor locally
// defined — the class of runtime error bundlers do not catch.
const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '..', 'src');

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
  const code = fs.readFileSync(file, 'utf8');
  filesChecked += 1;

  // Imported names: default, namespace, and named (incl. `as` aliases,
  // with multi-line brace lists handled by the negated-class match).
  const known = new Set();
  for (const m of code.matchAll(/import\s*\{([^}]*)\}\s*from/g)) {
    m[1].split(',').forEach((part) => {
      const name = part.trim().split(/\s+as\s+/).pop().trim();
      if (name) known.add(name);
    });
  }
  for (const m of code.matchAll(/import\s+([A-Za-z_$][\w$]*)\s*(?:,\s*\{[^}]*\})?\s*from/g)) known.add(m[1]);
  for (const m of code.matchAll(/import\s*\*\s*as\s*([A-Za-z_$][\w$]*)\s*from/g)) known.add(m[1]);

  // Local declarations.
  for (const m of code.matchAll(/(?:const|let|var|function|class)\s+([A-Za-z_$][\w$]*)/g)) known.add(m[1]);
  // Single-parameter arrow functions — HOC factory pattern: (Screen) => <Screen />.
  for (const m of code.matchAll(/\(\s*([A-Z][\w$]*)\s*\)\s*=>/g)) known.add(m[1]);

  // JSX component usages.
  for (const m of code.matchAll(/<([A-Z][\w.]*)[\s/>]/g)) {
    const base = m[1].split('.')[0];
    if (!known.has(base)) {
      problems.push(`${path.relative(srcDir, file)}: <${m[1]}> is not imported or defined`);
    }
  }
}

if (problems.length) {
  console.log(problems.join('\n'));
  process.exit(1);
}
console.log(`OK: ${filesChecked} files, all JSX identifiers imported/defined`);
