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
                    pathOrNull(values.get("--partial-output")),
                    parseOptionalNonNegativeInt(values.get("--partition-id"), "--partition-id"),
                    pathOrNull(values.get("--partial-results-dir")),
                    values.get("--ice-workers"),
                    values.get("--ice-host"),
                    parsePositiveInt(values.get("--ice-port"), 10000, "--ice-port"),
                    values.get("--ice-identity")
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
                + "  --distributed-partition        Create Version 3 partition files and manifest only\n"
                + "  --distributed-master           Run Version 3 master process\n"
                + "  --distributed-worker           Run Version 3 worker process\n"
                + "  --distributed-scan-worker      Run Version 3 worker by scanning raw datagrams\n"
                + "  --distributed-merge            Merge remote scan-worker partial results\n"
                + "  --ice-master                   Run Version 3 master with remote Ice workers\n"
                + "  --ice-worker-server            Run Version 3 Ice worker server\n"
                + "  --workers <number>             Worker JVM count for distributed master\n"
                + "  --partitions <number>          Partition count for distributed master\n"
                + "  --partition-id <number>        Hash partition id for scan-worker mode\n"
                + "  --work-dir <path>              Distributed run work directory\n"
                + "  --partition <path>             Worker input partition CSV\n"
                + "  --partial-output <path>        Worker partial result CSV\n\n"
                + "  --partial-results-dir <path>   Directory containing partial result CSVs for merge mode\n"
                + "  --ice-workers <endpoints>      Semicolon-separated Ice worker proxies\n"
                + "  --ice-host <host>              Worker server bind host. Default: 0.0.0.0\n"
                + "  --ice-port <port>              Worker server TCP port. Default: 10000\n"
                + "  --ice-identity <name>          Worker object identity. Default: sitm-worker\n\n"
                + "Default mode is Version 2 Thread Pool. Version 3 uses the distributed Master-Worker pattern.\n";
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
            case "--distributed-partition":
            case "--distributed-master":
            case "--distributed-worker":
            case "--distributed-scan-worker":
            case "--distributed-merge":
            case "--ice-master":
            case "--ice-worker-server":
            case "--workers":
            case "--partitions":
            case "--partition-id":
            case "--work-dir":
            case "--partition":
            case "--partial-output":
            case "--partial-results-dir":
            case "--ice-workers":
            case "--ice-host":
            case "--ice-port":
            case "--ice-identity":
                return true;
            default:
                return false;
        }
    }

    private static boolean isKnownFlag(String option) {
        return "--distributed-partition".equals(option)
                || "--distributed-master".equals(option)
                || "--distributed-worker".equals(option)
                || "--distributed-scan-worker".equals(option)
                || "--distributed-merge".equals(option)
                || "--ice-master".equals(option)
                || "--ice-worker-server".equals(option);
    }

    private static ExecutionMode resolveMode(Map<String, String> values) {
        int selectedModes = 0;
        selectedModes += values.containsKey("--distributed-partition") ? 1 : 0;
        selectedModes += values.containsKey("--distributed-master") ? 1 : 0;
        selectedModes += values.containsKey("--distributed-worker") ? 1 : 0;
        selectedModes += values.containsKey("--distributed-scan-worker") ? 1 : 0;
        selectedModes += values.containsKey("--distributed-merge") ? 1 : 0;
        selectedModes += values.containsKey("--ice-master") ? 1 : 0;
        selectedModes += values.containsKey("--ice-worker-server") ? 1 : 0;
        if (selectedModes > 1) {
            return null;
        }
        if (values.containsKey("--distributed-partition")) {
            return ExecutionMode.DISTRIBUTED_PARTITION;
        }
        if (values.containsKey("--distributed-master")) {
            return ExecutionMode.DISTRIBUTED_MASTER;
        }
        if (values.containsKey("--distributed-worker")) {
            return ExecutionMode.DISTRIBUTED_WORKER;
        }
        if (values.containsKey("--distributed-scan-worker")) {
            return ExecutionMode.DISTRIBUTED_SCAN_WORKER;
        }
        if (values.containsKey("--distributed-merge")) {
            return ExecutionMode.DISTRIBUTED_MERGE;
        }
        if (values.containsKey("--ice-master")) {
            return ExecutionMode.ICE_MASTER;
        }
        if (values.containsKey("--ice-worker-server")) {
            return ExecutionMode.ICE_WORKER_SERVER;
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
        if (mode == ExecutionMode.DISTRIBUTED_PARTITION) {
            if (!values.containsKey("--lines")) {
                return "--lines";
            }
            if (!values.containsKey("--datagrams")) {
                return "--datagrams";
            }
            if (!values.containsKey("--work-dir")) {
                return "--work-dir";
            }
            return null;
        }
        if (mode == ExecutionMode.DISTRIBUTED_SCAN_WORKER) {
            if (!values.containsKey("--lines")) {
                return "--lines";
            }
            if (!values.containsKey("--datagrams")) {
                return "--datagrams";
            }
            if (!values.containsKey("--partial-output")) {
                return "--partial-output";
            }
            if (!values.containsKey("--partition-id")) {
                return "--partition-id";
            }
            return null;
        }
        if (mode == ExecutionMode.DISTRIBUTED_MERGE) {
            if (!values.containsKey("--lines")) {
                return "--lines";
            }
            if (!values.containsKey("--partial-results-dir")) {
                return "--partial-results-dir";
            }
            if (!values.containsKey("--output")) {
                return "--output";
            }
            return null;
        }
        if (mode == ExecutionMode.ICE_MASTER) {
            if (!values.containsKey("--lines")) {
                return "--lines";
            }
            if (!values.containsKey("--datagrams")) {
                return "--datagrams";
            }
            if (!values.containsKey("--output")) {
                return "--output";
            }
            if (!values.containsKey("--ice-workers")) {
                return "--ice-workers";
            }
            return null;
        }
        if (mode == ExecutionMode.ICE_WORKER_SERVER) {
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
