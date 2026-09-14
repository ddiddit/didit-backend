# 소셜 로그인 복구 배포 가이드

## 목표

새 Google/Kakao 애플리케이션의 소셜 식별자가 기존 값과 달라도 새 계정을 즉시 만들지 않는다.
사용자가 이메일 OTP를 통과한 뒤 서버가 기존 계정 연결, 신규 회원 생성, 수동 지원 필요 여부를 결정한다.

## API 흐름

1. `POST /api/v2/auth/social/login`
   - Google: `credentialType=ID_TOKEN`
   - Kakao: `credentialType=AUTHORIZATION_CODE`
   - 기존 `social_identities`와 일치하면 `AUTHENTICATED`와 서비스 토큰을 반환한다.
   - 일치하지 않으면 `EMAIL_VERIFICATION_REQUIRED`와 일회성 `loginSessionToken`을 반환한다.
2. `POST /api/v2/auth/social/email/start`
   - `loginSessionToken`, `email`을 전달한다.
   - 6자리 OTP를 Gmail SMTP로 발송한다.
3. `POST /api/v2/auth/social/email/verify`
   - `loginSessionToken`, `code`를 전달한다.
   - 같은 이메일의 활성 회원이 정확히 한 명이고 제공자도 같으면 기존 `users.id`에 새 식별자를 연결한다.
   - 활성 회원이 없으면 신규 회원을 생성한다.
   - 중복 이메일 또는 다른 제공자의 회원만 있으면 `SUPPORT_REQUIRED`를 반환하고 자동 병합하지 않는다.

기존 `POST /api/v1/auth/login`은 이미 등록된 식별자의 로그인만 허용한다. 모르는 식별자는
`ACCOUNT_VERIFICATION_REQUIRED`로 거절하며 더 이상 새 회원을 자동 생성하지 않는다.

## 운영 환경변수

비밀값은 저장소나 Vercel 공개 환경변수에 넣지 않고 백엔드 운영 환경에만 설정한다.

| 환경변수 | 용도 | 공개 여부 |
| --- | --- | --- |
| `OAUTH_GOOGLE_ALLOWED_CLIENT_IDS` | 허용할 Google Web/iOS Client ID. 여러 개면 쉼표로 구분 | Client ID 자체는 공개 가능 |
| `OAUTH_KAKAO_APP_ID` | Kakao 콘솔의 숫자 앱 ID | 백엔드 설정 |
| `OAUTH_KAKAO_REST_API_KEY` | Kakao 인가코드 교환용 REST API 키 | 프런트에도 동일 값 사용 가능 |
| `OAUTH_KAKAO_CLIENT_SECRET` | Kakao 토큰 교환 Client Secret | 백엔드 전용 비밀값 |
| `OAUTH_KAKAO_ALLOWED_REDIRECT_URIS` | 카카오 인가 코드 교환에 허용할 callback URI 목록(쉼표 구분) | 공개 가능 |
| `OAUTH_APPLE_ENABLED` | Apple 준비 전 `false`, 설정 완료 뒤 검증 후 `true` | 백엔드 설정 |
| `GMAIL_USERNAME` | OTP 발송 Gmail 계정 | 백엔드 전용 |
| `GMAIL_APP_PASSWORD` | Gmail 앱 비밀번호 | 백엔드 전용 비밀값 |

프런트에는 API 주소, Google Client ID, Kakao REST API 키만 둔다. Kakao Client Secret은 절대 Vercel의
`NUXT_PUBLIC_*` 변수에 넣지 않는다.

## 배포 전 DB 점검

V47은 기존 활성 회원의 `(provider, provider_id)`가 유일하다는 전제에서 식별자를 이관한다. 아래 결과가
한 행이라도 나오면 임의로 병합하지 말고 사용자 데이터를 먼저 확인한다.

```sql
SELECT provider, provider_id, COUNT(*) AS duplicate_count
FROM users
WHERE deleted_at IS NULL
  AND provider_id IS NOT NULL
GROUP BY provider, provider_id
HAVING COUNT(*) > 1;

SELECT LOWER(TRIM(email)) AS normalized_email,
       COUNT(*) AS account_count,
       GROUP_CONCAT(DISTINCT provider ORDER BY provider) AS providers
FROM users
WHERE deleted_at IS NULL
  AND email IS NOT NULL
  AND TRIM(email) <> ''
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;
```

첫 번째 쿼리는 반드시 0행이어야 한다. 두 번째 쿼리 결과는 마이그레이션을 막지는 않지만 해당 사용자가
OTP 인증을 완료하면 자동 연결 대신 `SUPPORT_REQUIRED`가 반환된다.

## 배포 순서

1. 최신 수동 DB 백업 파일의 gzip 무결성과 S3 객체를 다시 확인한다.
2. 위 중복 식별자 쿼리를 실행한다.
3. 백엔드 환경변수를 설정하되 값을 로그나 채팅에 남기지 않는다.
4. 백엔드를 배포해 Flyway V47을 적용한다.
5. 백엔드에서 Google/Kakao v2 API를 직접 스모크 테스트한다.
6. Vercel Preview에서 OTP를 포함한 로그인 전체 흐름을 확인한다.
7. `app.didit.io.kr` 프로덕션 프런트를 v2 API로 전환한다.
8. 기존 회원 1명과 신규 테스트 계정 1명으로 데이터 연결 여부를 확인한다.

## 스모크 테스트 체크리스트

- Google 기존 계정: OTP 후 기존 `users.id`가 유지되고 회고/프로젝트가 그대로 보이는가
- Kakao 기존 계정: 새 Kakao ID가 `social_identities`에 추가되고 기존 데이터가 보이는가
- 신규 계정: OTP 전에는 `users` 행이 생기지 않고 OTP 후에만 한 행이 생성되는가
- 잘못된 OTP: 5회 제한, 만료, 재사용 차단이 동작하는가
- 다른 제공자의 같은 이메일: 자동 병합 없이 `SUPPORT_REQUIRED`가 반환되는가
- Apple: 준비 전 로그인 UI가 숨겨져 있고 백엔드 `OAUTH_APPLE_ENABLED=false`인가
- CORS: `https://app.didit.io.kr`이 운영 허용 Origin에 포함되어 있는가

## 롤백 원칙

V47 적용 후에는 DB 스키마를 즉시 되돌리기보다 프런트를 이전 버전으로 되돌리고 v2 트래픽을 차단한다.
새 테이블은 기존 `users` 데이터를 파괴하지 않으므로 원인 분석 동안 유지한다. 이미 생성되거나 연결된
식별자를 삭제·수정해야 할 경우에는 `social_identities.user_id`와 실제 회원 데이터를 대조한 뒤 수동으로
처리한다.
