package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.service.result.InstrumentRunListItem;

import java.util.List;

public interface InstrumentRunQueryService {

    List<InstrumentRunListItem> getAllRuns();
}
