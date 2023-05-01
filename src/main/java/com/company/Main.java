package com.company;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.PrintStream;
import java.util.Scanner;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Main {

    private final Scanner scanner;
    private final LoginHandler loginHandler;
    private final PrintStream printStream;

    public Main() {
        scanner = new Scanner(System.in);
        printStream = System.out;
        loginHandler = new LoginHandler(scanner, new SwedbankLoginAdapter(), printStream);
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        var retry = true;
        while (retry) {
            try {
                loginHandler.performLogin();
            } catch (IllegalStateException e) {
                printStream.println(e.getMessage());
            } catch (Exception e) {
                printStream.println("Unexpected error happened");
            }

            printStream.println("Do you want to retry? y/n");
            retry = "y".equals(scanner.nextLine());
        }
    }
}