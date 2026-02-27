# SUIT Variable 폰트 weight · Figma 연동 이슈

## 0. 기본 원칙
**AppTypography 기본 스타일(Display1~TabUnselected)은 디자인 가이드 기준 → 값 변동 금지**
- Figma와 차이가 나더라도, 가이드가 정한 값은 변경하지 않음
- 화면별 Figma 비표준 weight가 필요한 경우만 별도 스타일(ST10xxx 등) 추가

## 1. 현상
- Figma에서 지정한 폰트 weight가 앱에서 제대로 반영되지 않음
- SUIT Variable 폰트의 weight가 특히 문제

## 2. SUIT Variable 현재 설정 (AppTypography.kt)

### FontFamily에 정의된 weight
| FontWeight | variationSettings | 용도 |
|------------|-------------------|------|
| 100 (Thin) | weight(100) | - |
| 200 (ExtraLight) | weight(200) | - |
| 300 (Light) | weight(300) | - |
| 400 (Normal) | weight(400) | BodyRegular, Input |
| 500 (Medium) | weight(500) | BodyMedium, Caption1 |
| 600 (SemiBold) | weight(600) | BodyBold, ButtonSmall, Disclaimer |
| 700 (Bold) | weight(700) | Display3, Caption2, ButtonLarge, TabSelected |
| 720 | weight(720) | HeadingH3 |
| 760 | weight(760) | HeadingH2 |
| 800 (ExtraBold) | weight(800) | Display1, HeadingH1, Label |
| 900 (Black) | weight(900) | - |

### Variable 폰트 특성
- Compose 주석: "FontWeight 파라미터만으로는 Variable 폰트의 wght axis가 동작하지 않음"
- 각 Font에 `variationSettings = FontVariation.Settings(FontVariation.weight(X))` 필수
- **TextStyle의 fontWeight**와 **FontFamily 내 matching Font**가 맞아야 올바른 weight 적용

## 3. Figma weight → Compose 매핑
Figma는 보통 100~900 숫자 사용. 아래와 매칭:
- 400 → Normal
- 500 → Medium
- 600 → SemiBold
- 700 → Bold
- 800 → ExtraBold

**주의:** Figma가 650, 720, 760 등 비표준 값을 쓰면, 해당 weight용 Font가 FontFamily에 있어야 함.

## 4. 확인 사항 (Figma Dev Mode)
각 텍스트 스타일에서 확인:
- **font-weight** (숫자)
- **font-size**
- **line-height**

현재 AppTypography와 Figma가 다른 항목이 있으면, FontFamily에 해당 weight의 Font를 추가하거나, AppTypography의 fontWeight를 Figma 숫자에 맞게 조정.

## 5. ST-10 화면 텍스트별 현재 스타일
| 요소 | 스타일 | weight |
|------|--------|--------|
| 장원영 님의 ~ 결과입니다 | Display3 | 700 (Bold) |
| 스마트폰 사용 ~ 해요 | BodyBold | 600 (SemiBold) |
| 습관 라벨 | BodyMedium | 500 (Medium) |
| 습관 상태 | BodyBold | 600 (SemiBold) |
| 디스클레임 | Caption1 | 500 (Medium) |
| 확인 버튼 | ButtonLarge | 700 (Bold) |
| 다시하기 | BodyMedium | 500 (Medium) |

## 6. 권장 조치
1. Figma 317-3002, 443-2572 등에서 각 텍스트의 **font-weight (숫자)** 확인
2. Figma weight가 720, 760 등이면 → 이미 Font 있음. AppTypography fontWeight가 맞는지 확인
3. Figma weight가 650, 350 등이면 → FontFamily에 `Font(weight = FontWeight(650), variationSettings = FontVariation.Settings(FontVariation.weight(650)))` 추가
4. 모든 스타일이 `fontWeight = FontWeight.XXX` 형태로 FontFamily 내 Font와 1:1 매칭되는지 검증
