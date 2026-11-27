# v2 AI·TTS 운영 한도

[#141](https://github.com/Team-Moru/moru-server/issues/141)의 정책입니다. 설치별 한도는 앱에서 한 설치가 쓸 수 있는 양을 제한합니다. 서비스 전체 상한은 Moru v2가 Google에 보내는 요청량과 비용을 제한합니다.

## 운영 프로젝트에서 확인할 값

| 항목 | 확인할 값 | 상태 |
| --- | --- | --- |
| Gemini | 운영 `GEMINI_ROUTINE_API_KEY`가 속한 프로젝트의 사용 티어, 운영 모델의 RPM·입력 TPM·RPD | 프로젝트 실측 필요. 모델 후보는 `gemini-3.5-flash-lite` |
| Cloud Text-to-Speech | 운영 TTS 자격 증명이 사용하는 프로젝트의 Chirp 3 HD 적용 쿼터와 기존 사용량 | 프로젝트 실측 필요. Google 공개 문서의 기준은 프로젝트당 분당 200회, 요청당 5,000바이트 |
| 예산 | 두 API에 할당할 월 예산, 기존 기능의 Google 사용량, 예상 v2 유입 | 운영 예산 확인 필요 |

Gemini 한도는 프로젝트 단위이므로 같은 프로젝트의 다른 API 키와 기존 루틴 단계 생성 사용량도 고려합니다. [Gemini 프로젝트별 한도 문서](https://ai.google.dev/gemini-api/docs/rate-limits) · [Cloud TTS 할당량](https://docs.cloud.google.com/text-to-speech/quotas)

## 초기 적용안

| 항목 | 적용 단위 | 분당 | 하루 |
| --- | --- | ---: | ---: |
| AI 루틴 생성 | 익명 설치 | 2 | 5 |
| 단건 TTS 신규 합성 | 익명 설치 | 10 | 30 |
| 익명 토큰 신규 발급 | 클라이언트 IP | 30 | — |
| 익명 토큰 갱신 | 익명 설치 | 5 | — |

운영 전체 상한의 검토 시작값은 **AI 분당 10회·하루 300회**, **TTS 분당 30회·하루 300회**입니다. 실제 적용값은 프로젝트 쿼터에서 기존 기능의 사용량과 여유분을 뺀 용량, 예상 트래픽, 서비스 예산 중 가장 보수적인 기준에 맞춰 확정합니다. Gemini는 RPM 외에도 요청당 입력 토큰을 측정해 TPM 여유를 확인합니다.

- AI는 요청당 입력 500·출력 500토큰으로 가정하면 30일 동안 하루 300회 기준 약 **US$12.60**입니다. 출력에는 추론 토큰을 포함합니다. 출시 전 테스트의 실제 토큰 사용량으로 다시 계산합니다. [Gemini 3.5 Flash-Lite Standard 요금](https://aistudio.google.com/docs/pricing#gemini-3.5-flash-lite)
- TTS는 하루 300건을 모두 250자로 합성한다고 가정하면 30일 기준 225만 자, 무료 제공량 차감 전 약 **US$67.50**입니다. 공통 음원 사전 생성과 기존 기능 호출은 별도 사용량으로 확인합니다. [Cloud TTS 요금](https://cloud.google.com/text-to-speech/pricing)

## 집계와 차감

- 일일 한도는 한국 시간 자정(`Asia/Seoul`)에 초기화합니다. 분당 한도는 서버 시각 기준 60초 고정 창으로 집계합니다. Redis에서 설치별·서비스 전체 한도 검사와 차감을 원자적으로 처리합니다. 적용 한도 중 하나라도 초과하면 해당 요청은 차감하지 않습니다.
- AI는 검증을 통과하고 Gemini 호출로 진행하는 요청을 집계합니다. TTS는 캐시를 확인한 뒤 실제 합성으로 진행하는 요청만 집계합니다. 캐시 적중, 서명 URL 재발급, 다른 요청의 합성 결과를 재사용하는 요청은 신규 합성 한도에 포함하지 않습니다.
- Google 호출을 시작한 뒤 타임아웃, 공급자 오류, 파싱 실패 또는 업로드 실패가 나면 차감을 유지합니다. 외부에서 요청을 처리했는지 즉시 확정하기 어렵기 때문입니다. 검증 실패와 외부 호출 전에 종료한 요청은 신규 합성·생성 사용량에 포함하지 않습니다.
- TTS 입력은 공백만 있으면 거절하고, 원문 기준 유니코드 코드 포인트 **250개**까지 받습니다. iOS는 `unicodeScalars.count`, JVM은 `String.codePointCount`로 같은 기준을 적용합니다. 합성에는 원문 그대로 전달합니다.

## 발급과 갱신

신규 발급 제한은 **IP당 분당 30회**, 갱신 제한은 **익명 설치당 분당 5회**를 초기안으로 사용합니다. 갱신은 검증된 갱신 토큰에서 설치를 식별합니다. 설치 UUID를 다시 제출하는 것만으로 기존 설치의 토큰을 발급하지 않습니다.

운영 요청의 IP는 애플리케이션 앞의 신뢰된 프록시가 덮어쓴 값으로 판단합니다. EC2 애플리케이션 포트는 루프백에 게시되어 있으므로, [#142](https://github.com/Team-Moru/moru-server/issues/142)에서 Nginx의 실제 전달 경로를 확인하고 클라이언트가 넣은 전달 헤더로 제한을 바꿀 수 없는지 검증합니다. [운영 Compose](../../docker-compose.prod.yml) · [운영 Nginx 로그 설정](../../deploy/nginx/README.md)

## 배포 설정과 클라이언트 응답

| 설정 항목 | 적용 범위 | 연결 이슈 |
| --- | --- | --- |
| 신규 발급·갱신 RPM | 발급 IP 및 검증된 익명 설치 | [#142](https://github.com/Team-Moru/moru-server/issues/142) |
| AI RPM·일일 횟수 | 익명 설치별 및 서비스 전체 | [#143](https://github.com/Team-Moru/moru-server/issues/143) |
| TTS 신규 합성 RPM·일일 횟수와 입력 길이 | 익명 설치별 및 서비스 전체, TTS 요청 검증 | [#144](https://github.com/Team-Moru/moru-server/issues/144) |

각 구현에서 Spring 설정을 운영 Compose의 환경 변수로 주입하고, 개발·테스트 값은 운영 값과 독립적으로 관리합니다. 한도 설정은 양의 정수로 검증합니다. 실제 운영 공급자 쿼터와 비용 검토를 마치면 서비스 전체 적용값을 결정하고 배포 설정에 반영합니다.

- 앱의 설치별 한도 초과에는 HTTP 429, API별 고정 코드, `Retry-After`를 반환합니다. AI 설치별 한도 코드는 `AI_ROUTINE_QUOTA_EXCEEDED`입니다. `Retry-After`는 해당 요청이 막힌 창이 모두 풀릴 때까지 남은 초를 사용합니다.
- 서비스 전체 상한과 토큰 발급 상한에도 429와 `Retry-After`를 반환하고, 설치별 한도와 구분되는 코드는 [#140](https://github.com/Team-Moru/moru-server/issues/140)에서 맞춥니다. Google의 429·503은 공급자 오류로 처리하고, Redis 장애 시 AI 호출·신규 합성은 중단합니다.
- 운영 배포 후 Redis 차단 건수, Google에 보낸 요청량, 공급자 429·5xx, TTS 캐시 적중률과 비용을 확인합니다. 수치를 변경할 때는 기존 설정값과 변경 사유를 기록하고 테스트 환경에서 429·`Retry-After`를 검증한 뒤 배포합니다.

## 운영 적용 전 확인

- [ ] [Google AI Studio](https://aistudio.google.com/rate-limit)에서 운영 프로젝트의 모델·RPM·TPM·RPD 및 사용량 확인
- [ ] [Google Cloud 콘솔](https://console.cloud.google.com/iam-admin/quotas)에서 운영 TTS 프로젝트의 적용 쿼터와 사용량 확인
- [ ] 월 예산과 기존 호출량을 바탕으로 서비스 전체 상한 확정
- [ ] iOS에 최대 입력 길이, 한도 응답 코드와 재시도 규칙 공유
