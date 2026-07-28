package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedItemResponse(
        UUID id,
        String resourceType,
        UUID resourceId,
        String title,       // 저장된 리소스의 표시 제목 (업체 이름, 업데이트 제목 등)
        String slugOrId,    // 프론트엔드가 상세 페이지로 링크 걸 때 쓸 값 (slug 또는 id)
        // 지금 공개 조회로 열 수 있는지. 삭제됐거나 관리자가 비공개 전환한 경우 false.
        // 저장은 사용자의 행위이므로 기록은 남기되, 링크는 막아야 404를 피할 수 있다.
        boolean available,
        OffsetDateTime createdAt
) {}