const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { SolapiMessageService } = require("solapi");

admin.initializeApp();

const VERIFICATION_CODES_COLLECTION = "verificationCodes";
const CODE_EXPIRY_MINUTES = 5;

// Solapi API 키 (config 우선, 없으면 환경변수 또는 기본값)
// 배포 시: firebase functions:config:set solapi.api_key="..." solapi.api_secret="..." solapi.sender_phone="01012345678"
const getSolapiConfig = () => {
  const config = functions.config().solapi || {};
  return {
    apiKey: config.api_key || process.env.SOLAPI_API_KEY || "NCSNLECLXXK3XOPV",
    apiSecret: config.api_secret || process.env.SOLAPI_API_SECRET || "MZWLI4NG3ZIQH5GS7BRGKKNTFWUX1YYR",
    senderPhone: config.sender_phone || process.env.SOLAPI_SENDER_PHONE || "01000000000",
  };
};

/**
 * 휴대폰 번호 정규화 (숫자만 추출, 010 형식)
 */
function normalizePhone(phone) {
  const digits = String(phone).replace(/\D/g, "");
  if (digits.startsWith("82") && digits.length >= 11) {
    return "0" + digits.substring(2);
  }
  return digits.startsWith("0") ? digits : "0" + digits;
}

/**
 * 휴대폰 번호를 Firestore 문서 ID로 변환
 */
function phoneToDocId(phone) {
  return normalizePhone(phone).replace(/^0/, "zero_");
}

/**
 * 6자리 인증번호 생성
 */
function generateCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

/**
 * Solapi로 SMS 발송
 */
async function sendSms(to, text) {
  const { apiKey, apiSecret, senderPhone } = getSolapiConfig();
  const messageService = new SolapiMessageService(apiKey, apiSecret);
  const normalizedTo = normalizePhone(to);
  const from = normalizePhone(senderPhone);
  if (from.length < 10) {
    throw new Error("Solapi 발신번호가 설정되지 않았습니다. firebase functions:config:set solapi.sender_phone=01012345678");
  }
  await messageService.send({
    to: normalizedTo,
    from,
    text,
  });
}

/**
 * 회원가입용 인증번호 SMS 발송
 */
exports.sendSignUpVerificationSms = functions.https.onCall(async (data, context) => {
  const phone = data.phone;
  if (!phone || typeof phone !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "휴대폰 번호가 필요합니다.");
  }

  const normalizedPhone = normalizePhone(phone);
  if (normalizedPhone.length < 10) {
    throw new functions.https.HttpsError("invalid-argument", "올바른 휴대폰 번호를 입력해주세요.");
  }

  const db = admin.firestore();
  const code = generateCode();
  const docId = phoneToDocId(normalizedPhone);
  const expiresAt = new Date(Date.now() + CODE_EXPIRY_MINUTES * 60 * 1000);

  await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).set({
    phone: normalizedPhone,
    code,
    expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
    purpose: "signup",
  });

  await sendSms(normalizedPhone, `[Cole] 회원가입 인증번호: ${code}\n${CODE_EXPIRY_MINUTES}분 안에 입력해주세요.`);

  return { success: true };
});

/**
 * 회원가입 인증번호 검증 후 Firebase 계정 생성
 */
