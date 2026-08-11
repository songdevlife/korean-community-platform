package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;

public interface CardRendererService {

        RenderedCard renderSingle(
                CardSpec cardSpec,
                byte[] heroImage
        );
    
        /**
         * One card of a carousel, by position.
         *
         * Index 0 is the cover and is drawn exactly as a single card would be;
         * later indexes are the cards a reader swipes to. Rendering by index
         * rather than returning the whole set means a caller can show one at a
         * time, and the cover keeps whatever artwork was already generated for it.
         */
        RenderedCard renderCarouselCard(
                CardSpec cardSpec,
                byte[] heroImage,
                int index
        );
    
        record RenderedCard(
            byte[] imageBytes,
            String contentType,
            int width,
            int height
    ) {}
}