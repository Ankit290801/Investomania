// E9 P1 — generates PWA icons from the brand identity without any image
// dependencies: brand gradient (#0F766E → #4F46E5) rounded square with the
// white "IT" monogram (geometric letterforms, matching DashboardLayout's
// BrandMark). Outputs:
//   public/icons/icon-192.png / icon-512.png            (purpose: any)
//   public/icons/maskable-192.png / maskable-512.png    (purpose: maskable —
//     full-bleed background, art inside the 80% safe zone)
//   public/icons/apple-touch-icon.png (180x180, full-bleed per iOS)
//   public/favicon-32.png
// Regenerate with: node scripts/generate-icons.cjs
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// ---- minimal PNG encoder (RGBA, no interlace) ------------------------------
function crc32(buf) {
  let table = crc32.table;
  if (!table) {
    table = crc32.table = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c;
    }
  }
  let crc = -1;
  for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ table[(crc ^ buf[i]) & 0xff];
  return (crc ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

function encodePNG(width, height, rgba) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type RGBA
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0; // filter: none
    rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  }
  return Buffer.concat([
    sig,
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

// ---- drawing helpers --------------------------------------------------------
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const TEAL = hex('#0F766E');
const INDIGO = hex('#4F46E5');
const WHITE = [255, 255, 255, 255];

// Bilinear gradient across the tile (135deg ≈ diagonal mix of x/y).
function gradient(x, y, size) {
  const t = (x + y) / (2 * size - 2);
  return [
    Math.round(TEAL[0] + (INDIGO[0] - TEAL[0]) * t),
    Math.round(TEAL[1] + (INDIGO[1] - TEAL[1]) * t),
    Math.round(TEAL[2] + (INDIGO[2] - TEAL[2]) * t),
    255
  ];
}

function blend(dst, x, y, w, px) {
  const i = (y * w + x) * 4;
  const a = px[3] / 255;
  dst[i] = Math.round(px[0] * a + dst[i] * (1 - a));
  dst[i + 1] = Math.round(px[1] * a + dst[i + 1] * (1 - a));
  dst[i + 2] = Math.round(px[2] * a + dst[i + 2] * (1 - a));
  dst[i + 3] = Math.round(px[3] + dst[i + 3] * (1 - a));
}

function roundedSquareMask(x, y, size, radius) {
  // SDF-ish: distance to rounded-rect, antialiased on the 1px boundary.
  const cx = Math.min(Math.max(x, radius), size - radius);
  const cy = Math.min(Math.max(y, radius), size - radius);
  const inside = cx >= radius && cx < size - radius ? 0 : Math.max(Math.abs(cx - x), Math.abs(cy - y));
  const edge = size / 2 - Math.hypot(x - size / 2 + 0.5, y - size / 2 + 0.5) * 0; // noop, keep simple
  if (x < 0 || y < 0 || x >= size || y >= size) return 0;
  if (inside < radius - 0.5) return 1;
  // corner region check via circle distance
  return 1;
}

// Simpler, correct antialiased rounded rect: signed distance field.
function roundedRectAlpha(x, y, size, radius) {
  const half = size / 2;
  const dx = Math.abs(x + 0.5 - half) - (half - radius);
  const dy = Math.abs(y + 0.5 - half) - (half - radius);
  const ox = Math.max(dx, 0);
  const oy = Math.max(dy, 0);
  const dist = Math.hypot(ox, oy) + Math.min(Math.max(dx, dy), 0) - radius;
  return Math.min(1, Math.max(0, 0.5 - dist));
}

// Stroked rounded-rect band for letterform geometry (thickness t, centered
// on the rectangle outline).
function strokeBand(x, y, rx0, ry0, rx1, ry1, r, t) {
  // distance to rounded-rect outline at (x,y)
  const cx = Math.min(Math.max(x, rx0 + r), rx1 - r);
  const cy = Math.min(Math.max(y, ry0 + r), ry1 - r);
  const dx = x - cx, dy = y - cy;
  const outside = Math.hypot(dx, dy);
  const insideRect = x >= rx0 + r && x <= rx1 - r;
  const insideH = y >= ry0 + r && y <= ry1 - r;
  // signed distance to the outline
  const d = insideRect || insideH ? r - Math.max(Math.abs(x - (rx0 + rx1) / 2) - ((rx1 - rx0) / 2 - r), Math.abs(y - (ry0 + ry1) / 2) - ((ry1 - ry0) / 2 - r)) : outside - r;
  const sd = Math.abs(Math.max(dx, dy)) === 0 ? d : d;
  return Math.min(1, Math.max(0, t / 2 - Math.abs(sd)));
}

function drawMonogram(size, pad, thickness) {
  const canvas = new Float32Array(size * size);
  const I = {
    x0: size * pad, x1: size * pad + size * thickness,
    y0: size * (pad + 0.06), y1: size * (1 - pad)
  };
  const T = {
    x0: size * pad + size * (thickness + 0.13), x1: size * (1 - pad),
    y0: size * (pad + 0.06), y1: size * (pad + 0.06) + size * thickness
  };
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let a = 0;
      const px = x + 0.5, py = y + 0.5;
      const box = (b) => Math.min(1, Math.max(0, Math.min(px, b.x1) - Math.max(px, b.x0) + 1)) *
        Math.min(1, Math.max(0, Math.min(py, b.y1) - Math.max(py, b.y0) + 1));
      // I: vertical stem
      a = Math.max(a, box(I));
      // T: horizontal bar + centered stem
      a = Math.max(a, box(T));
      const stemW = size * thickness;
      const stemCx = (T.x0 + T.x1) / 2;
      a = Math.max(a, Math.min(1, Math.max(0, (stemW / 2 - Math.abs(px - stemCx)) + 0.5)) *
        Math.min(1, Math.max(0, Math.min(py, size * (1 - pad)) - Math.max(py, T.y0) + 1)));
      canvas[y * size + x] = a;
    }
  }
  return canvas;
}

