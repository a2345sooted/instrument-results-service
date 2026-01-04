package com.robert.instrumentresultsservice.service.result;

import java.time.OffsetDateTime;

public record MeasurementsSubmitted(
        Long instrumentRunId,
        int measurementCount,
        OffsetDateTime submittedAt
) {}