exports.verifyAndCompleteSignUp = functions.https.onCall(async (data, context) => {
  const { phone, code, email, password, name, birth } = data;
  if (!phone || !code || !email || !password) {
    throw new functions.https.HttpsError("invalid-argument", "필수 정보가 누락되었습니다.");
  }

  const normalizedPhone = normalizePhone(phone);
  const normalizedEmail = email.trim().toLowerCase();
  const db = admin.firestore();
  const docId = phoneToDocId(normalizedPhone);
  const codeDoc = await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).get();

  if (!codeDoc.exists) {
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 만료되었거나 올바르지 않습니다.");
  }

  const { code: storedCode, expiresAt, purpose } = codeDoc.data();
  if (purpose !== "signup" || storedCode !== code) {
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 올바르지 않습니다.");
  }

  const now = admin.firestore.Timestamp.now();
  if (expiresAt.toMillis() < now.toMillis()) {
    await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).delete();
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 만료되었습니다. 다시 발송해주세요.");
  }

  const userRecord = await admin.auth().createUser({
    email: normalizedEmail,
    password,
  });

  await db.collection("users").document(userRecord.uid).set({
    uid: userRecord.uid,
    email: normalizedEmail,
    phone: normalizedPhone,
    name: name || "",
    birth: birth || "",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).delete();

  return { success: true, uid: userRecord.uid };
});

/**
 * 비밀번호 재설정용 인증번호 SMS 발송 (가입된 휴대폰 번호로)
 */
exports.sendPasswordResetSms = functions.https.onCall(async (data, context) => {
  const phone = data.phone;
  if (!phone || typeof phone !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "휴대폰 번호가 필요합니다.");
  }

  const normalizedPhone = normalizePhone(phone);
  if (normalizedPhone.length < 10) {
    throw new functions.https.HttpsError("invalid-argument", "올바른 휴대폰 번호를 입력해주세요.");
  }

  const db = admin.firestore();
  const usersSnapshot = await db.collection("users").where("phone", "==", normalizedPhone).limit(1).get();

  if (usersSnapshot.empty) {
    throw new functions.https.HttpsError("failed-precondition", "가입된 계정이 아닙니다.");
  }

  const code = generateCode();
  const docId = phoneToDocId(normalizedPhone) + "_reset";
  const expiresAt = new Date(Date.now() + CODE_EXPIRY_MINUTES * 60 * 1000);

  await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).set({
    phone: normalizedPhone,
    code,
    expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
    purpose: "password_reset",
  });

  await sendSms(normalizedPhone, `[Cole] 비밀번호 재설정 인증번호: ${code}\n${CODE_EXPIRY_MINUTES}분 안에 입력해주세요.`);

  return { success: true };
});

/**
 * 비밀번호 재설정 인증번호 검증 후 비밀번호 변경
 */
exports.verifyAndResetPassword = functions.https.onCall(async (data, context) => {
  const { phone, code, newPassword } = data;
  if (!phone || !code || !newPassword) {
    throw new functions.https.HttpsError("invalid-argument", "휴대폰 번호, 인증번호, 새 비밀번호가 필요합니다.");
  }

  const normalizedPhone = normalizePhone(phone);
  const db = admin.firestore();
  const docId = phoneToDocId(normalizedPhone) + "_reset";
  const codeDoc = await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).get();

  if (!codeDoc.exists) {
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 만료되었거나 올바르지 않습니다.");
  }

  const { code: storedCode, expiresAt, purpose } = codeDoc.data();
  if (purpose !== "password_reset" || storedCode !== code) {
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 올바르지 않습니다.");
  }

  const now = admin.firestore.Timestamp.now();
  if (expiresAt.toMillis() < now.toMillis()) {
    await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).delete();
    throw new functions.https.HttpsError("failed-precondition", "인증번호가 만료되었습니다. 다시 발송해주세요.");
  }

  const usersSnapshot = await db.collection("users").where("phone", "==", normalizedPhone).limit(1).get();
  if (usersSnapshot.empty) {
    throw new functions.https.HttpsError("not-found", "가입된 계정이 아닙니다.");
  }
  const userDoc = usersSnapshot.docs[0].data();
  const email = userDoc.email;
  if (!email) {
    throw new functions.https.HttpsError("internal", "사용자 정보를 찾을 수 없습니다.");
  }

  const userRecord = await admin.auth().getUserByEmail(email);
  await admin.auth().updateUser(userRecord.uid, { password: newPassword });

  await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).delete();

  return { success: true };
});
