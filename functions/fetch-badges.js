/**
 * Firestore badges 컬렉션 조회 → 마크다운 표 출력
 * 실행: cd functions && node fetch-badges.js [서비스계정경로]
 */
const admin = require('firebase-admin');
const path = require('path');

const PROJECT_ID = 'cole-c3f96';
const serviceAccountPath = process.argv[2];

async function main() {
  if (!admin.apps.length) {
    const options = { projectId: PROJECT_ID };
    const resolvedPath = serviceAccountPath ? path.resolve(process.cwd(), serviceAccountPath) : null;
    if (resolvedPath && require('fs').existsSync(resolvedPath)) {
      options.credential = admin.credential.cert(require(resolvedPath));
    }
    admin.initializeApp(options);
  }
  const db = admin.firestore();
  const snapshot = await db.collection('badges').orderBy('order').get();
  if (snapshot.empty) {
    console.log('badges 컬렉션이 비어 있습니다.');
    return;
  }
  const badges = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
  console.log('| id | title | description | message |');
  console.log('|:---|:------|:------------|:--------|');
  badges.forEach((b) => {
    const id = b.id || '';
    const title = (b.title || '').replace(/\|/g, '\\|');
    const desc = (b.description || '').replace(/\|/g, '\\|').replace(/\n/g, ' ');
    const msg = (b.message || '').replace(/\|/g, '\\|').replace(/\n/g, ' ');
    console.log(`| ${id} | ${title} | ${desc} | ${msg} |`);
  });
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
