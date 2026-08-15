# 회원탈퇴 기능 배포 체크리스트

회원탈퇴 기능은 Apple, S3, MySQL, Redis를 함께 사용한다. 아래 순서대로 운영 환경을 준비한 뒤 애플리케이션을 배포한다.

## 1. 운영 DB 스키마 반영

운영 환경은 `ddl-auto=validate`이므로 애플리케이션 배포 전에 다음 SQL을 실행한다.

```text
docs/sql/2026-08-15-create-apple-oauth-credentials.sql
```

적용 후 `apple_oauth_credentials` 테이블과 `member_id` UNIQUE/FK 제약조건을 확인한다.

## 2. Apple 설정값 등록

다음 값은 실제 값을 저장소에 커밋하지 않고 운영 서버 환경변수 또는 AWS Secrets Manager를 통해 주입한다.

```env
OAUTH_APPLE_CLIENT_ID=
OAUTH_APPLE_TEAM_ID=
OAUTH_APPLE_KEY_ID=
OAUTH_APPLE_PRIVATE_KEY=
APPLE_TOKEN_ENCRYPTION_KEY=
```

- `OAUTH_APPLE_PRIVATE_KEY`에는 Apple에서 받은 `.p8` 키를 등록한다.
- `APPLE_TOKEN_ENCRYPTION_KEY`는 `openssl rand -base64 32`로 별도 생성한다.
- 개발과 운영 환경의 암호화 키를 분리한다.
- 운영 암호화 키를 잃으면 저장된 Apple Refresh Token을 복호화할 수 없다.

## 3. S3 및 IAM 설정

운영 환경에 다음 값을 설정한다.

```env
AWS_S3_ENABLED=true
AWS_S3_REGION=ap-northeast-2
AWS_S3_BUCKET=moru-prod-assets-488230509502
```

EC2에는 `moru-server-s3-role`을 연결하고 버킷과 객체 권한을 구분하여 다음 정책을 부여한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::moru-prod-assets-488230509502",
      "Condition": {
        "StringLike": {
          "s3:prefix": ["tts/*", "profiles/*"]
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": [
        "arn:aws:s3:::moru-prod-assets-488230509502/tts/*",
        "arn:aws:s3:::moru-prod-assets-488230509502/profiles/*"
      ]
    }
  ]
}
```

`s3:ListBucket`은 버킷 ARN에, 객체 조회·생성·삭제 권한은 객체 ARN에 적용한다. EC2 IAM Role을 사용하는 운영 환경에서는 `AWS_CREDENTIALS_ACCESS_KEY`, `AWS_CREDENTIALS_SECRET_KEY`를 비워 둔다.

현재 삭제 구현은 버전 관리가 비활성화된 버킷을 기준으로 한다. 버킷 버전 관리가 활성화되어 있다면 출시 전에 모든 객체 버전과 삭제 마커를 제거하는 구현 및 다음 권한을 추가해야 한다.

```text
s3:ListBucketVersions
s3:DeleteObjectVersion
```

## 4. 배포 순서

1. 운영 DB DDL 적용
2. Apple 및 암호화 환경변수 등록
3. EC2 IAM Role과 S3 버전 관리 상태 확인
4. 애플리케이션 배포
5. Apple 신규 로그인과 기존 회원 재로그인 확인
6. 테스트 회원으로 회원탈퇴 실행

기존 `moru:deleted:{resourceType}:{resourceId}` 형식의 Redis tombstone이 있다면 배포 작업에서 일회성 배치로 정리한다. 신규 tombstone은 `moru:deleted:{memberId}:{resourceType}:{resourceId}` 형식으로 저장되어 회원탈퇴 시 회원 prefix만 삭제된다.

## 5. 배포 후 확인

- Apple 로그인 시 `authorizationCode`가 서버로 전달되는지 확인
- `apple_oauth_credentials.encrypted_refresh_token`에 평문이 저장되지 않는지 확인
- Apple 회원탈퇴 후 Apple 연결이 해제되는지 확인
- 회원 관련 DB 행이 모두 삭제되는지 확인
- Redis Refresh Token, 멱등성 캐시, tombstone이 삭제되는지 확인
- S3의 프로필 이미지와 TTS MP3가 삭제되는지 확인
- 동일한 Access Token으로 탈퇴를 재요청했을 때 `200 COMPLETED`가 반환되는지 확인
- 동시에 탈퇴를 요청했을 때 하나의 요청만 처리되고 나머지는 `409 MEMBER4091`이 반환되는지 확인
