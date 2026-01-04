package com.robert.instrumentresultsservice.api;

import com.robert.instrumentresultsservice.api.dto.*;
import com.robert.instrumentresultsservice.service.InstrumentRunService;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import com.robert.instrumentresultsservice.service.result.MeasurementsSubmitted;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /**
     * Get an instrument run by ID.
     */
    @GetMapping("/{runId}")
    public GetInstrumentRunResponse getRunById(@PathVariable Long runId) {
        InstrumentRunDetails result = instrumentRunService.getRunById(runId);

        List<RequiredMeasurementDto> requiredMeasurements = result.requiredMeasurements()
                .stream()
                .map(rm -> new RequiredMeasurementDto(
                        rm.code(),
                        rm.name(),
                        rm.unit(),
                        rm.displayOrder(),
                        rm.submittedValue()
                ))
                .toList();

        return new GetInstrumentRunResponse(
                result.id(),
                result.instrumentCode(),
                result.instrumentName(),
                result.createdByClientId(),
                result.externalReference(),
                result.status(),
                result.measurementsSubmittedAt(),
                result.measurementsSubmittedByClientId(),
                result.processingStartedAt(),
                result.processingCompletedAt(),
                result.errorCode(),
                result.errorMessage(),
                result.processResult(),
                result.createdAt(),
                result.updatedAt(),
                requiredMeasurements
        );
    }
}
