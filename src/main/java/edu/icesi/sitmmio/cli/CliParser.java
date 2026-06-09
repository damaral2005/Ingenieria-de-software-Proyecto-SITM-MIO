package edu.icesi.sitmmio.cli;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CliParser {
    public ParseResult parse(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return ParseResult.help();
            }
            if (!arg.startsWith("--")) {
                return ParseResult.error("Unexpected positional argument: " + arg);
            }
            if (!isKnownOption(arg)) {
                return ParseResult.error("Unknown option: " + arg);
            }
            if (isKnownFlag(arg)) {
                values.put(arg, "true");
                continue;
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                return ParseResult.error("Missing value for option: " + arg);
            }
            values.put(arg, args[++i]);
        }

        ExecutionMode mode = resolveMode(values);
        if (mode == null) {
            return ParseResult.error("Only one execution mode can be selected.");
        }

        String missing = firstMissingRequired(values, mode);
        if (missing != null) {
            return ParseResult.error("Missing required option: " + missing);
        }

        try {
            int workerCount = parsePositiveInt(values.get("--workers"), CliOptions.DEFAULT_WORKER_COUNT, "--workers");
            CliOptions options = new CliOptions(
                    pathOrNull(values.get("--lines")),
                    pathOrNull(values.get("--datagrams")),
                    pathOrNull(values.get("--output")),
                    values.get("--route-col"),
                    values.get("--bus-col"),
                    values.get("--timestamp-col"),
                    values.get("--latitude-col"),
                    values.get("--longitude-col"),
                    values.get("--active-route-col"),
                    parseBoolean(values.get("--datagrams-has-header"), CliOptions.DEFAULT_DATAGRAMS_HAS_HEADER,
                            "--datagrams-has-header"),
                    parseOptionalNonNegativeInt(values.get("--route-index"), "--route-index"),
                    parseOptionalNonNegativeInt(values.get("--bus-index"), "--bus-index"),
                    parseOptionalNonNegativeInt(values.get("--timestamp-index"), "--timestamp-index"),
                    parseOptionalNonNegativeInt(values.get("--latitude-index"), "--latitude-index"),
                    parseOptionalNonNegativeInt(values.get("--longitude-index"), "--longitude-index"),
                    parsePositiveDouble(values.get("--coordinate-scale"), CliOptions.DEFAULT_COORDINATE_SCALE,
                            "--coordinate-scale"),
                    parsePositiveInt(values.get("--max-gap-minutes"), CliOptions.DEFAULT_MAX_GAP_MINUTES,
                            "--max-gap-minutes"),
                    parsePositiveDouble(values.get("--max-speed-kmh"), CliOptions.DEFAULT_MAX_SPEED_KMH,
                            "--max-speed-kmh"),
                    parsePositiveInt(values.get("--threads"), CliOptions.DEFAULT_THREAD_COUNT, "--threads"),
                    mode,
                    workerCount,
                    parsePositiveInt(values.get("--partitions"), workerCount, "--partitions"),
                    pathOrNull(values.get("--work-dir")),
                    pathOrNull(values.get("--partition")),
                    pathOrNull(values.get("--partial-output"))
            );
            return ParseResult.success(options);
        } catch (IllegalArgumentException exception) {
            return ParseResult.error(exception.getMessage());
        }
    }

    public String usage() {
        return "Usage:\n"
                + "  ./gradlew run --args=\"--lines <path> --datagrams <path> --output <path> [options]\"\n\n"
                + "Required:\n"
                + "  --lines <path>                 Active routes CSV path\n"
                + "  --datagrams <path>             GPS datagrams CSV path\n"
                + "  --output <path>                Output CSV path under results/\n\n"
                + "Optional columns:\n"
                + "  --active-route-col <name>      Active route ID column in lines CSV\n"
                + "  --route-col <name>             Route ID column in datagrams CSV\n"
                + "  --bus-col <name>               Bus/vehicle ID column in datagrams CSV\n"
                + "  --timestamp-col <name>         Timestamp column in datagrams CSV\n"
                + "  --latitude-col <name>          Latitude column in datagrams CSV\n"
                + "  --longitude-col <name>         Longitude column in datagrams CSV\n\n"
                + "Headerless datagram options:\n"
                + "  --datagrams-has-header <true|false>  Default: true\n"
                + "  --route-index <0-based index>        Required when headerless\n"
                + "  --bus-index <0-based index>          Required when headerless\n"
                + "  --timestamp-index <0-based index>    Required when headerless\n"
                + "  --latitude-index <0-based index>     Required when headerless\n"
                + "  --longitude-index <0-based index>    Required when headerless\n"
                + "  --coordinate-scale <number>          Default: 1\n\n"
                + "Optional thresholds:\n"
                + "  --max-gap-minutes <number>     Default: 10\n"
                + "  --max-speed-kmh <number>       Default: 120\n\n"
                + "ThreadPool options:\n"
                + "  --threads <number>             Default: available processors\n\n"
                + "Distributed modes:\n"
                + "  --distributed-master           Run Version 3 master process\n"
                + "  --distributed-worker           Run Version 3 worker process\n"
                + "  --workers <number>             Worker JVM count for distributed master\n"
                + "  --partitions <number>          Partition count for distributed master\n"
                + "  --work-dir <path>              Distributed run work directory\n"
                + "  --partition <path>             Worker input partition CSV\n"
                + "  --partial-output <path>        Worker partial result CSV\n\n"
                + "Default mode is Version 2 Thread Pool. Version 3 uses distributed Master-Worker with Producer-Consumer file work items.\n";
    }

    private static boolean isKnownOption(String option) {
        switch (option) {
            case "--lines":
            case "--datagrams":
            case "--output":
            case "--route-col":
            case "--bus-col":
            case "--timestamp-col":
            case "--latitude-col":
            case "--longitude-col":
            case "--active-route-col":
            case "--datagrams-has-header":
            case "--route-index":
            case "--bus-index":
            case "--timestamp-index":
            case "--latitude-index":
            case "--longitude-index":
            case "--coordinate-scale":
            case "--max-gap-minutes":
            case "--max-speed-kmh":
            case "--threads":
            case "--distributed-master":
            case "--distributed-worker":
            case "--workers":
            case "--partitions":
            case "--work-dir":
            case "--partition":
            case "--partial-output":
                return true;
            default:
                return false;
        }
    }

    private static boolean isKnownFlag(String option) {
        return "--distributed-master".equals(option) || "--distributed-worker".equals(option);
    }

    private static ExecutionMode resolveMode(Map<String, String> values) {
        boolean distributedMaster = values.containsKey("--distributed-master");
        boolean distributedWorker = values.containsKey("--distributed-worker");
        if (distributedMaster && distributedWorker) {
            return null;
        }
        if (distributedMaster) {
            return ExecutionMode.DISTRIBUTED_MASTER;
        }
        if (distributedWorker) {
            return ExecutionMode.DISTRIBUTED_WORKER;
        }
        return ExecutionMode.THREAD_POOL;
    }

    private static String firstMissingRequired(Map<String, String> values, ExecutionMode mode) {
        if (mode == ExecutionMode.DISTRIBUTED_WORKER) {
            if (!values.containsKey("--partition")) {
                return "--partition";
            }
            if (!values.containsKey("--partial-output")) {
                return "--partial-output";
            }
            return null;
        }
        if (!values.containsKey("--lines")) {
            return "--lines";
        }
        if (!values.containsKey("--datagrams")) {
            return "--datagrams";
        }
        if (!values.containsKey("--output")) {
            return "--output";
        }
        return null;
    }

    private static Path pathOrNull(String value) {
        return value == null ? null : Path.of(value);
    }

    private static int parsePositiveInt(String value, int defaultValue, String optionName) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(optionName + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(optionName + " must be an integer.", exception);
        }
    }

    private static Integer parseOptionalNonNegativeInt(String value, String optionName) {
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(optionName + " must be zero or greater.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(optionName + " must be an integer.", exception);
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue, String optionName) {
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(optionName + " must be true or false.");
    }

    private static double parsePositiveDouble(String value, double defaultValue, String optionName) {
        if (value == null) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed <= 0.0) {
                throw new IllegalArgumentException(optionName + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(optionName + " must be a number.", exception);
        }
    }
}
