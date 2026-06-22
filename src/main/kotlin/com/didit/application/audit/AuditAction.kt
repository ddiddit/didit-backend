package com.didit.application.audit

enum class AuditAction(
    val label: String,
) {
    USER_LOGGED_IN("로그인"),
    USER_LOGGED_OUT("로그아웃"),
    USER_SIGNED_UP("회원가입"),
    USER_PROFILE_UPDATED("프로필 수정"),
    USER_WITHDREW("회원 탈퇴"),

    ADMIN_LOGGED_IN("관리자 로그인"),
    ADMIN_LOGGED_OUT("관리자 로그아웃"),
    ADMIN_INVITED("관리자 초대"),
    ADMIN_APPROVED("관리자 승인"),
    ADMIN_REJECTED("관리자 거절"),
    ADMIN_DELETED("관리자 삭제"),
    ADMIN_NOTICE_EMAIL_SENT("공지 이메일 발송"),

    INQUIRY_ANSWERED("문의 답변"),
    INQUIRY_ANSWER_UPDATED("문의 답변 수정"),
    INQUIRY_ANSWER_DELETED("문의 답변 삭제"),
    INQUIRY_REGISTERED("문의 등록"),
    INQUIRY_DELETED("문의 삭제"),

    NOTICE_REGISTERED("공지 등록"),
    NOTICE_UPDATED("공지 수정"),
    NOTICE_DELETED("공지 삭제"),

    RETROSPECTIVE_STARTED("회고 시작"),
    RETROSPECTIVE_SAVED("회고 저장"),
    RETROSPECTIVE_DELETED("회고 삭제"),
    RETROSPECTIVE_RESTARTED("회고 다시 시작"),

    BADGE_ACQUIRED("배지 획득"),

    PROJECT_CREATED("프로젝트 생성"),
    PROJECT_UPDATED("프로젝트 수정"),
    PROJECT_DELETED("프로젝트 삭제"),

    APP_CONFIG_UPDATED("앱 설정 변경"),
    ADMIN_PROMPT_UPDATED("프롬프트 수정"),

    USER_FORCE_WITHDREW("회원 강제 탈퇴"),
    USER_MARKETING_CONSENT_UPDATED("마케팅 수신 동의 변경"),

    ADMIN_NOTIFICATION_SENT("알림 발송"),
}
