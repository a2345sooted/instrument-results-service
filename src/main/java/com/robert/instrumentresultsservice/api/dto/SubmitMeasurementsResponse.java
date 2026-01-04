package com.robert.instrumentresultsservice.api.dto;

import java.time.OffsetDateTime;

/**
 * Response returned after successfully submitting measurements for a run.
 */
public record SubmitMeasurementsResponse(

        Long instrumentRunId,
        int measurementCount,
        OffsetDateTime submittedAt

) {}
