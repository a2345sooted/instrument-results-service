package com.robert.instrumentresultsservice.api.dto;

import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.service.result.ProcessResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response returned when retrieving an instrument run by ID.
 */
public record GetInstrumentRunResponse(
        Long id,
        String instrumentCode,
        String instrumentName,
        UUID createdByClientId,
        String externalReference,
        InstrumentRunStatus status,
        OffsetDateTime measurementsSubmittedAt,
        UUID measurementsSubmittedByClientId,
        OffsetDateTime processingStartedAt,
        OffsetDateTime processingCompletedAt,
        String errorCode,
        String errorMessage,
        ProcessResult processResult,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RequiredMeasurementDto> requiredMeasurements
) {}
