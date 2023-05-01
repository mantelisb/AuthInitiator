package com.company;

import com.company.beans.AuthenticationCode;
import com.company.beans.AuthenticationMethod;
import com.company.beans.LoginStatus;
import com.company.beans.dto.LoginInitiationResponse;
import lombok.AllArgsConstructor;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Scanner;

@AllArgsConstructor
public class LoginHandler {

    private static final String DEFAULT_USER_ID = "191212121212";
    public static final AuthenticationCode SELECTED_AUTH_METHOD = AuthenticationCode.BANKID_MOBILE;
    private final Scanner scanner;
    private final LoginAdapter loginAdapter;
    private final PrintStream printStream;

    public void performLogin() throws Exception {
        AuthenticationMethod loginMethod = getLoginMethod();

        String userId = getUserId();

        var initiatedLogin = loginAdapter.initiateLogin(userId, loginMethod);
        checkLoginStatus(initiatedLogin);

    }

    private AuthenticationMethod getLoginMethod() throws Exception {
        var methods = loginAdapter.getAllLoginMethods();
        printStream.println("Available login methods: " + methods.stream().map(AuthenticationMethod::getMessage).toList());

        return methods.stream()
                .filter(method -> SELECTED_AUTH_METHOD.equals(method.getCode()))
                .findAny().orElseThrow(() -> new IllegalStateException("%s method is not available".formatted(SELECTED_AUTH_METHOD)));
    }

    private String getUserId() {
        printStream.println("Enter user id you want to login, or just press enter to use default user");
        var userId = scanner.nextLine();
        return userId.isBlank() ? DEFAULT_USER_ID : userId;
    }

    private void checkLoginStatus(LoginInitiationResponse initiatedLogin) throws Exception {
        if (LoginStatus.CLIENT_NOT_STARTED.equals(initiatedLogin.getStatus())) {

            for (int c = 0; c < 10; c++) {
                Thread.sleep(Duration.ofSeconds(1));
                var currentLoginStatus = loginAdapter.getLoginStatus(initiatedLogin.getLinks().getNext());

                if (LoginStatus.COMPLETE.equals(currentLoginStatus)) {
                    printStream.println("Login was successfully, login status: " + currentLoginStatus);
                    break;
                }

                printStream.println("Current login status: " + currentLoginStatus);
            }

        } else {
            printStream.println("Can not proceed with current login status: " + initiatedLogin.getStatus());
        }
    }
}
