package com.robert.instrumentresultsservice.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request to submit measurements for a run.
 */
public record SubmitMeasurementsRequest(

        @NotNull
        @NotEmpty
        Map<String, BigDecimal> measurements

) {}
