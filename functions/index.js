const functions = require("firebase-functions");
const admin = require("firebase-admin");
const https = require("https");
const { SolapiMessageService } = require("solapi");
const Anthropic = require("@anthropic-ai/sdk").default;

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
 * sendOne: 단일 메시지 API (messages/v4/send)
 */
async function sendSms(to, text) {
  const { apiKey, apiSecret, senderPhone } = getSolapiConfig();
  const messageService = new SolapiMessageService(apiKey, apiSecret);
  const normalizedTo = normalizePhone(to);
  const from = normalizePhone(senderPhone);
  if (from.length < 10) {
    throw new Error("Solapi 발신번호가 설정되지 않았습니다. firebase functions:config:set solapi.sender_phone=01012345678");
  }
  // sendOne: 단건 발송 (SMS 기본)
  await messageService.sendOne({
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

  try {
    await sendSms(normalizedPhone, `[aptox] 회원가입 인증번호: ${code}\n${CODE_EXPIRY_MINUTES}분 안에 입력해주세요.`);
  } catch (e) {
    const errMsg = e?.errorMessage || e?.message || String(e);
    const errCode = e?.errorCode || e?.code || e?.statusCode || "";
    console.error("Solapi sendSignUpVerificationSms error:", errMsg, "code:", errCode);
    const msg = errMsg.toLowerCase();
    if (msg.includes("발신") || msg.includes("from") || msg.includes("sender")) {
      throw new functions.https.HttpsError("failed-precondition", "발신번호가 Solapi 콘솔에 등록되지 않았어요. console.solapi.com에서 발신번호를 등록해주세요.");
    }
    if (msg.includes("잔액") || msg.includes("balance") || errCode === "1030" || errCode === "2230" || String(errCode).includes("1030") || String(errCode).includes("2230")) {
      throw new functions.https.HttpsError("failed-precondition", "Solapi 잔액이 부족해요. console.solapi.com에서 충전해주세요.");
    }
    if (msg.includes("auth") || msg.includes("api") || msg.includes("key") || msg.includes("1020")) {
      throw new functions.https.HttpsError("failed-precondition", "Solapi API 키가 잘못되었어요. console.solapi.com에서 API 키를 확인해주세요.");
    }
    if (msg.includes("수신") || msg.includes("3010")) {
      throw new functions.https.HttpsError("invalid-argument", "수신번호 형식이 올바르지 않아요. 01012345678 형식으로 입력해주세요.");
    }
    const shortMsg = (errMsg || "").substring(0, 80).replace(/[^\w\s\u3131-\u318E\uAC00-\uD7A3.-]/g, "");
    throw new functions.https.HttpsError("internal", "문자 발송에 실패했어요. " + (shortMsg ? "(" + shortMsg + ") " : "") + "console.solapi.com에서 발신번호·API키를 확인하고 Firebase 로그를 확인해주세요.");
  }

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

  try {
    const userRecord = await admin.auth().createUser({
      email: normalizedEmail,
      password,
    });

    await db.collection("users").doc(userRecord.uid).set({
      uid: userRecord.uid,
      email: normalizedEmail,
      phone: normalizedPhone,
      name: name || "",
      birth: birth || "",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    await db.collection(VERIFICATION_CODES_COLLECTION).doc(docId).delete();

    return { success: true, uid: userRecord.uid };
  } catch (e) {
    const code = e?.code || e?.errorInfo?.code || "";
    const errMsg = e?.message || e?.errorInfo?.message || String(e);
    console.error("verifyAndCompleteSignUp error:", code, errMsg);

    if (code === "auth/email-already-exists" || code === "auth/email-already-in-use" || code === "auth/credential-already-in-use") {
      throw new functions.https.HttpsError("already-exists", "이미 사용 중인 이메일이에요.");
    }
    if (code === "auth/invalid-password" || code === "auth/weak-password") {
      throw new functions.https.HttpsError("invalid-argument", "비밀번호가 너무 약해요. 8자 이상, 영문 대·소문자·숫자를 포함해주세요.");
    }
    if (code === "auth/invalid-email") {
      throw new functions.https.HttpsError("invalid-argument", "이메일 형식이 올바르지 않아요.");
    }
    throw new functions.https.HttpsError("internal", "계정 생성에 실패했어요. 잠시 후 다시 시도해주세요.");
  }
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

  try {
    await sendSms(normalizedPhone, `[aptox] 비밀번호 재설정 인증번호: ${code}\n${CODE_EXPIRY_MINUTES}분 안에 입력해주세요.`);
  } catch (e) {
    const errMsg = e?.errorMessage || e?.message || String(e);
    const errCode = e?.errorCode || e?.code || e?.statusCode || "";
    console.error("Solapi sendPasswordResetSms error:", errMsg, "code:", errCode);
    const msg = errMsg.toLowerCase();
    if (msg.includes("발신") || msg.includes("from") || msg.includes("sender")) {
      throw new functions.https.HttpsError("failed-precondition", "발신번호가 Solapi 콘솔에 등록되지 않았어요. console.solapi.com에서 발신번호를 등록해주세요.");
    }
    if (msg.includes("잔액") || msg.includes("balance") || errCode === "1030" || errCode === "2230") {
      throw new functions.https.HttpsError("failed-precondition", "Solapi 잔액이 부족해요. console.solapi.com에서 충전해주세요.");
    }
    if (msg.includes("auth") || msg.includes("api") || msg.includes("key") || msg.includes("1020")) {
      throw new functions.https.HttpsError("failed-precondition", "Solapi API 키가 잘못되었어요. console.solapi.com에서 API 키를 확인해주세요.");
    }
    const shortMsg = (errMsg || "").substring(0, 80).replace(/[^\w\s\u3131-\u318E\uAC00-\uD7A3.-]/g, "");
    throw new functions.https.HttpsError("internal", "문자 발송에 실패했어요. " + (shortMsg ? "(" + shortMsg + ") " : "") + "console.solapi.com에서 발신번호·API키를 확인하고 Firebase 로그를 확인해주세요.");
  }

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

/**
 * 카카오 액세스 토큰 → Firebase Custom Token 변환
 * Android에서 onCall("kakaoLogin", { accessToken }) 으로 호출
 */
exports.kakaoLogin = functions.https.onCall(async (data, context) => {
  const accessToken = data.accessToken;
  if (!accessToken || typeof accessToken !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "accessToken이 필요합니다.");
  }

  // 카카오 사용자 정보 조회
  let kakaoUser;
  try {
    kakaoUser = await getKakaoUserInfo(accessToken);
  } catch (e) {
    console.error("카카오 사용자 정보 조회 실패:", e.message);
    throw new functions.https.HttpsError("unauthenticated", "카카오 토큰이 유효하지 않습니다.");
  }

  const kakaoId = String(kakaoUser.id);
  const uid = `kakao_${kakaoId}`;
  const nickname = kakaoUser.kakao_account?.profile?.nickname || "";
  const email = kakaoUser.kakao_account?.email || null;
  const profileImage = kakaoUser.kakao_account?.profile?.profile_image_url || null;

  // Firestore users 저장/업데이트
  const db = admin.firestore();
  const userRef = db.collection("users").doc(uid);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    await userRef.set({
      uid,
      provider: "kakao",
      kakaoId,
      nickname,
      ...(email && { email }),
      ...(profileImage && { profileImage }),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else {
    await userRef.update({
      nickname,
      ...(email && { email }),
      ...(profileImage && { profileImage }),
      lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  // Firebase Custom Token 발급
  try {
    const firebaseToken = await admin.auth().createCustomToken(uid, { provider: "kakao", kakaoId });
    return { firebaseToken };
  } catch (e) {
    console.error("kakaoLogin createCustomToken 실패:", e.message, "uid:", uid);
    throw new functions.https.HttpsError("internal", "인증 토큰 발급 실패: " + (e.message || String(e)));
  }
});

/**
 * 네이버 액세스 토큰 → Firebase Custom Token 변환
 * Android에서 onCall("naverLogin", { accessToken }) 으로 호출
 */
exports.naverLogin = functions.https.onCall(async (data, context) => {
  const accessToken = data.accessToken;
  if (!accessToken || typeof accessToken !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "accessToken이 필요합니다.");
  }

  // 네이버 사용자 정보 조회
  let naverUser;
  try {
    naverUser = await getNaverUserInfo(accessToken);
  } catch (e) {
    console.error("네이버 사용자 정보 조회 실패:", e.message);
    throw new functions.https.HttpsError("unauthenticated", "네이버 토큰이 유효하지 않습니다.");
  }

  const response = naverUser.response;
  if (!response || !response.id) {
    throw new functions.https.HttpsError("unauthenticated", "네이버 사용자 정보를 가져올 수 없습니다.");
  }

  const naverId = String(response.id);
  const uid = `naver_${naverId}`;
  const nickname = response.nickname || response.name || "";
  const email = response.email || null;
  const profileImage = response.profile_image || null;

  // Firestore users 저장/업데이트
  const db = admin.firestore();
  const userRef = db.collection("users").doc(uid);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    await userRef.set({
      uid,
      provider: "naver",
      naverId,
      nickname,
      ...(email && { email }),
      ...(profileImage && { profileImage }),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else {
    await userRef.update({
      nickname,
      ...(email && { email }),
      ...(profileImage && { profileImage }),
      lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  // Firebase Custom Token 발급
  try {
    const firebaseToken = await admin.auth().createCustomToken(uid, { provider: "naver", naverId });
    return { firebaseToken };
  } catch (e) {
    console.error("naverLogin createCustomToken 실패:", e.message, "uid:", uid);
    throw new functions.https.HttpsError("internal", "인증 토큰 발급 실패: " + (e.message || String(e)));
  }
});

/**
 * 네이버 사용자 정보 API 호출
 */
function getNaverUserInfo(accessToken) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "openapi.naver.com",
      path: "/v1/nid/me",
      method: "GET",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    };
    const req = https.request(options, (res) => {
      let body = "";
      res.on("data", (chunk) => { body += chunk; });
      res.on("end", () => {
        if (res.statusCode !== 200) {
          reject(new Error(`Naver API error: ${res.statusCode} ${body}`));
        } else {
          try {
            resolve(JSON.parse(body));
          } catch (e) {
            reject(new Error("Naver API 응답 파싱 실패"));
          }
        }
      });
    });
    req.on("error", reject);
    req.end();
  });
}

/**
 * 카카오 사용자 정보 API 호출 (node https 내장 모듈)
 */
function getKakaoUserInfo(accessToken) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "kapi.kakao.com",
      path: "/v2/user/me",
      method: "GET",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    };
    const req = https.request(options, (res) => {
      let body = "";
      res.on("data", (chunk) => { body += chunk; });
      res.on("end", () => {
        if (res.statusCode !== 200) {
          reject(new Error(`Kakao API error: ${res.statusCode} ${body}`));
        } else {
          try {
            resolve(JSON.parse(body));
          } catch (e) {
            reject(new Error("Kakao API 응답 파싱 실패"));
          }
        }
      });
    });
    req.on("error", reject);
    req.end();
  });
}

/**
 * 앱 목록 AI 카테고리 분류 - 앱에서 classifyApps({ apps: [{ package, appName }] }) 로 호출
 * API 키: firebase functions:config:set anthropic.key="sk-ant-api03-..."
 * 응답: { results: [{ package, appName, category }] } — category는 SNS/게임/OTT/쇼핑/웹툰/주식·코인/기타 중 하나
 */
exports.classifyApps = functions.https.onCall(async (data, context) => {
  const apps = data?.apps;
  if (!Array.isArray(apps) || apps.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "apps 배열이 필요합니다.");
  }

  const config = functions.config().anthropic || {};
  const apiKey = config.api_key || config.key || process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Anthropic API 키가 설정되지 않았습니다. firebase functions:config:set anthropic.api_key=sk-ant-api03-..."
    );
  }

  const categories = "SNS, 게임, OTT, 쇼핑, 웹툰, 주식·코인, 기타";
  const appListStr = apps
    .map((a) => `- ${a.package || "(없음)"} | ${a.appName || "(없음)"}`)
    .join("\n");

  const prompt = `다음 앱 목록을 아래 7개 카테고리 중 정확히 하나씩 분류해주세요. 카테고리: ${categories}

앱 목록:
${appListStr}

반드시 JSON 배열만 출력하세요. 다른 설명 없이. 형식:
[{"package":"패키지명","appName":"앱이름","category":"카테고리"}]
각 category는 반드시 ${categories} 중 정확히 하나만 사용.`;

  try {
    const client = new Anthropic({ apiKey });
    const response = await client.messages.create({
      model: "claude-sonnet-4-20250514",
      max_tokens: 2048,
      messages: [{ role: "user", content: prompt }],
    });

    const textContent = response.content.find((c) => c.type === "text");
    const rawText = textContent ? textContent.text.trim() : "";

    // JSON 블록 추출 (```json ... ``` 또는 그냥 [...])
    let jsonStr = rawText;
    const codeBlockMatch = rawText.match(/```(?:json)?\s*([\s\S]*?)```/);
    if (codeBlockMatch) jsonStr = codeBlockMatch[1].trim();
    const arr = JSON.parse(jsonStr);
    if (!Array.isArray(arr)) throw new Error("응답이 배열이 아닙니다.");

    const results = arr.map((item) => ({
      package: String(item.package || ""),
      appName: String(item.appName || ""),
      category: String(item.category || "기타").trim(),
    }));
    return { results };
  } catch (e) {
    const errMsg = e?.message || String(e);
    console.error("classifyApps error:", errMsg);
    if (errMsg.includes("invalid_api_key") || errMsg.includes("401")) {
      throw new functions.https.HttpsError("failed-precondition", "Claude API 키가 유효하지 않습니다.");
    }
    if (errMsg.includes("rate_limit") || errMsg.includes("429")) {
      throw new functions.https.HttpsError("resource-exhausted", "요청이 너무 많아요. 잠시 후 다시 시도해주세요.");
    }
    throw new functions.https.HttpsError("internal", "앱 분류에 실패했어요. " + errMsg.substring(0, 80));
  }
});

/**
 * Claude API 호출 - 앱에서 callClaude({ prompt: "..." }) 로 호출
 * API 키 설정: firebase functions:config:set anthropic.api_key="sk-ant-api03-..."
 */
exports.callClaude = functions.https.onCall(async (data, context) => {
  const prompt = data?.prompt;
  if (!prompt || typeof prompt !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "prompt가 필요합니다.");
  }

  const config = functions.config().anthropic || {};
  const apiKey = config.api_key || process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Anthropic API 키가 설정되지 않았습니다. firebase functions:config:set anthropic.api_key=sk-ant-api03-..."
    );
  }

  try {
    const client = new Anthropic({ apiKey });
    const response = await client.messages.create({
      model: "claude-sonnet-4-5-20250929",
      max_tokens: 1024,
      messages: [{ role: "user", content: prompt.trim() }],
    });

    const textContent = response.content.find((c) => c.type === "text");
    const reply = textContent ? textContent.text : "";

    return { reply, usage: response.usage };
  } catch (e) {
    const errMsg = e?.message || String(e);
    console.error("callClaude error:", errMsg);
    if (errMsg.includes("invalid_api_key") || errMsg.includes("401")) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Claude API 키가 유효하지 않습니다. console.anthropic.com에서 확인해주세요."
      );
    }
    if (errMsg.includes("rate_limit") || errMsg.includes("429")) {
      throw new functions.https.HttpsError("resource-exhausted", "요청이 너무 많아요. 잠시 후 다시 시도해주세요.");
    }
    throw new functions.https.HttpsError("internal", "Claude 응답 생성에 실패했어요. " + errMsg.substring(0, 80));
  }
});

