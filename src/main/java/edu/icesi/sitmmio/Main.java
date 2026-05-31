package edu.icesi.sitmmio;

import edu.icesi.sitmmio.cli.CommandLineApp;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLineApp(System.out, System.err).run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
