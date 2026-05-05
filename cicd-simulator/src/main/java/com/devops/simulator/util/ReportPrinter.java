package com.devops.simulator.util;

import com.devops.simulator.model.PipelineRun;
import com.devops.simulator.model.PipelineStatus;
import com.devops.simulator.model.StageResult;

/**
 * Prints a final pipeline summary — similar to the Blue Ocean
 * or classic Jenkins build summary page.
 */
public class ReportPrinter {

    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD   = "\u001B[1m";

    public static void print(PipelineRun run) {
        System.out.println();
        System.out.println(BOLD + "  ╔══════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + "  ║              PIPELINE RUN SUMMARY                       ║" + RESET);
        System.out.println(BOLD + "  ╚══════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        System.out.printf("  %-20s %s%n", "Service:",      run.getServiceName());
        System.out.printf("  %-20s #%d%n", "Build Number:", run.getBuildNumber());
        System.out.printf("  %-20s %s%n", "Branch:",       run.getGitBranch());
        System.out.printf("  %-20s %s%n", "Commit:",       run.getCommitHash());
        System.out.printf("  %-20s %s%n", "Overall Status:", statusColored(run.getOverallStatus()));
        System.out.println();

        System.out.printf("  %-30s %-16s %-12s%n", "Stage", "Status", "Duration");
        System.out.println("  " + "─".repeat(60));

        for (StageResult sr : run.getStageResults()) {
            String statusStr = statusColored(sr.getStatus());
            System.out.printf("  %-30s %-25s %-12s%n",
                sr.getStage().getDisplayName(),
                statusStr,
                sr.getDurationMs() + "ms");
        }

        System.out.println();

        // Print logs only for failed stages
        for (StageResult sr : run.getStageResults()) {
            if (sr.getStatus() == PipelineStatus.FAILED) {
                System.out.println(RED + "  [FAILED STAGE LOGS] " + sr.getStage().getDisplayName() + RESET);
                Logger.printStageLogs(sr.getLogs());
                System.out.println();
            }
        }

        System.out.println(BOLD + "  " + "═".repeat(60) + RESET);
        System.out.println();
    }

    private static String statusColored(PipelineStatus status) {
        return switch (status) {
            case SUCCESS     -> GREEN  + "✓ SUCCESS"     + RESET;
            case FAILED      -> RED    + "✗ FAILED"      + RESET;
            case ROLLED_BACK -> YELLOW + "↩ ROLLED BACK" + RESET;
            case RUNNING     -> "⟳ RUNNING";
            case SKIPPED     -> "⊘ SKIPPED";
            default          -> "• PENDING";
        };
    }
}