function makeIcon(size, { maskable }) {
  const rgba = Buffer.alloc(size * size * 4);
  const radius = maskable ? 0 : Math.round(size * 0.22);
  // Art scale: maskable keeps content within the 80% safe zone.
  const artScale = maskable ? 0.72 : 1.0;
  const artSize = Math.round(size * artScale);
  const artOffset = Math.round((size - artSize) / 2);
  const mono = drawMonogram(artSize, maskable ? 0.24 : 0.22, 0.16);

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let px;
      if (maskable) {
        px = gradient(x, y, size); // full-bleed
      } else {
        const a = roundedRectAlpha(x, y, size, radius);
        px = a > 0 ? [...gradient(x, y, size).slice(0, 3), Math.round(a * 255)] : [0, 0, 0, 0];
      }
      blend(rgba, x, y, size, px);
      // monogram (only inside the art square)
      const ax = x - artOffset, ay = y - artOffset;
      if (ax >= 0 && ay >= 0 && ax < artSize && ay < artSize) {
        const m = mono[ay * artSize + ax];
        if (m > 0) blend(rgba, x, y, size, [...WHITE.slice(0, 3), Math.round(m * 255)]);
      }
    }
  }
  return encodePNG(size, size, rgba);
}

const outDir = path.resolve(__dirname, '..', 'public');
fs.mkdirSync(path.join(outDir, 'icons'), { recursive: true });

const jobs = [
  ['icons/icon-192.png', 192, { maskable: false }],
  ['icons/icon-512.png', 512, { maskable: false }],
  ['icons/maskable-192.png', 192, { maskable: true }],
  ['icons/maskable-512.png', 512, { maskable: true }],
  ['icons/apple-touch-icon.png', 180, { maskable: true }], // iOS: full-bleed
  ['favicon-32.png', 32, { maskable: false }]
];

for (const [rel, size, opts] of jobs) {
  fs.writeFileSync(path.join(outDir, rel), makeIcon(size, opts));
  console.log('wrote', rel);
}
console.log('done');
