package site.seaunit.zhishiku.richtext.image;

import java.time.Instant;
import java.util.UUID;

public record ImageUploadResponse(
    UUID uploadId,
    UUID itemId,
    String status,
    String scanState,
    String redactionState,
    String mediaType,
    long sourceSizeBytes,
    long sizeBytes,
    String contentHash,
    int normalizedWidth,
    int normalizedHeight,
    double aspectRatio,
    String errorCode,
    Instant expiresAt) {}
