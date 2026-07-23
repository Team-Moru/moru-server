package com.moru.server.global.ai.prompt;

public class RoutinePrompt {

    public static String judgePrompt(String userInput) {
        return """
            사용자가 루틴을 완료했는지 확인하는 중입니다.
            아래 입력이 긍정/완료 의사인지 판단하세요.
            반드시 이 JSON 형식으로만 답하세요:
            { "shouldProceed": true 또는 false, "aiResponse": "사용자에게 할 격려/안내 멘트" }

            사용자 입력: "%s"
            """.formatted(userInput);
    }
}