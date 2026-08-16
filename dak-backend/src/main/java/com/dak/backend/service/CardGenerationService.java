package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;

/**
 * Converts finished DAK content into a specification for a visual card.
 *
 * The input must be DAK's own reviewed/publishable content rather than
 * raw source material. Implementations decide what short text and visual
 * concept should appear on the card, but do not render images themselves.
 */
public interface CardGenerationService {

    CardSpec generateForAustraliaUpdate(String title, String sourceContent);

    CardSpec generateForGuide(String title, String summary, String body);
}