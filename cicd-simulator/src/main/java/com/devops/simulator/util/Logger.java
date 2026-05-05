package com.devops.simulator.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured logger that mimics Jenkins console output style.
 * Uses ANSI colors for clear visual feedback in the terminal.
 */
public class Logger {

    // ANSI color codes
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BLUE   = "\u001B[34m";
    private static final String BOLD   = "\u001B[1m";

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void banner(String text) {
        String line = "═".repeat(60);
        System.out.println();
        System.out.println(BOLD + BLUE + "  " + line + RESET);
        System.out.println(BOLD + BLUE + "  ║  " + text + RESET);
        System.out.println(BOLD + BLUE + "  " + line + RESET);
    }

    public static void stage(String stageName) {
        System.out.println();
        System.out.println(BOLD + CYAN +
            "  ┌─── Stage: " + stageName + " ───" + RESET);
    }

    public static void step(String command) {
        System.out.println(CYAN + "  │  $ " + command + RESET);
    }

    public static void log(String message) {
        System.out.println("  │  [" + timestamp() + "] " + message);
    }

    public static void success(String message) {
        System.out.println(GREEN + "  └─ ✓ " + message + RESET);
    }

    public static void error(String message) {
        System.out.println(RED + "  └─ ✗ " + message + RESET);
    }

    public static void warn(String message) {
        System.out.println(YELLOW + "  └─ ⚠ " + message + RESET);
    }

    public static void info(String message) {
        System.out.println("  │  " + message);
    }

    public static void separator() {
        System.out.println("  " + "─".repeat(60));
    }

    public static void printStageLogs(java.util.List<String> logs) {
        for (String log : logs) {
            if (log.startsWith("[FATAL]") || log.startsWith("[ERROR]")) {
                System.out.println(RED + "  │  " + log + RESET);
            } else if (log.startsWith("[WARNING]")) {
                System.out.println(YELLOW + "  │  " + log + RESET);
            } else {
                System.out.println("  │  " + log);
            }
        }
    }

    private static String timestamp() {
        return LocalDateTime.now().format(TIME_FMT);
    }
}
