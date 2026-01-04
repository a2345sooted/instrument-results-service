package com.robert.instrumentresultsservice.service;

public interface ProcessingService {

    /**
     * Kick off async processing for a run.
     */
    void processRunAsync(Long runId);
}
