# Cole Cloud Functions - Solapi SMS 인증

휴대폰 번호로 6자리 인증번호를 SMS 발송하고, 회원가입 및 비밀번호 재설정 시 검증합니다.

## 사전 설정

### 1. Firebase 프로젝트 연결

```bash
firebase login
firebase use <your-project-id>
```

### 2. Solapi 발신번호 등록

1. [Solapi 콘솔](https://console.solapi.com)에서 발신번호 등록
2. API Key와 API Secret 확인 (Credentials 메뉴)

### 3. Cloud Functions 설정 (선택)

기본값이 코드에 포함되어 있습니다. 다른 키 사용 시:

```bash
firebase functions:config:set solapi.api_key="YOUR_API_KEY" solapi.api_secret="YOUR_API_SECRET" solapi.sender_phone="01012345678"
```

`solapi.sender_phone`은 Solapi에 등록한 발신번호입니다.

## 배포

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

## 함수 목록

- **sendSignUpVerificationSms** (callable): 회원가입용 6자리 인증번호 SMS 발송
- **verifyAndCompleteSignUp** (callable): 인증번호 검증 후 Firebase 계정 생성
- **sendPasswordResetSms** (callable): 비밀번호 재설정용 6자리 인증번호 SMS 발송 (가입된 휴대폰으로)
- **verifyAndResetPassword** (callable): 인증번호 검증 후 비밀번호 변경
