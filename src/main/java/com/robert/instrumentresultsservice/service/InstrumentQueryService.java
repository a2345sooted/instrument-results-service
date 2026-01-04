package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.api.dto.InstrumentSummary;

import java.util.List;

public interface InstrumentQueryService {

    List<InstrumentSummary> listActiveInstruments();
}
