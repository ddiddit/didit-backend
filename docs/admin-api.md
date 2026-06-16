# didit 어드민 API 문서

Base URL: `https://api.didit.ai.kr`

모든 응답은 아래 형식을 따릅니다.
```json
{ "data": { ... } }
```

오류 응답:
```json
{ "type": "...", "title": "...", "status": 400, "detail": "...", "code": "..." }
```

---

## 인증

### 로그인
```
POST /api/v1/admin/auth/login
```
**인증 불필요**

Request:
```json
{ "email": "admin@example.com", "password": "password" }
```

Response:
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

---

### 토큰 갱신
```
POST /api/v1/admin/auth/refresh
```
**인증 불필요**

Request:
```json
{ "refreshToken": "eyJ..." }
```

Response:
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

---

### 로그아웃
```
POST /api/v1/admin/auth/logout
```
**권한: ADMIN 이상**  
Response: `204 No Content`

---

## 매니저 관리

### 매니저 목록 조회
```
GET /api/v1/admin
```
**권한: SUPER_ADMIN**

Response:
```json
{
  "data": [
    {
      "id": "uuid",
      "email": "admin@example.com",
      "position": "DEVELOPER",
      "role": "ADMIN",
      "status": "ACTIVE",
      "createdAt": "2026-01-01T00:00:00"
    }
  ]
}
```

---

### 매니저 초대
```
POST /api/v1/admin/invite
```
**권한: SUPER_ADMIN**

Request:
```json
{ "email": "new@example.com", "position": "DEVELOPER" }
```
- `position`: `PLANNER` | `DESIGNER` | `DEVELOPER`

Response: `204 No Content`  
> 해당 이메일로 초대 링크 발송 (48시간 유효)

---

### 매니저 등록 (초대 수락)
```
POST /api/v1/admin/register?token={uuid}
```
**인증 불필요**

Request:
```json
{ "email": "new@example.com", "password": "password" }
```

Response: `204 No Content`

---

### 매니저 승인
```
POST /api/v1/admin/{adminId}/approve
```
**권한: SUPER_ADMIN**  
Response: `204 No Content`

---

### 매니저 거절
```
POST /api/v1/admin/{adminId}/reject
```
**권한: SUPER_ADMIN**  
Response: `204 No Content`

---

### 매니저 삭제
```
DELETE /api/v1/admin/{adminId}
```
**권한: SUPER_ADMIN**  
Response: `204 No Content`

---

## 유저 관리

### 유저 목록 조회
```
GET /api/v1/admin/users
```
**권한: ADMIN 이상**

Query Parameters:
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| keyword | string | N | 이메일·닉네임 검색 |
| job | string | N | `DEVELOPER` \| `PLANNER` \| `DESIGNER` |
| isDeleted | boolean | N | 탈퇴 유저 포함 여부 |
| page | int | N | 페이지 번호 (기본값: 0) |

Response:
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "email": "user@example.com",
        "nickname": "닉네임",
        "job": "DEVELOPER",
        "age": "TWENTIES",
        "experience": "JUNIOR",
        "provider": "KAKAO",
        "createdAt": "2026-01-01T00:00:00",
        "lastLoginAt": "2026-01-01T00:00:00",
        "onboardingCompleted": true,
        "deleted": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### 유저 상세 조회
```
GET /api/v1/admin/users/{userId}
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": {
    "profile": { /* UserListResponse 동일 */ },
    "timeline": [
      {
        "action": "USER_LOGGED_IN",
        "payload": {},
        "createdAt": "2026-01-01T00:00:00"
      }
    ]
  }
}
```

---

### 유저 강제 탈퇴
```
POST /api/v1/admin/users/{userId}/force-withdraw
```
**권한: SUPER_ADMIN**  
Response: `204 No Content`

---

## 대시보드 통계

