package com.robert.instrumentresultsservice.service.result;

import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;

import java.time.OffsetDateTime;

public record InstrumentRunCreated(
        Long runId,
        String instrumentCode,
        InstrumentRunStatus status,
        OffsetDateTime createdAt
) {}
