package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.Main;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WorkerProcessLauncher {
    public void launchAndWait(PartitionWorkItem workItem, int maxGapMinutes, double maxSpeedKmh)
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.add("--distributed-worker");
        command.add("--partition");
        command.add(workItem.partitionPath().toAbsolutePath().toString());
        command.add("--partial-output");
        command.add(workItem.partialResultPath().toAbsolutePath().toString());
        command.add("--max-gap-minutes");
        command.add(Integer.toString(maxGapMinutes));
        command.add("--max-speed-kmh");
        command.add(Double.toString(maxSpeedKmh));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Path.of("").toAbsolutePath().toFile());
        processBuilder.inheritIO();
        try {
            int exitCode = processBuilder.start().waitFor();
            if (exitCode != 0) {
                throw new IOException("Distributed worker failed for partition "
                        + workItem.partitionId() + " with exit code " + exitCode + ".");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for distributed worker.", exception);
        }
    }

    private static String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        return Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
