package com.nexora.documents.api.dto;

import jakarta.validation.constraints.NotBlank;

// Post-MVP Voice & AI: Request for extracting structured silk mandi bill fields from text or speech transcripts.
public record TextParseRequest(
        @NotBlank String text) {
}