### 통계 조회
```
GET /api/v1/admin/stats
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": {
    "totalUsers": 1000,
    "newUsersToday": 5,
    "totalRetrospects": 500,
    "unansweredInquiries": 3,
    "dau": 120,
    "todayRetrospects": 20,
    "weeklyRetroTrend": [
      { "date": "2026-01-01", "count": 30 }
    ],
    "recentUsers": [
      {
        "id": "uuid",
        "email": "user@example.com",
        "nickname": "닉네임",
        "job": "DEVELOPER",
        "createdAt": "2026-01-01T00:00:00"
      }
    ],
    "recentInquiries": [
      {
        "id": "uuid",
        "type": "BUG",
        "content": "문의 내용",
        "status": "PENDING",
        "createdAt": "2026-01-01T00:00:00"
      }
    ]
  }
}
```

---

## 탈퇴 통계

### 탈퇴 통계 조회
```
GET /api/v1/admin/withdrawal-stats
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": {
    "total": 50,
    "breakdown": [
      {
        "reason": "NO_LONGER_NEEDED",
        "count": 20,
        "percentage": 40.0
      }
    ]
  }
}
```

탈퇴 사유 코드:
- `NO_LONGER_NEEDED`: 회고 기능이 필요 없어졌어요
- `MISSING_FEATURES`: 기대했던 기능이 없어요
- `SERVICE_ISSUES`: 서비스 오류나 불편한 점이 있어요
- `DIFFICULT_TO_USE`: 사용 방법이 어렵거나 잘 모르겠어요
- `SWITCHING_SERVICE`: 다른 서비스를 이용할 예정이에요
- `OTHER`: 기타

---

## 배지 관리

### 배지 목록 조회
```
GET /api/v1/admin/badges
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "첫 회고",
      "description": "첫 번째 회고를 완료했어요",
      "conditionType": "RETROSPECT_COUNT",
      "acquiredCount": 150,
      "createdAt": "2026-01-01T00:00:00"
    }
  ]
}
```

---

### 배지 보유 유저 목록
```
GET /api/v1/admin/badges/{badgeId}/holders
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": [
    {
      "userId": "uuid",
      "email": "user@example.com",
      "nickname": "닉네임",
      "acquiredAt": "2026-01-01T00:00:00"
    }
  ]
}
```

---

## 프롬프트 관리

### 프롬프트 목록 조회
```
GET /api/v1/admin/prompts
```
**권한: ADMIN 이상**

Response:
```json
{
  "data": [
    {
      "id": "uuid",
      "jobType": "DEVELOPER",
      "promptType": "DEEP_QUESTION",
      "content": "프롬프트 내용",
      "updatedAt": "2026-01-01T00:00:00",
      "updatedBy": "admin-uuid"
    }
  ]
}
```

- `jobType`: `DEVELOPER` | `PLANNER` | `DESIGNER`
- `promptType`: `DEEP_QUESTION` | `SUMMARY`

---

### 프롬프트 단건 조회
```
GET /api/v1/admin/prompts/{id}
```
**권한: ADMIN 이상**

Response: 프롬프트 단건 객체

---

### 프롬프트 수정
```
PUT /api/v1/admin/prompts/{id}
```
**권한: SUPER_ADMIN**

Request:
```json
{ "content": "수정된 프롬프트 내용" }
```

Response: 수정된 프롬프트 단건 객체

---

## 감사 로그

### 감사 로그 목록 조회
```
GET /api/v1/admin/audit-logs
```
**권한: ADMIN 이상**

Query Parameters:
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| action | string | N | 특정 액션 필터 |
| actorType | string | N | `USER` \| `ADMIN` \| `SYSTEM` |
| page | int | N | 페이지 번호 (기본값: 0) |

Response:
```json
{
  "data": {
    "content": [
      {
        "action": "ADMIN_INVITED",
        "actorId": "uuid",
        "actorType": "ADMIN",
        "targetId": "uuid",
        "targetType": "ADMIN",
        "payload": { "email": "new@example.com", "position": "DEVELOPER" },
        "createdAt": "2026-01-01T00:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "page": 0
  }
}
```

주요 `action` 값:
- `ADMIN_LOGGED_OUT`, `ADMIN_INVITED`, `ADMIN_APPROVED`, `ADMIN_REJECTED`, `ADMIN_DELETED`
- `ADMIN_PROMPT_UPDATED`
- `ADMIN_NOTICE_EMAIL_SENT`
- `USER_LOGGED_IN`, `USER_SIGNED_UP`, `USER_WITHDREW`
