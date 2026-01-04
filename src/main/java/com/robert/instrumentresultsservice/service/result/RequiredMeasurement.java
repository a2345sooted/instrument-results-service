package com.robert.instrumentresultsservice.service.result;

import java.math.BigDecimal;

public record RequiredMeasurement(
        String code,
        String name,
        String unit,
        int displayOrder,
        BigDecimal submittedValue
) {}
