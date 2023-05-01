package com.company;

import com.company.beans.AuthenticationCode;
import com.company.beans.AuthenticationMethod;
import com.company.beans.Links;
import com.company.beans.LoginStatus;
import com.company.beans.dto.LoginInitiationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginHandlerTest {

    private static final String USER_ID = "user id";
    private static final String BANK_ID_LOGIN_MESSAGE = "bank id login message";
    private static final String DEFAULT_USER_ID = "191212121212";
    @Mock
    public AuthenticationMethod selectedAuthMethod;
    @Mock
    private Scanner scanner;
    @Mock
    private LoginAdapter loginAdapter;
    @Mock
    private PrintStream printStream;
    @Mock
    private LoginInitiationResponse initiatedLogin;
    @Mock
    private Links links;

    private List<AuthenticationMethod> authenticationMethods = new LinkedList<>();

    @InjectMocks
    private LoginHandler loginHandler;

    @BeforeEach
    public void setUp() throws Exception {
        when(loginAdapter.getAllLoginMethods()).thenReturn(authenticationMethods);
        authenticationMethods.add(selectedAuthMethod);
        lenient().when(scanner.nextLine()).thenReturn(USER_ID);
        lenient().when(loginAdapter.initiateLogin(any(), eq(selectedAuthMethod))).thenReturn(initiatedLogin);
        lenient().when(initiatedLogin.getLinks()).thenReturn(links);
        lenient().when(selectedAuthMethod.getCode()).thenReturn(AuthenticationCode.BANKID_MOBILE);
        lenient().when(selectedAuthMethod.getMessage()).thenReturn(BANK_ID_LOGIN_MESSAGE);
    }

    @Test
    public void performLogin_shouldGetAllLoginMethods() throws Exception {
        loginHandler.performLogin();

        verify(loginAdapter).getAllLoginMethods();
    }

    @Test
    public void performLogin_shouldInitiateLogin_withSelectedLoginMethod() throws Exception {
        loginHandler.performLogin();

        verify(loginAdapter).initiateLogin(any(), eq(selectedAuthMethod));
    }

    @Test
    public void performLogin_shouldPrintAvailableLoginMethods() throws Exception {
        loginHandler.performLogin();

        verify(printStream).println("Available login methods: [%s]".formatted(BANK_ID_LOGIN_MESSAGE));
    }

    @Test
    public void performLogin_shouldThrowException_whenBankIdIsNotAvailableToSelect() throws Exception {
        when(loginAdapter.getAllLoginMethods()).thenReturn(List.of(AuthenticationMethod.builder().code(AuthenticationCode.BANKID_CARD).build()));
        assertThatThrownBy(() -> loginHandler.performLogin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("%s method is not available".formatted(AuthenticationCode.BANKID_MOBILE));
    }

    @Test
    public void performLogin_shouldAskUserForID() throws Exception {
        loginHandler.performLogin();

        verify(printStream).println("Enter user id you want to login, or just press enter to use default user");
    }

    @Test
    public void performLogin_shouldUseDefaultUserId_whenBlankValueProvided() throws Exception {
        when(scanner.nextLine()).thenReturn(" ");
        loginHandler.performLogin();

        verify(loginAdapter).initiateLogin(DEFAULT_USER_ID, selectedAuthMethod);
    }

    @Test
    public void performLogin_shouldUseProvidedUserId_whenValueProvided() throws Exception {
        when(scanner.nextLine()).thenReturn(USER_ID);
        loginHandler.performLogin();

        verify(loginAdapter).initiateLogin(USER_ID, selectedAuthMethod);
    }

    @Test
    public void performLogin_shouldNotWaitForLogin_whenIncorrectStatus() throws Exception {
        var loginStatus = LoginStatus.OUTSTANDING_TRANSACTION;
        when(initiatedLogin.getStatus()).thenReturn(loginStatus);

        loginHandler.performLogin();

        verify(printStream).println("Can not proceed with current login status: " + loginStatus);
        verifyLoginRetry(0);
    }

    @Test
    public void performLogin_shouldWaitForLogin_when3rdTimeCompleted() throws Exception {
        when(initiatedLogin.getStatus()).thenReturn(LoginStatus.CLIENT_NOT_STARTED);
        when(loginAdapter.getLoginStatus(any())).thenReturn(LoginStatus.CLIENT_NOT_STARTED, LoginStatus.CLIENT_NOT_STARTED, LoginStatus.COMPLETE);

        loginHandler.performLogin();

        verifyLoginWaitStatus(2);
        verifyLoginRetry(3);
        verify(printStream).println("Login was successfully, login status: " + LoginStatus.COMPLETE);
    }

    @Test
    public void performLogin_shouldWaitForLogin_maximum10Times() throws Exception {
        when(initiatedLogin.getStatus()).thenReturn(LoginStatus.CLIENT_NOT_STARTED);
        when(loginAdapter.getLoginStatus(any())).thenReturn(LoginStatus.CLIENT_NOT_STARTED);

        loginHandler.performLogin();

        verifyLoginWaitStatus(10);
        verifyLoginRetry(10);
    }

    private void verifyLoginWaitStatus(int wantedNumberOfInvocations) throws Exception {
        verify(printStream, times(wantedNumberOfInvocations)).println("Current login status: " + LoginStatus.CLIENT_NOT_STARTED);
    }
    private void verifyLoginRetry(int wantedNumberOfInvocations) throws Exception {
        verify(loginAdapter, times(wantedNumberOfInvocations)).getLoginStatus(any());
    }
}
