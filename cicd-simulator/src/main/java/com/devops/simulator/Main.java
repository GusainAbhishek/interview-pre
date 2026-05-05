package com.devops.simulator;

import com.devops.simulator.pipeline.CiCdPipeline;
import com.devops.simulator.pipeline.PipelineFactory;
import com.devops.simulator.util.Logger;

import java.util.Scanner;

/**
 * ┌──────────────────────────────────────────────────────────┐
 *  CI/CD Pipeline Simulator — Java Edition
 *  Demonstrates: Jenkins → Docker → Kubernetes flow
 *  Problem: CrashLoopBackOff from missing env vars
 *  Solution: Secret injection via Jenkins + K8s Secrets
 * └──────────────────────────────────────────────────────────┘
 *
 * Run modes:
 *   1. Broken pipeline  → reproduces CrashLoopBackOff + auto-rollback
 *   2. Fixed pipeline   → full success with secrets injected
 *   3. Both             → side-by-side comparison
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        printWelcome();

        Scanner scanner = new Scanner(System.in);
        System.out.print("  Select mode (1/2/3): ");
        String choice = scanner.nextLine().trim();

        System.out.println();

        switch (choice) {
            case "1" -> runBroken();
            case "2" -> runFixed();
            case "3" -> {
                runBroken();
                Thread.sleep(1000);
                System.out.println();
                Logger.banner("Now running the FIXED pipeline...");
                Thread.sleep(500);
                runFixed();
            }
            default -> {
                System.out.println("  Invalid choice. Running both scenarios.");
                runBroken();
                Thread.sleep(500);
                runFixed();
            }
        }

        scanner.close();
    }

    private static void runBroken() {
        Logger.banner("SCENARIO 1 — BROKEN PIPELINE (No Secret Injection)");
        System.out.println("  Simulating: .env missing from CI, K8s deployment has no envFrom.");
        System.out.println("  Expected:   CrashLoopBackOff → auto-rollback");
        System.out.println();

        CiCdPipeline broken = PipelineFactory.createBrokenPipeline();
        broken.run(41);
    }

    private static void runFixed() {
        Logger.banner("SCENARIO 2 — FIXED PIPELINE (Secrets Injected Correctly)");
        System.out.println("  Simulating: Jenkins withCredentials + kubectl secret + envFrom: secretRef");
        System.out.println("  Expected:   All pods Running, health check passes");
        System.out.println();

        CiCdPipeline fixed = PipelineFactory.createFixedPipeline();
        fixed.run(42);
    }

    private static void printWelcome() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════════╗");
        System.out.println("  ║       CI/CD Pipeline Simulator — Java Edition           ║");
        System.out.println("  ║   Jenkins + Docker + Kubernetes + Secrets Management    ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Select a scenario:");
        System.out.println("  [1] Broken pipeline  → CrashLoopBackOff + rollback");
        System.out.println("  [2] Fixed pipeline   → Successful deploy");
        System.out.println("  [3] Both             → Full comparison");
        System.out.println();
    }
}
