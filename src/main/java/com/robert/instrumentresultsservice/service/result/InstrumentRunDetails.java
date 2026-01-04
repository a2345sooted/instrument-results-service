package com.robert.instrumentresultsservice.service.result;

import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InstrumentRunDetails(
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
        String processResult,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RequiredMeasurement> requiredMeasurements
) {}
