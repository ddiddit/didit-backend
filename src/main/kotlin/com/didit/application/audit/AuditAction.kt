package com.didit.application.audit

enum class AuditAction(
    val label: String,
    val actorType: ActorType,
) {
    USER_LOGGED_IN("로그인", ActorType.USER),
    USER_LOGGED_OUT("로그아웃", ActorType.USER),
    USER_SIGNED_UP("회원가입", ActorType.USER),
    USER_PROFILE_UPDATED("프로필 수정", ActorType.USER),
    USER_WITHDREW("회원 탈퇴", ActorType.USER),

    ADMIN_LOGGED_IN("관리자 로그인", ActorType.ADMIN),
    ADMIN_LOGGED_OUT("관리자 로그아웃", ActorType.ADMIN),
    ADMIN_INVITED("관리자 초대", ActorType.ADMIN),
    ADMIN_REGISTERED("관리자 가입", ActorType.ADMIN),
    ADMIN_APPROVED("관리자 승인", ActorType.ADMIN),
    ADMIN_REJECTED("관리자 거절", ActorType.ADMIN),
    ADMIN_DELETED("관리자 삭제", ActorType.ADMIN),
    ADMIN_NOTICE_EMAIL_SENT("공지 이메일 발송", ActorType.ADMIN),

    INQUIRY_ANSWERED("문의 답변", ActorType.ADMIN),
    INQUIRY_ANSWER_UPDATED("문의 답변 수정", ActorType.ADMIN),
    INQUIRY_ANSWER_DELETED("문의 답변 삭제", ActorType.ADMIN),
    INQUIRY_REGISTERED("문의 등록", ActorType.USER),
    INQUIRY_DELETED("문의 삭제", ActorType.USER),

    NOTICE_REGISTERED("공지 등록", ActorType.ADMIN),
    NOTICE_UPDATED("공지 수정", ActorType.ADMIN),
    NOTICE_DELETED("공지 삭제", ActorType.ADMIN),

    RETROSPECTIVE_STARTED("회고 시작", ActorType.USER),
    RETROSPECTIVE_SAVED("회고 저장", ActorType.USER),
    RETROSPECTIVE_DELETED("회고 삭제", ActorType.USER),
    RETROSPECTIVE_RESTARTED("회고 다시 시작", ActorType.USER),

    BADGE_ACQUIRED("배지 획득", ActorType.SYSTEM),

    PROJECT_CREATED("프로젝트 생성", ActorType.USER),
    PROJECT_UPDATED("프로젝트 수정", ActorType.USER),
    PROJECT_DELETED("프로젝트 삭제", ActorType.USER),

    APP_CONFIG_UPDATED("앱 설정 변경", ActorType.ADMIN),
    ADMIN_PROMPT_UPDATED("프롬프트 수정", ActorType.ADMIN),

    USER_FORCE_WITHDREW("회원 강제 탈퇴", ActorType.ADMIN),
    USER_MARKETING_CONSENT_UPDATED("마케팅 수신 동의 변경", ActorType.USER),
    USER_NOTIFICATION_SETTING_UPDATED("알림 설정 변경", ActorType.USER),
    USER_NIGHT_PUSH_CONSENT_UPDATED("야간 푸시 동의 변경", ActorType.USER),

    ADMIN_NOTIFICATION_SENT("알림 발송", ActorType.ADMIN),
}
