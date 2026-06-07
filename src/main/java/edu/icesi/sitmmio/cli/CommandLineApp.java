package edu.icesi.sitmmio.cli;

import edu.icesi.sitmmio.service.ThreadPoolRunSummary;
import edu.icesi.sitmmio.service.ThreadPoolSpeedCalculator;

import java.io.IOException;
import java.io.PrintStream;

public final class CommandLineApp {
    private final PrintStream out;
    private final PrintStream err;
    private final CliParser parser;
    private final ThreadPoolSpeedCalculator calculator;

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.parser = new CliParser();
        this.calculator = new ThreadPoolSpeedCalculator();
    }

    public int run(String[] args) {
        ParseResult result = parser.parse(args);
        if (result.helpRequested()) {
            out.print(parser.usage());
            return 0;
        }
        if (!result.valid()) {
            err.println(result.errorMessage());
            err.println();
            err.print(parser.usage());
            return 2;
        }

        try {
            ThreadPoolRunSummary summary = calculator.run(result.options());
            out.print(summary.formatForConsole());
            return 0;
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Failed to run ThreadPool speed calculation: " + exception.getMessage());
            return 1;
        }
    }
}
