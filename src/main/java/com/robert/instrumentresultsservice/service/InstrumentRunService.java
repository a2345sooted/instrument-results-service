package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import com.robert.instrumentresultsservice.service.result.MeasurementsSubmitted;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface InstrumentRunService {

    InstrumentRunCreated createRun(
            String instrumentCode,
            UUID createdByClientId,
            String externalReference
    );

    MeasurementsSubmitted submitMeasurements(
            Long instrumentRunId,
            Map<String, BigDecimal> measurementsByCode,
            UUID submittedByClientId
    );

    InstrumentRunDetails getRunById(Long instrumentRunId);
}
