package com.moru.server.global.idempotency;

public interface DeletedResourceTombstoneService {
    void markDeleted(String resourceType, Long resourceId, Long ownerId);
    boolean wasDeletedBy(String resourceType, Long resourceId, Long ownerId);
}
