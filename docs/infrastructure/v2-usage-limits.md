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
