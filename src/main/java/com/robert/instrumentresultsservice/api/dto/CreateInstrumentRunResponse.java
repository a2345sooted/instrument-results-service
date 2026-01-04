package com.robert.instrumentresultsservice.api.dto;

import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;

import java.time.OffsetDateTime;

/**
 * Response returned after creating an instrument run.
 */
public record CreateInstrumentRunResponse(

        Long id,
        String instrumentCode,
        InstrumentRunStatus status,
        OffsetDateTime createdAt

) {}
