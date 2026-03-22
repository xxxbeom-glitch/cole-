# 잘못 지급된 뱃지 정리 방법

뱃지 조건이 변경되면서 기존 조건으로 잘못 지급된 뱃지가 있을 수 있습니다.

## 1. Firestore badges 컬렉션 마스터 데이터 정리

**마스터 데이터만 업데이트하는 경우** (사용자별 획득 뱃지는 유지):

```bash
cd functions
npm run seed-badges -- ./cole-c3f96-firebase-adminsdk-fbsvc-3dd45b92b2.json
```

- `badges` 컬렉션의 문서(id, title, condition 등)가 `firestore_seed_badges.json`으로 **전체 교체**됩니다.
- `users/{userId}/badges/` 하위 문서(사용자별 획득 뱃지)는 **변경되지 않습니다**.

## 2. 잘못 지급된 사용자별 뱃지 삭제

### 방법 A: Firebase 콘솔에서 수동 삭제

1. Firebase Console → Firestore Database
2. `users` 컬렉션 → 대상 사용자 문서 선택
3. `badges` 서브컬렉션에서 잘못 지급된 뱃지 문서(badge_00x) 삭제

### 방법 B: Cloud Functions로 일괄 삭제

특정 뱃지 ID를 가진 모든 사용자의 획득 기록을 삭제하려면:

```javascript
// functions/scripts/remove-badges.js (예시)
const admin = require('firebase-admin');
admin.initializeApp({ projectId: 'cole-c3f96' });
const db = admin.firestore();

const BADGE_IDS_TO_REMOVE = ['badge_007', 'badge_008', 'badge_009', 'badge_014']; // 예: 이전 조건으로 지급된 뱃지

async function main() {
  const usersSnap = await db.collection('users').get();
  for (const userDoc of usersSnap.docs) {
    for (const bid of BADGE_IDS_TO_REMOVE) {
      await userDoc.ref.collection('badges').doc(bid).delete();
    }
    console.log(`Removed from ${userDoc.id}`);
  }
}
main();
```

### 방법 C: 특정 사용자만 정리

```javascript
const userId = 'USER_UID_HERE';
await db.collection('users').doc(userId).collection('badges').doc('badge_007').delete();
```

## 3. 조건 변경으로 인해 “추가 지급”이 필요한 경우

삭제만 하면, 사용자가 다시 조건을 달성했을 때 `BadgeAutoGrant`가 자동으로 재지급합니다.

- **badge_004~009**: 자정 리셋 시 `onMidnightReset`에서 누적일 재계산 후 조건 체크
- **badge_010~015**: 자정 리셋 시 어제의 밤 10시/9시 이후 사용량을 확인해 누적일 업데이트 후 조건 체크
- **badge_016~018**: 앱 기동·로그인 시 획득 뱃지 개수 기준으로 자동 체크

따라서 잘못된 뱃지를 삭제한 뒤, 앱을 다시 실행하거나 자정이 넘어가면 올바른 조건에 따라 다시 지급됩니다.

## 4. 로컬 통계 초기화 (선택)

`BadgeStatsPreferences` 데이터를 초기화하면 뱃지 관련 로컬 카운터가 0으로 돌아갑니다.
(디버그 메뉴에서 “뱃지 통계 초기화” 등이 있다면 사용)

---

*프로젝트: aptox | 최종 업데이트: 2025-03*
