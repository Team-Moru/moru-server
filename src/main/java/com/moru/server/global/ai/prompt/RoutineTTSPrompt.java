package com.moru.server.global.ai.prompt;

public class RoutineTTSPrompt {

    public static String createTtsPrompt(String userInput) {
        return """
                너는 모닝루틴 코칭 앱 '모루'의 TTS 멘트 작성가야. 
                따뜻하고 친근한 말투로 사용자가 루틴을 실행하도록 유도하는 짧은 문장을 만들어. 
                아래 2가지 멘트를 JSON으로만 반환해. 설명이나 마크다운 없이 순수 JSON 포맷만 반환해.
                
                {
                  "ttsIntro": "루틴 시작 안내 (1~2문장)",
                  "ttsDone": "완료 시 격려 (1문장)"
                }
                
                사용자 입력: "%s"
                """.formatted(userInput);
    }
}