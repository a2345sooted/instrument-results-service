package com.robert.instrumentresultsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.service.result.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceImplTest {

    @Mock InstrumentRunRepository instrumentRunRepository;
    @Mock InstrumentRunEventRepository instrumentRunEventRepository;

    @Captor ArgumentCaptor<InstrumentRunEvent> eventCaptor;

    @Test
    void processRunAsync_runNotFound_doesNothing() {
        // Arrange
        Long runId = 123L;
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.empty());

        ProcessingServiceImpl service = new ProcessingServiceImpl(
                instrumentRunRepository,
                instrumentRunEventRepository,
                new ObjectMapper(),
                1L
        );

        // Act
        service.processRunAsync(runId);

        // Assert
        verify(instrumentRunRepository).findById(runId);
        verifyNoMoreInteractions(instrumentRunRepository);
        verifyNoInteractions(instrumentRunEventRepository);
    }

    @Test
    void processRunAsync_success_setsProcessingThenSucceeded_setsProcessResult_andEmitsStartedAndCompleted() {
        // Arrange
        Long runId = 123L;

        InstrumentRun run = new InstrumentRun();
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        ProcessingServiceImpl service = new ProcessingServiceImpl(
                instrumentRunRepository,
                instrumentRunEventRepository,
                new ObjectMapper(),
                1L // tiny delay
        );

        // Act
        service.processRunAsync(runId);

        // Assert: final state SUCCEEDED
        assertEquals(InstrumentRunStatus.SUCCEEDED, run.getStatus());
        assertNotNull(run.getProcessingStartedAt());
        assertNotNull(run.getProcessingCompletedAt());
        assertNull(run.getErrorCode());
        assertNull(run.getErrorMessage());

        ProcessResult pr = run.getProcessResult();
        assertNotNull(pr);
        assertEquals("stub-result", pr.result());

        // Saved at least twice: once for PROCESSING, once for SUCCEEDED
        verify(instrumentRunRepository, atLeast(2)).save(same(run));

        // Events: STARTED then COMPLETED
        verify(instrumentRunEventRepository, times(2)).save(eventCaptor.capture());
        InstrumentRunEvent first = eventCaptor.getAllValues().get(0);
        InstrumentRunEvent second = eventCaptor.getAllValues().get(1);

        assertEquals(InstrumentRunEventType.PROCESSING_STARTED, first.getEventType());
        assertSame(run, first.getInstrumentRun());
        assertNull(first.getDetails());

        assertEquals(InstrumentRunEventType.PROCESSING_COMPLETED, second.getEventType());
        assertSame(run, second.getInstrumentRun());
        assertNull(second.getDetails());
    }

    @Test
    void processRunAsync_interrupted_marksFailed_andEmitsStartedAndFailed_withoutWaiting() {
        // Arrange
        Long runId = 123L;

        InstrumentRun run = new InstrumentRun();
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        ProcessingServiceImpl service = new ProcessingServiceImpl(
                instrumentRunRepository,
                instrumentRunEventRepository,
                new ObjectMapper(),
                10_000L // big delay; we interrupt so it doesn't wait
        );

        Thread.currentThread().interrupt();

        try {
            // Act
            service.processRunAsync(runId);

            // Assert: final state FAILED
            assertEquals(InstrumentRunStatus.FAILED, run.getStatus());
            assertNotNull(run.getProcessingStartedAt());
            assertNotNull(run.getProcessingCompletedAt());
            assertEquals("INTERRUPTED", run.getErrorCode());
            assertEquals("Processing thread interrupted", run.getErrorMessage());

            // Events: STARTED then FAILED
            verify(instrumentRunEventRepository, times(2)).save(eventCaptor.capture());
            InstrumentRunEvent first = eventCaptor.getAllValues().get(0);
            InstrumentRunEvent second = eventCaptor.getAllValues().get(1);

            assertEquals(InstrumentRunEventType.PROCESSING_STARTED, first.getEventType());
            assertNull(first.getDetails());

            assertEquals(InstrumentRunEventType.PROCESSING_FAILED, second.getEventType());
            assertNotNull(second.getDetails());

            ObjectNode details = (ObjectNode) second.getDetails();
            assertEquals("INTERRUPTED", details.get("errorCode").asText());
            assertEquals("Processing thread interrupted", details.get("errorMessage").asText());
        } finally {
            // Clear interrupt flag so other tests aren't affected
            Thread.interrupted();
        }
    }

    @Test
    void processRunAsync_unexpectedException_marksFailed_andEmitsStartedAndFailed() {
        // Arrange
        Long runId = 123L;

        InstrumentRun run = new InstrumentRun();
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        // 1st save ok (PROCESSING), 2nd save throws (SUCCEEDED), 3rd save ok (markFailed)
        when(instrumentRunRepository.save(same(run)))
                .thenReturn(run)
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(run);

        ProcessingServiceImpl service = new ProcessingServiceImpl(
                instrumentRunRepository,
                instrumentRunEventRepository,
                new ObjectMapper(),
                0L // no delay in unit tests
        );

        // Act
        service.processRunAsync(runId);

        // Assert: final state FAILED
        assertEquals(InstrumentRunStatus.FAILED, run.getStatus());
        assertNotNull(run.getProcessingStartedAt());
        assertNotNull(run.getProcessingCompletedAt());
        assertEquals("UNEXPECTED_ERROR", run.getErrorCode());
        assertEquals("boom", run.getErrorMessage());

        // Assert: events STARTED then FAILED
        verify(instrumentRunEventRepository, times(2)).save(eventCaptor.capture());
        InstrumentRunEvent first = eventCaptor.getAllValues().get(0);
        InstrumentRunEvent second = eventCaptor.getAllValues().get(1);

        assertEquals(InstrumentRunEventType.PROCESSING_STARTED, first.getEventType());
        assertNull(first.getDetails());

        assertEquals(InstrumentRunEventType.PROCESSING_FAILED, second.getEventType());
        assertNotNull(second.getDetails());

        ObjectNode details = (ObjectNode) second.getDetails();
        assertEquals("UNEXPECTED_ERROR", details.get("errorCode").asText());
        assertEquals("boom", details.get("errorMessage").asText());
    }

}
