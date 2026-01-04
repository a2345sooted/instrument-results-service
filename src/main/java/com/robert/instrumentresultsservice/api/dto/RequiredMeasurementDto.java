package com.robert.instrumentresultsservice.api.dto;

import java.math.BigDecimal;

/**
 * Represents a measurement definition required by an instrument.
 */
public record RequiredMeasurementDto(
        String code,
        String name,
        String unit,
        int displayOrder,
        BigDecimal submittedValue
) {}
