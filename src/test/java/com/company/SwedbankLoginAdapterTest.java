package com.company;

import com.company.beans.AuthenticationMethod;
import com.company.beans.ErrorMessages;
import com.company.beans.FieldError;
import com.company.beans.Link;
import com.company.beans.LoginStatus;
import com.company.beans.RequestMethod;
import com.company.beans.dto.AuthenticationMethodsResponse;
import com.company.beans.dto.HttpException;
import com.company.beans.dto.LoginInitiationResponse;
import com.company.beans.dto.MobileBankIDRequest;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SwedbankLoginAdapterTest {

    private static final String USER_ID = "191010101010";
    private static final AuthenticationMethod LOGIN_METHOD = AuthenticationMethod.builder().location(new Link("POST", "uri")).build();
    private static final String RESPONSE_BODY = "response body";
    private static final String REQUEST_BODY = "request body";
    private static final Link GET_LINK = new Link(RequestMethod.GET.toString(), "/request/path");

    @Mock
    private Gson gson;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private HttpHeaders httpHeader;
    @Mock
    private HttpException httpException;
    @Mock
    private ErrorMessages errorMessages;
    @Mock
    private FieldError fieldError1;
    @Mock
    private FieldError fieldError2;
    @Mock
    private AuthenticationMethodsResponse methodsResponse;

    @Captor
    ArgumentCaptor<HttpRequest> requestCaptor;

    @InjectMocks
    private SwedbankLoginAdapter loginAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(httpHeader);
        when(httpResponse.body()).thenReturn(RESPONSE_BODY);
        when(httpResponse.statusCode()).thenReturn(200);
    }

    @Test
    public void getAllLoginMethods_shouldGetAuthenticationMethods() throws Exception {
        mockForGetAllLoginMethods();
        var expected = Mockito.mock(List.class);
        when(methodsResponse.getAuthenticationMethods()).thenReturn(expected);

        var methods = loginAdapter.getAllLoginMethods();

        assertThat(methods).isEqualTo(expected);
    }

    @Test
    public void getLoginStatus_shouldGetStatusOfLogin() throws Exception {
        LoginStatus expectedStatus = LoginStatus.CLIENT_NOT_STARTED;
        var loginInitiationResponse = getMockedLoginInitiationResponse();
        when(loginInitiationResponse.getStatus()).thenReturn(expectedStatus);

        var loginStatus = loginAdapter.getLoginStatus(GET_LINK);

        assertThat(loginStatus).isEqualTo(expectedStatus);
    }

    @Test
    public void initiateLogin_shouldGetCorrectlyMapResponse() throws Exception {
        var loginInitiationResponse = getMockedLoginInitiationResponse();
        mockForLoginInitiation();

        var response = loginAdapter.initiateLogin(USER_ID, LOGIN_METHOD);

        assertThat(response).isEqualTo(loginInitiationResponse);
    }

    @Test
    public void getAllLoginMethods_shouldCallWithCorrectlyFormedRequest() throws Exception {
        mockForGetAllLoginMethods();
        loginAdapter.getAllLoginMethods();

        verifyRequest(new Link(RequestMethod.GET.toString(), "/v5/identification/"));
    }

    @Test
    public void getLoginStatus_shouldCallWithCorrectlyFormedRequest() throws Exception {
        getMockedLoginInitiationResponse();

        loginAdapter.getLoginStatus(GET_LINK);

        verifyRequest(GET_LINK);
    }

    @Test
    public void initiateLogin_shouldCallWithCorrectlyFormedRequest() throws Exception {
        mockForLoginInitiation();

        loginAdapter.initiateLogin(USER_ID, LOGIN_METHOD);

        verifyRequest(LOGIN_METHOD.getLocation(), Optional.of(REQUEST_BODY));
    }

    @ParameterizedTest
    @MethodSource("provideAdapterCalls")
    public void allMethods_shouldMapNot2xxStatusCode_toException(ThrowableConsumer<SwedbankLoginAdapter> methodConsumer) {
        when(httpResponse.statusCode()).thenReturn(400);
        lenient().when(gson.toJson(any(MobileBankIDRequest.class))).thenReturn(REQUEST_BODY);
        when(gson.fromJson(any(String.class), eq(HttpException.class))).thenReturn(httpException);
        when(httpException.getErrorMessages()).thenReturn(errorMessages);
        when(errorMessages.getFields()).thenReturn(List.of(fieldError1, fieldError2));
        var firstError = "Bad error";
        var secondError = "Worse error";
        when(fieldError1.getMessage()).thenReturn(firstError);
        when(fieldError2.getMessage()).thenReturn(secondError);

        assertThatThrownBy(() -> methodConsumer.accept(loginAdapter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("%s, %s".formatted(firstError, secondError));
    }

    private LoginInitiationResponse getMockedLoginInitiationResponse() {
        var loginInitiationResponse = Mockito.mock(LoginInitiationResponse.class);
        when(gson.fromJson(RESPONSE_BODY, LoginInitiationResponse.class)).thenReturn(loginInitiationResponse);
        return loginInitiationResponse;
    }

    private void mockForLoginInitiation() {
        when(gson.toJson(any(MobileBankIDRequest.class))).thenReturn(REQUEST_BODY);
    }

    private void mockForGetAllLoginMethods() {
        when(gson.fromJson(any(String.class), eq(AuthenticationMethodsResponse.class))).thenReturn(methodsResponse);
    }

    private void verifyRequest(Link link) throws IOException, InterruptedException {
        verifyRequest(link, Optional.empty());
    }

    private void verifyRequest(Link link, Optional<String> requestBody) throws IOException, InterruptedException {
        verify(httpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
        assertThat(requestCaptor.getValue())
                .returns(link.getMethod(), HttpRequest::method)
                .returns("online.swedbank.se", request -> request.uri().getHost())
                .returns("/TDE_DAP_Portal_REST_WEB/api" + link.getUri(), request -> request.uri().getPath());

        requestBody.ifPresent(body ->
                assertThat(requestCaptor.getValue().bodyPublisher().orElseThrow().contentLength())
                        .isEqualTo(HttpRequest.BodyPublishers.ofString(body).contentLength()));
    }

    private static Stream<Arguments> provideAdapterCalls() {
        return Stream.of(
                Arguments.of(getAllMethods),
                Arguments.of(initiateLogin),
                Arguments.of(getLoginStatus)
        );
    }

    private final static ThrowableConsumer<SwedbankLoginAdapter> getAllMethods = SwedbankLoginAdapter::getAllLoginMethods;
    private final static ThrowableConsumer<SwedbankLoginAdapter> initiateLogin = (loginAdapter) -> loginAdapter.initiateLogin(USER_ID, LOGIN_METHOD);
    private final static ThrowableConsumer<SwedbankLoginAdapter> getLoginStatus = (loginAdapter) -> loginAdapter.getLoginStatus(GET_LINK);

    interface ThrowableConsumer<T> {
        void accept(T t) throws Exception;
    }
}
