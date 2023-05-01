package com.company;

import com.company.beans.AuthenticationMethod;
import com.company.beans.FieldError;
import com.company.beans.RequestMethod;
import com.company.beans.dto.AuthenticationMethodsResponse;
import com.company.beans.Link;
import com.company.beans.LoginStatus;
import com.company.beans.dto.HttpException;
import com.company.beans.dto.MobileBankIDRequest;
import com.company.beans.dto.LoginInitiationResponse;
import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SwedbankLoginAdapter implements LoginAdapter {

    private static final String BASE_PATH = "https://online.swedbank.se/TDE_DAP_Portal_REST_WEB/api";
    private static final Link LOGIN_METHODS_LINK = Link.builder().method(RequestMethod.GET.toString()).uri("/v5/identification/").build();
    private final Gson gson;
    private final HttpClient httpClient;

    public SwedbankLoginAdapter() {
        gson = new Gson();
        httpClient = HttpClient.newHttpClient();
    }

    private final Map<String, String> headers = new HashMap<>() {{
        put("Authorization", "QjdkWkhRY1k3OFZSVno5bDoxNTkyMjI3MzYxNzA2");
        put("X-Client", "loginititatorapp");
        put("Content-Type", "application/json");
    }};

    @Override
    public List<AuthenticationMethod> getAllLoginMethods() throws URISyntaxException, IOException, InterruptedException {
        HttpResponse<String> response = performRequest(LOGIN_METHODS_LINK, Optional.empty());

        return parseResponse(response, AuthenticationMethodsResponse.class).getAuthenticationMethods();
    }

    @Override
    public LoginInitiationResponse initiateLogin(String userId, AuthenticationMethod method) throws URISyntaxException, IOException, InterruptedException {
        var requestBody = gson.toJson(MobileBankIDRequest.builder().userId(userId).build());

        HttpResponse<String> response = performRequest(method.getLocation(), Optional.of(HttpRequest.BodyPublishers.ofString(requestBody)));

        return parseResponse(response, LoginInitiationResponse.class);
    }

    @Override
    public LoginStatus getLoginStatus(Link link) throws URISyntaxException, IOException, InterruptedException {
        HttpResponse<String> response = performRequest(link, Optional.empty());

        return parseResponse(response, LoginInitiationResponse.class).getStatus();
    }

    private HttpResponse<String> performRequest(Link link, Optional<HttpRequest.BodyPublisher> requestBody) throws URISyntaxException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder().uri(new URI(BASE_PATH + link.getUri()));

        headers.forEach(request::header);

        requestMethodConsumer(link.getMethod()).accept(request, requestBody);

        var response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());

        updateSessionCookie(response);

        return response;
    }


    private BiConsumer<HttpRequest.Builder, Optional<HttpRequest.BodyPublisher>> requestMethodConsumer(String method) {
        if (RequestMethod.POST.toString().equals(method)) {
            return (builder, body) -> builder.POST(body.orElse(HttpRequest.BodyPublishers.noBody()));
        } else if (RequestMethod.GET.toString().equals(method)) {
            return (builder, body) -> builder.GET();
        }

        throw new IllegalStateException("%s request method is not supported".formatted(method));
    }

    private void updateSessionCookie(HttpResponse<?> response) {
        Optional.ofNullable(response.headers().map().get("set-cookie"))
                .flatMap(setCookie -> setCookie.stream()
                        .flatMap(cookies -> Arrays.stream(cookies.split(";")))
                        .filter(cookie -> cookie.contains("JSESSIONID"))
                        .findAny()).ifPresent(cookie -> headers.put("Cookie", cookie));
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> responseType) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return gson.fromJson(response.body(), responseType);
        } else {
            String errors = gson.fromJson(response.body(), HttpException.class).getErrorMessages().getFields()
                    .stream().map(FieldError::getMessage).collect(Collectors.joining(", "));

            throw new IllegalStateException(errors);
        }
    }

}
