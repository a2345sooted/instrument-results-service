package com.robert.instrumentresultsservice.api;

import com.robert.instrumentresultsservice.api.dto.InstrumentSummary;
import com.robert.instrumentresultsservice.service.InstrumentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentController {

    private final InstrumentQueryService instrumentQueryService;

    public InstrumentController(InstrumentQueryService instrumentQueryService) {
        this.instrumentQueryService = instrumentQueryService;
    }

    /**
     * Returns active instruments (id, code, name).
     */
    @GetMapping
    public List<InstrumentSummary> listActiveInstruments() {
        return instrumentQueryService.listActiveInstruments();
    }
}
