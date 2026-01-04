package com.robert.instrumentresultsservice.api;

import com.robert.instrumentresultsservice.api.dto.CreateInstrumentRunRequest;
import com.robert.instrumentresultsservice.api.dto.CreateInstrumentRunResponse;
import com.robert.instrumentresultsservice.api.dto.SubmitMeasurementsRequest;
import com.robert.instrumentresultsservice.api.dto.SubmitMeasurementsResponse;
import com.robert.instrumentresultsservice.service.InstrumentRunService;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.service.result.MeasurementsSubmitted;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.robert.instrumentresultsservice.api.ApiConstants.CLIENT_ID_HEADER;

@RestController
@RequestMapping("/api/v1/instrument-runs")
public class InstrumentRunController {

    private final InstrumentRunService instrumentRunService;

    public InstrumentRunController(InstrumentRunService instrumentRunService) {
        this.instrumentRunService = instrumentRunService;
    }

    /**
     * Create a new instrument run.
     */
    @PostMapping
    public CreateInstrumentRunResponse createRun(
            @RequestBody @Valid CreateInstrumentRunRequest request,
            @RequestHeader(CLIENT_ID_HEADER) UUID clientId
    ) {
        InstrumentRunCreated result =
                instrumentRunService.createRun(
                        request.instrumentCode(),
                        clientId,
                        request.externalReference()
                );

        return new CreateInstrumentRunResponse(
                result.runId(),
                result.instrumentCode(),
                result.status(),
                result.createdAt()
        );
    }

    /**
     * Submit measurements for an instrument run.
     */
    @PostMapping("/{runId}/measurements")
    public SubmitMeasurementsResponse submitMeasurements(
            @PathVariable Long runId,
            @RequestBody @Valid SubmitMeasurementsRequest request,
            @RequestHeader(CLIENT_ID_HEADER) UUID clientId
    ) {
        MeasurementsSubmitted result =
                instrumentRunService.submitMeasurements(
                        runId,
                        request.measurements(),
                        clientId
                );

        return new SubmitMeasurementsResponse(
                result.instrumentRunId(),
                result.measurementCount(),
                result.submittedAt()
        );
    }
}
