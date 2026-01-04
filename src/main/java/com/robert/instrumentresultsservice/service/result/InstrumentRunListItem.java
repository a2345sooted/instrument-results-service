package com.robert.instrumentresultsservice.service.result;

import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;

import java.time.OffsetDateTime;

public record InstrumentRunListItem(
        Long id,
        String externalReference,
        InstrumentRunStatus status,
        OffsetDateTime createdAt
) {}
