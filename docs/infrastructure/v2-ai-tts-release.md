# iOS v2 AI 및 TTS 배포 검증

## 설정

- 기존 운영 Compose의 `.env`에서 `GEMINI_ROUTINE_MODEL`을 확인합니다. 출시 후보인 `gemini-3.5-flash-lite`로 운영 프로젝트의 루틴 초안과 타이머 단계 생성을 검증합니다.
- 기존 Google Cloud TTS와 비공개 S3가 활성화되어 있는지 확인합니다. 서명 URL의 동작은 iOS 또는 별도 브라우저에서 검증합니다.
- 아래 이름은 Spring Boot 환경 변수입니다. 앱 컨테이너가 읽는 `.env`에서 설정하며, [운영 한도 정책](v2-usage-limits.md)에 따라 조정합니다.
  - 발급과 갱신은 `MORU_V2_REGISTRATIONPERMINUTE`, `MORU_V2_REFRESHPERMINUTE`를 사용합니다.
  - AI는 `MORU_V2_AIPERMINUTE`, `MORU_V2_AIPERDAY`, `MORU_V2_AIGLOBALPERMINUTE`, `MORU_V2_AIGLOBALPERDAY`를 사용합니다.
  - TTS는 `MORU_V2_TTSPERMINUTE`, `MORU_V2_TTSPERDAY`, `MORU_V2_TTSGLOBALPERMINUTE`, `MORU_V2_TTSGLOBALPERDAY`, `MORU_V2_TTSMAXCHARACTERS`를 사용합니다.
  - 캐시 조회를 포함한 단건 TTS 요청은 `MORU_V2_TTSREQUESTSPERMINUTE`(기본 설치당 60회), `MORU_V2_TTSREQUESTSGLOBALPERMINUTE`(기본 전체 180회)를 사용합니다.
- Nginx가 앱으로 보내는 위치에서 `proxy_set_header X-Moru-Client-Ip $remote_addr;`로 들어오는 헤더를 덮어쓰고, 앱 포트가 외부에 노출되지 않는지 확인합니다. 이후 `MORU_V2_TRUSTPROXYCLIENTIP=true`를 적용하고 다른 IP를 헤더에 넣어도 같은 IP의 발급 제한이 유지되는지 확인합니다. 이 설정이 꺼져 있으면 서버는 직접 연결의 주소를 사용합니다.
- Flyway가 익명 설치 저장소와 TTS 카탈로그 컬럼을 추가하고, 기존 `ttsId`를 유지하면서 v2 목소리를 반영합니다. 배포 전에 DB에 `Orus`(온기), `Leda`(산뜻), `Kore`(단정), `Alnilam`(여유)이 각각 한 행씩 있는지 확인합니다.
- 카운트다운은 공개 S3의 `tts/common/v2/countdown/<voiceCode 소문자>/`에 `5.mp3`부터 `1.mp3`까지 저장합니다. 배포 전에 공개 URL 20개를 확인합니다. Flyway V8이 `countdown_audio_prefix`를 등록하고 `selection_version`을 올립니다.

## 검증

- [ ] 익명 등록 후 AI 생성·TTS 합성을 호출하고, 가입 회원 전용 API 접근이 403인지 확인
- [ ] 토큰 회전 후 한도를 유지하고 기존 갱신 토큰을 거절하는지 확인
- [ ] AI 파싱 실패·한도 초과 화면과 iOS TTS 실패 항목 재시도 검증
- [ ] 같은 TTS를 동시에 요청했을 때 합성이 한 번만 실행되고 캐시된 음원이 다시 제공되는지 확인
- [ ] 공개 조회에 목소리 4종과 카운트다운 MP3 20개가 표시되고 iOS 오프라인 재생이 되는지 확인
- [ ] 공개 목록은 토큰 없이 조회하고, 회원 토큰으로 로그인했을 때는 기존에 선택한 음성도 목록에 포함되는지 확인
- [ ] 운영의 정상 호출과 429·5xx, 서비스 사용량 확인

배포 후 문제가 있으면 기존 배포 절차로 직전 이미지로 되돌립니다. 추가한 마이그레이션은 기존 스키마를 보존하므로 저장된 데이터를 삭제할 필요는 없습니다. 이전 이미지에서도 마이그레이션 검증이 성공하는지 테스트 환경에서 확인합니다.
