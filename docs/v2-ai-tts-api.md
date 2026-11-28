# iOS v2 AI 및 TTS API

모든 요청 본문은 JSON입니다. 응답은 `isSuccess`, `code`, `message`와 성공 시 `result`를 사용합니다. #140의 연동 기준은 아래와 같습니다.

| API | 인증 | 요청 | 성공 결과 |
| --- | --- | --- | --- |
| `POST /auth/anonymous` | 공개 | `{"installationId":"<UUID>"}` | `result.accessToken`, `result.refreshToken` |
| `POST /auth/anonymous/refresh` | 공개 | `{"refreshToken":"<token>"}` | 새 `result.accessToken`, `result.refreshToken` |
| `POST /routine-groups/ai-generate` | Bearer | `{"userInput":"물 마시고 스트레칭하기"}` | 기존 `result.title`, `result.description`, `result.routines` |
| `GET /tts` | 공개 | 없음 | `result.voices` |
| `POST /tts/synthesize` | 익명 Bearer | `{"ttsId":1,"text":"이제 스트레칭을 시작할게요."}` | `result.audioUrl` |

iOS는 첫 실행에서 설치 UUID를 생성해 저장하고 토큰 쌍을 안전하게 보관합니다. 만료된 액세스 토큰은 갱신 토큰으로 교체합니다. 갱신이 성공하면 응답의 두 토큰을 모두 저장합니다. 기존 갱신 토큰은 사용할 수 없으므로 동시에 여러 요청이 갱신을 필요로 해도 앱에서 한 번만 갱신합니다. 등록된 UUID의 자격 증명을 복구할 수 없으면 새 UUID로 등록합니다. 기존 UUID를 다시 등록하면 `INSTALLATION_ALREADY_REGISTERED`(409)가 반환됩니다.

## 오류와 재시도

| 상황 | HTTP | `code` |
| --- | --- | --- |
| AI 모델 응답 파싱 실패 | 502 | `AI_ROUTINE_PARSE_FAILED` |
| AI 설치 한도 초과 | 429 | `AI_ROUTINE_QUOTA_EXCEEDED` |
| TTS 설치 한도 초과 | 429 | `TTS_QUOTA_EXCEEDED` |
| 캐시 조회를 포함한 TTS 설치별 요청 한도 초과 | 429 | `TTS_REQUEST_RATE_LIMITED` |
| 토큰 요청 한도 초과 | 429 | `ANONYMOUS_AUTH_RATE_LIMITED` |
| 서비스 전체 한도 초과 | 429 | `SERVICE_CAPACITY_EXCEEDED` |
| AI 공급자 호출 실패 | 503 | `AI_ROUTINE_UPSTREAM_UNAVAILABLE` |
| 같은 TTS 합성이 진행 중 | 409 | `TTS_SYNTHESIS_IN_PROGRESS` |
| TTS 공급자 또는 저장소 오류 | 503 | `TTS_SYNTHESIS_UNAVAILABLE` |
| Redis에서 한도를 확인할 수 없음 | 503 | `QUOTA_UNAVAILABLE` |

429의 `Retry-After`는 재시도까지 남은 초입니다. 409 합성 진행 중 응답은 잠시 후 해당 항목만 재시도할 수 있습니다. TTS 문장은 기본 250 유니코드 코드 포인트까지 받습니다. 만료된 다운로드 URL은 같은 요청을 다시 보내 발급받고, 앱은 다운로드한 MP3를 로컬에 저장합니다. 캐시된 MP3는 요청 한도에만 집계합니다.

토큰 없이 `GET /tts`를 호출하면 v2 음성 4종을 반환합니다. 익명 설치 토큰이 붙거나 토큰이 만료된 경우에도 같은 공개 목록을 반환합니다. 유효한 회원 토큰으로 조회하면 기존에 선택한 음성을 확인할 수 있도록 전체 목록을 반환합니다.

`GET /tts`에는 기존 필드와 `countdownAudioUrls`가 포함됩니다. 카운트다운이 준비된 목소리는 `{"5":"<url>","4":"<url>","3":"<url>","2":"<url>","1":"<url>"}` 형식의 개별 MP3 URL을 받습니다. 음원 경로가 없으면 빈 객체를 받습니다. 앱은 `ttsId`로 합성을 요청하고 `displayName`을 표시합니다. `selectionVersion`이 바뀌면 해당 목소리의 공통 음원 캐시를 갱신합니다.
