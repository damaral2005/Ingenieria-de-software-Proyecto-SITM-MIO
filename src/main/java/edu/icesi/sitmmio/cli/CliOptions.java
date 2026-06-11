package edu.icesi.sitmmio.cli;

import java.nio.file.Path;

public final class CliOptions {
    public static final int DEFAULT_MAX_GAP_MINUTES = 10;
    public static final double DEFAULT_MAX_SPEED_KMH = 120.0;
    public static final boolean DEFAULT_DATAGRAMS_HAS_HEADER = true;
    public static final double DEFAULT_COORDINATE_SCALE = 1.0;
    public static final int DEFAULT_THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static final int DEFAULT_WORKER_COUNT = DEFAULT_THREAD_COUNT;
    public static final int DEFAULT_WORKER_RETRIES = 2;

    private final Path linesPath;
    private final Path datagramsPath;
    private final Path outputPath;
    private final String routeColumn;
    private final String busColumn;
    private final String timestampColumn;
    private final String latitudeColumn;
    private final String longitudeColumn;
    private final String activeRouteColumn;
    private final boolean datagramsHasHeader;
    private final Integer routeIndex;
    private final Integer busIndex;
    private final Integer timestampIndex;
    private final Integer latitudeIndex;
    private final Integer longitudeIndex;
    private final double coordinateScale;
    private final int maxGapMinutes;
    private final double maxSpeedKmh;
    private final int threadCount;
    private final ExecutionMode executionMode;
    private final int workerCount;
    private final int partitionCount;
    private final int workerRetryCount;
    private final Path workDirectory;
    private final Path partitionPath;
    private final Path partialResultPath;
    private final Integer partitionId;
    private final Path partialResultsDirectory;
    private final String iceWorkers;
    private final String iceHost;
    private final int icePort;
    private final String iceIdentity;

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        this(
                linesPath,
                datagramsPath,
                outputPath,
                routeColumn,
                busColumn,
                timestampColumn,
                latitudeColumn,
                longitudeColumn,
                activeRouteColumn,
                DEFAULT_DATAGRAMS_HAS_HEADER,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_COORDINATE_SCALE,
                maxGapMinutes,
                maxSpeedKmh,
                DEFAULT_THREAD_COUNT,
                ExecutionMode.THREAD_POOL,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_RETRIES,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                10000,
                "sitm-worker");
    }

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        this(
                linesPath,
                datagramsPath,
                outputPath,
                routeColumn,
                busColumn,
                timestampColumn,
                latitudeColumn,
                longitudeColumn,
                activeRouteColumn,
                datagramsHasHeader,
                routeIndex,
                busIndex,
                timestampIndex,
                latitudeIndex,
                longitudeIndex,
                coordinateScale,
                maxGapMinutes,
                maxSpeedKmh,
                DEFAULT_THREAD_COUNT,
                ExecutionMode.THREAD_POOL,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_RETRIES,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                10000,
                "sitm-worker");
    }

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale,
            int maxGapMinutes,
            double maxSpeedKmh,
            int threadCount
    ) {
        this(
                linesPath,
                datagramsPath,
                outputPath,
                routeColumn,
                busColumn,
                timestampColumn,
                latitudeColumn,
                longitudeColumn,
                activeRouteColumn,
                datagramsHasHeader,
                routeIndex,
                busIndex,
                timestampIndex,
                latitudeIndex,
                longitudeIndex,
                coordinateScale,
                maxGapMinutes,
                maxSpeedKmh,
                threadCount,
                ExecutionMode.THREAD_POOL,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_COUNT,
                DEFAULT_WORKER_RETRIES,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                10000,
                "sitm-worker");
    }

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale,
            int maxGapMinutes,
            double maxSpeedKmh,
            int threadCount,
            ExecutionMode executionMode,
            int workerCount,
            int partitionCount,
            int workerRetryCount,
            Path workDirectory,
            Path partitionPath,
            Path partialResultPath,
            Integer partitionId,
            Path partialResultsDirectory,
            String iceWorkers,
            String iceHost,
            int icePort,
            String iceIdentity
    ) {
        ExecutionMode mode = executionMode == null ? ExecutionMode.THREAD_POOL : executionMode;
        if (mode != ExecutionMode.DISTRIBUTED_WORKER
                && mode != ExecutionMode.ICE_WORKER_SERVER
                && linesPath == null) {
            throw new IllegalArgumentException("Lines path is required.");
        }
        if (mode != ExecutionMode.DISTRIBUTED_WORKER
                && mode != ExecutionMode.DISTRIBUTED_MERGE
                && mode != ExecutionMode.ICE_WORKER_SERVER
                && datagramsPath == null) {
            throw new IllegalArgumentException("Datagrams path is required.");
        }
        if (mode != ExecutionMode.DISTRIBUTED_WORKER
                && mode != ExecutionMode.DISTRIBUTED_SCAN_WORKER
                && mode != ExecutionMode.DISTRIBUTED_PARTITION
                && mode != ExecutionMode.ICE_WORKER_SERVER
                && outputPath == null) {
            throw new IllegalArgumentException("Output path is required.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_PARTITION && workDirectory == null) {
            throw new IllegalArgumentException("Work directory is required for distributed partition mode.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_WORKER && partitionPath == null) {
            throw new IllegalArgumentException("Partition path is required for distributed worker mode.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_WORKER && partialResultPath == null) {
            throw new IllegalArgumentException("Partial result path is required for distributed worker mode.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_SCAN_WORKER && partialResultPath == null) {
            throw new IllegalArgumentException("Partial result path is required for distributed scan worker mode.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_SCAN_WORKER && partitionId == null) {
            throw new IllegalArgumentException("Partition id is required for distributed scan worker mode.");
        }
        if (mode == ExecutionMode.DISTRIBUTED_MERGE && partialResultsDirectory == null) {
            throw new IllegalArgumentException("Partial results directory is required for distributed merge mode.");
        }
        if (mode == ExecutionMode.ICE_MASTER && isBlank(iceWorkers)) {
            throw new IllegalArgumentException("Ice worker endpoints are required for Ice master mode.");
        }
        if (partitionId != null && partitionId < 0) {
            throw new IllegalArgumentException("Partition id must be zero or greater.");
        }
        if (maxGapMinutes <= 0) {
            throw new IllegalArgumentException("Max gap minutes must be greater than zero.");
        }
        if (!Double.isFinite(maxSpeedKmh) || maxSpeedKmh <= 0.0) {
            throw new IllegalArgumentException("Max speed must be greater than zero.");
        }
        if (!Double.isFinite(coordinateScale) || coordinateScale <= 0.0) {
            throw new IllegalArgumentException("Coordinate scale must be greater than zero.");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be greater than zero.");
        }
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero.");
        }
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("Partition count must be greater than zero.");
        }
        if (workerRetryCount < 0) {
            throw new IllegalArgumentException("Worker retry count must be zero or greater.");
        }
        if (icePort <= 0 || icePort > 65535) {
            throw new IllegalArgumentException("Ice port must be between 1 and 65535.");
        }
        if (mode == ExecutionMode.ICE_WORKER_SERVER && isBlank(iceIdentity)) {
            throw new IllegalArgumentException("Ice identity is required for Ice worker server mode.");
        }

        this.linesPath = linesPath;
        this.datagramsPath = datagramsPath;
        this.outputPath = outputPath;
        this.routeColumn = routeColumn;
        this.busColumn = busColumn;
        this.timestampColumn = timestampColumn;
        this.latitudeColumn = latitudeColumn;
        this.longitudeColumn = longitudeColumn;
        this.activeRouteColumn = activeRouteColumn;
        this.datagramsHasHeader = datagramsHasHeader;
        this.routeIndex = routeIndex;
        this.busIndex = busIndex;
        this.timestampIndex = timestampIndex;
        this.latitudeIndex = latitudeIndex;
        this.longitudeIndex = longitudeIndex;
        this.coordinateScale = coordinateScale;
        this.maxGapMinutes = maxGapMinutes;
        this.maxSpeedKmh = maxSpeedKmh;
        this.threadCount = threadCount;
        this.executionMode = mode;
        this.workerCount = workerCount;
        this.partitionCount = partitionCount;
        this.workerRetryCount = workerRetryCount;
        this.workDirectory = workDirectory;
        this.partitionPath = partitionPath;
        this.partialResultPath = partialResultPath;
        this.partitionId = partitionId;
        this.partialResultsDirectory = partialResultsDirectory;
        this.iceWorkers = iceWorkers;
        this.iceHost = isBlank(iceHost) ? "0.0.0.0" : iceHost;
        this.icePort = icePort;
        this.iceIdentity = isBlank(iceIdentity) ? "sitm-worker" : iceIdentity;
    }

    public Path linesPath() {
        return linesPath;
    }

    public Path datagramsPath() {
        return datagramsPath;
    }

    public Path outputPath() {
        return outputPath;
    }

    public String routeColumn() {
        return routeColumn;
    }

    public String busColumn() {
        return busColumn;
    }

    public String timestampColumn() {
        return timestampColumn;
    }

    public String latitudeColumn() {
        return latitudeColumn;
    }

    public String longitudeColumn() {
        return longitudeColumn;
    }

    public String activeRouteColumn() {
        return activeRouteColumn;
    }

    public boolean datagramsHasHeader() {
        return datagramsHasHeader;
    }

    public Integer routeIndex() {
        return routeIndex;
    }

    public Integer busIndex() {
        return busIndex;
    }

    public Integer timestampIndex() {
        return timestampIndex;
    }

    public Integer latitudeIndex() {
        return latitudeIndex;
    }

    public Integer longitudeIndex() {
        return longitudeIndex;
    }

    public double coordinateScale() {
        return coordinateScale;
    }

    public int maxGapMinutes() {
        return maxGapMinutes;
    }

    public double maxSpeedKmh() {
        return maxSpeedKmh;
    }

    public int threadCount() {
        return threadCount;
    }

    public ExecutionMode executionMode() {
        return executionMode;
    }

    public int workerCount() {
        return workerCount;
    }

    public int partitionCount() {
        return partitionCount;
    }

    public int workerRetryCount() {
        return workerRetryCount;
    }

    public Path workDirectory() {
        return workDirectory;
    }

    public Path partitionPath() {
        return partitionPath;
    }

    public Path partialResultPath() {
        return partialResultPath;
    }

    public Integer partitionId() {
        return partitionId;
    }

    public Path partialResultsDirectory() {
        return partialResultsDirectory;
    }

    public String iceWorkers() {
        return iceWorkers;
    }

    public String iceHost() {
        return iceHost;
    }

    public int icePort() {
        return icePort;
    }

    public String iceIdentity() {
        return iceIdentity;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
