/**
 * Firestore badges 컬렉션 시드 스크립트
 *
 * 실행: cd functions && npm run seed-badges
 *       또는: GOOGLE_APPLICATION_CREDENTIALS="경로/서비스계정.json" npm run seed-badges
 *
 * 사전 준비 (둘 중 하나):
 * 1. gcloud auth application-default login
 * 2. Firebase 콘솔 > 프로젝트 설정 > 서비스 계정 > 새 비공개 키 생성 후
 *    GOOGLE_APPLICATION_CREDENTIALS 환경변수에 해당 JSON 경로 지정
 */
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const PROJECT_ID = 'cole-c3f96';

// 인자로 서비스 계정 경로를 넘기면 사용 (예: npm run seed-badges -- ./service-account.json)
const serviceAccountPath = process.argv[2];

async function main() {
  if (!admin.apps.length) {
    const options = { projectId: PROJECT_ID };
    const resolvedPath = serviceAccountPath ? path.resolve(process.cwd(), serviceAccountPath) : null;
    if (resolvedPath && fs.existsSync(resolvedPath)) {
      options.credential = admin.credential.cert(require(resolvedPath));
    }
    // credential 없으면 GOOGLE_APPLICATION_CREDENTIALS 또는 gcloud application-default 사용
    admin.initializeApp(options);
  }
  const db = admin.firestore();

  const jsonPath = path.join(__dirname, '..', 'firestore_seed_badges.json');
  const raw = fs.readFileSync(jsonPath, 'utf8');
  const data = JSON.parse(raw);

  if (!data.badges) {
    console.error('firestore_seed_badges.json에 badges 키가 없습니다.');
    process.exit(1);
  }

  const badgesRef = db.collection('badges');

  // 1. 기존 badges 컬렉션 문서 전부 삭제
  const existingSnapshot = await badgesRef.get();
  if (!existingSnapshot.empty) {
    const deleteBatch = db.batch();
    existingSnapshot.docs.forEach((doc) => deleteBatch.delete(doc.ref));
    await deleteBatch.commit();
    console.log(`기존 ${existingSnapshot.size}개 문서 삭제 완료.\n`);
  }

  // 2. 새 데이터로 시드
  const batch = db.batch();
  let count = 0;

  for (const [docId, badge] of Object.entries(data.badges)) {
    const docRef = badgesRef.doc(docId);
    const docData = {
      id: badge.id,
      order: badge.order,
      title: badge.title,
      description: badge.description,
      condition: badge.condition,
      icon: badge.icon,
    };
    if (badge.message) docData.message = badge.message;
    batch.set(docRef, docData, { merge: true });
    count++;
    console.log(`  ${docId}: ${badge.title}`);
  }

  await batch.commit();
  console.log(`\n완료: badges 컬렉션에 ${count}개 문서 저장됨.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
