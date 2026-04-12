# 하루 사용 제한 위젯 — 수정 관련 파일 맵

Figma 노드: `1718:5871`(3개) / `1737:6029`(2개) / `1737:5982`(1개) / `1737:5952`(0개)

---

## 1. 진입점 · 업데이트 로직

| 파일 | 역할 |
|------|------|
| `app/src/main/java/com/aptox/app/widget/AptoxDailyLimitWidgetProvider.kt` | **핵심.** `onUpdate` · `onAppWidgetOptionsChanged` · 행 바인딩 · CTA 가시성 분기 |
| `app/src/main/java/com/aptox/app/widget/DailyLimitWidgetDataLoader.kt` | 하루 사용량 데이터 로드 (앱 목록, 사용 시간, 진행률) |
| `app/src/main/java/com/aptox/app/widget/RestrictionsWidgetUpdateHelper.kt` | 앱 내부에서 `updateAll()` 호출하는 헬퍼 |

---

## 2. 레이아웃 · UI 스케일

| 파일 | 역할 |
|------|------|
| `app/src/main/res/layout/widget_daily_limit_4x2.xml` | **레이아웃 XML.** RemoteViews 전용 (LinearLayout / TextView / ImageView / ProgressBar만 사용) |
| `app/src/main/java/com/aptox/app/widget/DailyLimitWidgetAppearance.kt` | Figma 110 캔버스 → 실제 위젯 dp 비율 스케일 (패딩·폰트·바 높이·마진 적용) |
| `app/src/main/java/com/aptox/app/widget/AppWidgetSizeDp.kt` | `AppWidgetManager.getAppWidgetOptions()` 로 실제 위젯 dp 읽기 |
| `app/src/main/java/com/aptox/app/widget/WidgetDesignFloat.kt` | `<item format="float" type="dimen">` 리소스 읽기 유틸 |

---

## 3. 디자인 토큰 (수치 변경 시 여기만 수정)

| 파일 | 역할 |
|------|------|
| `app/src/main/res/values/widget_daily_limit_design.xml` | **Figma 110 캔버스 기준 수치 전부.** 패딩·타이틀 sp·행 sp·바 높이·간격·CTA 최소 높이 |
| `app/src/main/res/values/widget_daily_limit_dimens.xml` | corner radius (배경·CTA·프로그레스 바) |

---

## 4. 드로어블

| 파일 | 역할 |
|------|------|
| `app/src/main/res/drawable/widget_daily_limit_bg.xml` | 위젯 전체 배경 `#1E1E1E`, radius 6dp |
| `app/src/main/res/drawable/widget_daily_limit_cta_bg.xml` | CTA 블록 배경 `rgba(255,255,255,0.02)`, radius 6dp |
| `app/src/main/res/drawable/widget_daily_limit_progress.xml` | 진행 바 (트랙 `rgba(255,255,255,0.05)` / 채움 `#6C54DD`) |
| `app/src/main/res/drawable/ic_widget_daily_limit_header.xml` | 헤더 우측 아이콘 벡터 (현재 `ic_add` 사용 중 → 교체 가능) |

---

## 5. 색상 (colors.xml 내 관련 항목)

파일: `app/src/main/res/values/colors.xml`

```
widget_daily_limit_surface       #FF1E1E1E   배경
widget_daily_limit_text          #FFFFFFFF   텍스트
widget_daily_limit_progress_fill #FF6C54DD   진행 바 채움
widget_daily_limit_progress_track #0DFFFFFF  진행 바 트랙 (rgba 5%)
widget_daily_limit_cta_fill      #05FFFFFF   CTA 블록 (rgba 2%)
```

---

## 6. 문자열 (strings.xml 내 관련 항목)

파일: `app/src/main/res/values/strings.xml`

```
widget_daily_limit_header_title    「하루 사용 제한」 헤더 텍스트
widget_daily_limit_cta             「사용 제한 앱 추가」 CTA 문구
widget_daily_limit_status_limited  "%1$d/%2$d분 사용 중"
widget_daily_limit_status_unlimited "%1$d분 사용 중"
widget_daily_limit_header_icon_cd  접근성 설명
```

---

## 7. 위젯 메타데이터

| 파일 | 역할 |
|------|------|
| `app/src/main/res/xml/aptox_daily_limit_widget_info.xml` | API 30 이하 — minWidth/minHeight 110dp |
| `app/src/main/res/xml-v31/aptox_daily_limit_widget_info.xml` | API 31+ — targetCellWidth/Height 2 |

---

## 8. 폰트 (현재 미사용 — RemoteViews inflate 안정성 때문에 sans-serif 고정)

> 아래 파일은 생성되어 있으나, XML에서 직접 참조하면 런처 프로세스에서 인플레이트 실패 가능.
> 런타임에 `setTypeface` 로 적용하거나, API 31+ 전용 분기로만 사용할 것.

```
app/src/main/res/font/suit_widget_daily_title.xml   SUIT Variable wght 500
app/src/main/res/font/suit_widget_daily_row.xml     SUIT Variable wght 660
app/src/main/res/font/suit_widget_daily_cta.xml     SUIT Variable wght 660
```

---

## 케이스별 동작 요약

| 앱 수 | 행 1 | 행 2 | 행 3 | CTA 가시성 | CTA minHeight |
|-------|------|------|------|-----------|---------------|
| 3개   | VISIBLE | VISIBLE | VISIBLE | GONE | — |
| 2개   | VISIBLE | VISIBLE | GONE | VISIBLE | 20dp 스케일 |
| 1개   | VISIBLE | GONE | GONE | VISIBLE | 44dp 스케일 |
| 0개   | GONE | GONE | GONE | VISIBLE | weight=1 전체 |

CTA를 탭하면 MainActivity가 열림 (PendingIntent → MainActivity).
