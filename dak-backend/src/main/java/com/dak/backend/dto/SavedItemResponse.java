package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedItemResponse(
        UUID id,
        String resourceType,
        UUID resourceId,
        String title,       // 저장된 리소스의 표시 제목 (업체 이름, 업데이트 제목 등)
        String slugOrId,    // 프론트엔드가 상세 페이지로 링크 걸 때 쓸 값 (slug 또는 id)
        OffsetDateTime createdAt
) {}