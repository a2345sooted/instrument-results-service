package com.robert.instrumentresultsservice.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a new instrument run.
 */
public record CreateInstrumentRunRequest(

        @NotBlank
        String instrumentCode,

        String externalReference

) {}
