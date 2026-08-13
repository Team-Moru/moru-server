package com.moru.server.domain.routine.event;

public record RoutineTtsCreatedEvent(
        Long routineTtsId,
        String voiceName) {
}
