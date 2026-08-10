package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;

public interface CardRendererService {

    RenderedCard renderSingle(
            CardSpec cardSpec,
            byte[] heroImage
    );

    record RenderedCard(
            byte[] imageBytes,
            String contentType,
            int width,
            int height
    ) {}
}