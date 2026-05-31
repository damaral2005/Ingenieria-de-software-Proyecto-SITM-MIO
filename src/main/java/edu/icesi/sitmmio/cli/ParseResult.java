package edu.icesi.sitmmio.cli;

public final class ParseResult {
    private final boolean valid;
    private final boolean helpRequested;
    private final CliOptions options;
    private final String errorMessage;

    private ParseResult(boolean valid, boolean helpRequested, CliOptions options, String errorMessage) {
        this.valid = valid;
        this.helpRequested = helpRequested;
        this.options = options;
        this.errorMessage = errorMessage;
    }

    public static ParseResult success(CliOptions options) {
        return new ParseResult(true, false, options, null);
    }

    public static ParseResult help() {
        return new ParseResult(false, true, null, null);
    }

    public static ParseResult error(String message) {
        return new ParseResult(false, false, null, message);
    }

    public boolean valid() {
        return valid;
    }

    public boolean helpRequested() {
        return helpRequested;
    }

    public CliOptions options() {
        return options;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
