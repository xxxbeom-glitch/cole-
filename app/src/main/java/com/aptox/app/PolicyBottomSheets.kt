package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 이용약관 바텀시트 (Figma 1022-9092)
 * - 제목: 서비스 이용약관 (HeadingH3)
 * - 내용 카드: 높이 280dp, 상하패딩 적용, 카드 내부 스크롤
 * - 본문: TextBody 컬러
 */
@Composable
fun TermsPolicyBottomSheet(
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        title = "서비스 이용약관",
        onDismissRequest = onDismiss,
        onPrimaryClick = onDismiss,
        primaryButtonText = "확인",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "서비스 이용약관",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "시행일: 2025년 1월 1일",
                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = """제1조 (목적) 본 약관은 aptox(이하 '회사')가 제공하는 스마트폰 사용 관리 서비스(이하 '서비스')의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임 사항을 규정함을 목적으로 합니다.

제2조 (정의) 본 약관에서 사용하는 용어의 정의는 다음과 같습니다. ① '서비스'란 회사가 제공하는 앱 차단, 사용 시간 관리 등 디지털 디톡스 관련 모든 기능을 의미합니다. ② '이용자'란 본 약관에 동의하고 회사가 제공하는 서비스를 이용하는 자를 의미합니다. ③ '프리미엄 서비스'란 유료 구독을 통해 제공되는 추가 기능을 의미합니다. ④ '콘텐츠'란 서비스 내에서 제공되는 통계, 배지, 달성 기록 등 모든 데이터를 의미합니다.

제3조 (약관의 효력 및 변경) ① 본 약관은 서비스를 이용하고자 하는 모든 이용자에게 적용됩니다. ② 회사는 필요한 경우 관련 법령에 위반되지 않는 범위 내에서 본 약관을 변경할 수 있습니다. ③ 약관이 변경되는 경우, 회사는 변경 사항을 시행일 7일 전부터 앱 내 공지 또는 이메일을 통해 고지합니다.

제4조 (서비스 이용 및 계정) ① 이용자는 카카오, 네이버, 구글 소셜 로그인을 통해 계정을 생성할 수 있습니다. ② 이용자는 자신의 계정 정보를 안전하게 관리할 책임이 있으며, 타인에게 계정을 양도하거나 공유할 수 없습니다. ③ 이용자는 타인의 계정을 무단으로 사용해서는 안 됩니다. ④ 회사는 이용자가 본 약관을 위반한 경우 사전 통보 없이 계정을 정지 또는 삭제할 수 있습니다.

제5조 (유료 서비스 및 결제) ① 프리미엄 서비스는 월간 (₩3,900) 또는 연간(₩39,000) 구독을 통해 이용할 수 있습니다. ② 결제는 Google Play 스토어를 통해 이루어지며, 결제 수단 관리는 Google Play 계정 설정에서 진행합니다. ③ 구독은 해지하지 않는 한 자동으로 갱신되며, 갱신 전 Google Play 스토어에서 구독을 취소할 수 있습니다. ④ 구독 취소 후에도 결제된 구독 기간 만료일까지 프리미엄 서비스를 이용할 수 있습니다.

제6조 (개인정보 보호) ① 회사는 이용자의 개인정보를 관련 법령 및 개인정보처리방침에 따라 보호합니다. ② 수집되는 정보: 소셜 로그인 프로필(이름, 프로필 사진), 앱 사용 통계, 차단 설정 데이터가 포함됩니다.""",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
        }
    }
}

/**
 * 개인정보처리방침 바텀시트 (Figma 1022-9092 동일 구조)
 * - 제목: 개인정보처리방침 (HeadingH3)
 * - 내용 카드: 높이 280dp, 상하패딩 적용, 카드 내부 스크롤
 * - 본문: TextBody 컬러
 */
@Composable
fun PrivacyPolicyBottomSheet(
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        title = "개인정보처리방침",
        onDismissRequest = onDismiss,
        onPrimaryClick = onDismiss,
        primaryButtonText = "확인",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "개인정보처리방침",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "시행일: 2025년 1월 1일",
                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = """제1조 (개인정보의 수집 및 이용 목적) 회사는 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않습니다. ① 서비스 제공 ② 회원 관리 ③ 서비스 개선 및 신규 서비스 개발

제2조 (수집하는 개인정보의 항목) 회사는 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다. ① 필수항목: 소셜 로그인 프로필(이름, 프로필 사진), 이메일 주소 ② 자동 수집: 앱 사용 통계, 차단 설정 데이터, 기기 정보

제3조 (개인정보의 보유 및 이용 기간) 회사는 법령에 따른 개인정보 보유·이용기간 또는 정보주체로부터 개인정보를 수집 시에 동의받은 개인정보 보유·이용기간 내에서 개인정보를 처리·보유합니다. 회원 탈퇴 시 개인정보는 즉시 삭제됩니다. 단, 관련 법령에 의해 보존이 필요한 경우 해당 기간 동안 보관합니다.

제4조 (개인정보의 제3자 제공) 회사는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다. 다만, 이용자가 사전에 동의한 경우 또는 법령의 규정에 의거하거나 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우에는 예외로 합니다.

제5조 (개인정보 처리의 위탁) 회사는 서비스 향상을 위해 아래와 같이 개인정보 처리 업무를 외부 전문업체에 위탁하여 운영하고 있습니다. ① Google Firebase: 인증 및 데이터 저장 ② Google Play: 결제 처리

제6조 (이용자의 권리) 이용자는 언제든지 자신의 개인정보를 조회하거나 수정할 수 있으며, 수집·이용에 대한 동의를 철회하거나 가입 해지를 요청할 수 있습니다. 개인정보 관련 문의는 앱 내 고객센터를 통해 접수할 수 있습니다.""",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
        }
    }
}