/**
 * 버그 신고 제출 - Storage URL 수신 후 Firestore 저장
 * data: { title?: string, content: string, imageUrls?: string[] }
 */
exports.submitBugReport = functions.https.onCall(async (data, context) => {
  const title = data?.title;
  const content = data?.content;
  const imageUrls = Array.isArray(data?.imageUrls) ? data.imageUrls : [];
  if (!content || typeof content !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "내용을 입력해주세요.");
  }

  const trimmedContent = content.trim();
  if (trimmedContent.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "내용을 입력해주세요.");
  }

  const trimmedTitle = (title && typeof title === "string") ? title.trim() : "";

  let docRef;
  try {
    const db = admin.firestore();
    const hasImages = imageUrls.length > 0;
    const messageParts = [
      trimmedTitle ? `제목: ${trimmedTitle}` : null,
      `내용: ${trimmedContent}`,
      hasImages ? `이미지 URLs:\n${imageUrls.join("\n")}` : null,
    ].filter(Boolean);
    const messageText = messageParts.join("\n\n");
    const messageSubject = trimmedTitle ? `[버그신고] ${trimmedTitle}` : "[버그신고] 제목 없음";
    docRef = await db.collection("bugReports").add({
      to: "sky0805sky@naver.com",
      message: {
        subject: messageSubject,
        text: messageText,
      },
      title: trimmedTitle || null,
      body: trimmedContent,
      description: trimmedContent,
      content: trimmedContent,
      imageUrl: hasImages ? imageUrls[0] : null,
      imageUrls: hasImages ? imageUrls : null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      userId: context.auth?.uid || null,
    });
  } catch (e) {
    console.error("submitBugReport Firestore 저장 실패:", e.message);
    throw new functions.https.HttpsError("internal", "저장 실패: " + (e.message || "알 수 없는 오류"));
  }

  return { success: true, id: docRef.id };
});

/**
 * 로그인한 사용자 본인 계정 삭제 (Admin SDK)
 * 클라이언트는 이 호출 성공 후 Firestore users/{uid} 정리 및 로컬 초기화를 진행한다.
 */
exports.deleteAccount = functions.https.onCall(async (data, context) => {
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError("unauthenticated", "로그인이 필요합니다.");
  }
  const uid = context.auth.uid;
  try {
    await admin.auth().deleteUser(uid);
    return { success: true };
  } catch (e) {
    const code = e?.code || e?.errorInfo?.code || "";
    if (code === "auth/user-not-found") {
      return { success: true };
    }
    console.error("deleteAccount error:", code, e?.message || e);
    throw new functions.https.HttpsError(
      "internal",
      "계정 삭제에 실패했습니다. 잠시 후 다시 시도해주세요."
    );
  }
});
