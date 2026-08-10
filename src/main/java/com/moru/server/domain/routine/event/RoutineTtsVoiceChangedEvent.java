package com.moru.server.domain.routine.event;

public record RoutineTtsVoiceChangedEvent(
        Long routineTtsId,
        String voiceName,
        Long voiceVersion) {
}
