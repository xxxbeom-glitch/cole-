/**
 * 1024x1024 소스 PNG → Android Adaptive Icon 밀도별 리소스 생성
 * foreground: mdpi 108, hdpi 162, xhdpi 216, xxhdpi 324, xxxhdpi 432
 */
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const SOURCE = path.join(__dirname, '..', 'app-icon', 'icon_source_1024.png');

const SIZES = [
  { folder: 'drawable-mdpi', size: 108 },
  { folder: 'drawable-hdpi', size: 162 },
  { folder: 'drawable-xhdpi', size: 216 },
  { folder: 'drawable-xxhdpi', size: 324 },
  { folder: 'drawable-xxxhdpi', size: 432 },
];

const RES_ROOT = path.join(__dirname, '..', 'app', 'src', 'main', 'res');

async function main() {
  if (!fs.existsSync(SOURCE)) {
    console.error('Source icon not found:', SOURCE);
    process.exit(1);
  }

  const buf = await sharp(SOURCE)
    .resize(1024, 1024)
    .png()
    .toBuffer();

  for (const { folder, size } of SIZES) {
    const dir = path.join(RES_ROOT, folder);
    const outPath = path.join(dir, 'ic_launcher_foreground.png');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    await sharp(buf)
      .resize(size, size)
      .png()
      .toFile(outPath);
    console.log('Created:', outPath, `(${size}x${size})`);
  }
  console.log('Done.');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
