package com.robert.instrumentresultsservice;

import org.springframework.boot.SpringApplication;

public class TestInstrumentResultsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(InstrumentResultsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
