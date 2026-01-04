package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;

import java.util.UUID;

public interface InstrumentRunService {

    InstrumentRunCreated createRun(
            String instrumentCode,
            UUID createdByClientId,
            String externalReference
    );
}
