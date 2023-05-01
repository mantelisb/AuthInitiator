package com.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintStream;
import java.util.Scanner;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MainTest {

    private static final String RETRY_MESSAGE = "Do you want to retry? y/n";
    @Mock
    private Scanner scanner;
    @Mock
    private LoginHandler loginHandler;
    @Mock
    private PrintStream printStream;

    @InjectMocks
    private Main main;

    @Test
    public void shouldAlwaysCallLoginHandler() throws Exception {
        main.run();

        verify(loginHandler).performLogin();
        verifyRetryMessage();
    }

    @Test
    public void shouldPrintErrorMessage_whenIllegalStateExceptionOccurs() throws Exception {
        String errorMessage = "error message";
        doThrow(new IllegalStateException(errorMessage)).when(loginHandler).performLogin();

        main.run();

        verify(printStream).println(errorMessage);
        verifyRetryMessage();
    }

    @Test
    public void shouldPrintUnexpectedErrorMessage_whenExceptionOccurs() throws Exception {
        doThrow(new Exception()).when(loginHandler).performLogin();

        main.run();

        verify(printStream).println("Unexpected error happened");
        verifyRetryMessage();
    }

    @Test
    public void shouldPerformLogin4Times_whenUserAsks3Retries() throws Exception {
        when(scanner.nextLine()).thenReturn("y", "y", "y", "n");
        main.run();

        int invocations = 4;
        verify(loginHandler, times(invocations)).performLogin();
        verify(printStream, times(invocations)).println(RETRY_MESSAGE);
    }

    private void verifyRetryMessage() {
        verify(printStream).println(RETRY_MESSAGE);
    }

}