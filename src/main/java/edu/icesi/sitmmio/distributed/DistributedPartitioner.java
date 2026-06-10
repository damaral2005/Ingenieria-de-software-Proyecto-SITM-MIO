package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

public final class DistributedPartitioner {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final DatagramPartitioner partitioner;
    private final PartitionManifestCsv manifestCsv;

    public DistributedPartitioner() {
        this(new ActiveRoutesCsvReader(), new DatagramPartitioner(), new PartitionManifestCsv());
    }

    DistributedPartitioner(
            ActiveRoutesCsvReader activeRoutesReader,
            DatagramPartitioner partitioner,
            PartitionManifestCsv manifestCsv
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.partitioner = partitioner;
        this.manifestCsv = manifestCsv;
    }

    public DistributedPartitionSummary partition(CliOptions options) throws IOException {
        long startNanos = System.nanoTime();
        Set<String> activeRoutes = activeRoutesReader.read(options.linesPath(), options.activeRouteColumn());
        Path workDirectory = options.workDirectory();
        Files.createDirectories(workDirectory);

        CsvColumnConfig columnConfig = new CsvColumnConfig(
                options.activeRouteColumn(),
                options.routeColumn(),
                options.busColumn(),
                options.timestampColumn(),
                options.latitudeColumn(),
                options.longitudeColumn(),
                options.datagramsHasHeader(),
                options.routeIndex(),
                options.busIndex(),
                options.timestampIndex(),
                options.latitudeIndex(),
                options.longitudeIndex(),
                options.coordinateScale());

        PartitioningSummary summary = partitioner.partition(
                options.datagramsPath(),
                activeRoutes,
                columnConfig,
                workDirectory,
                options.partitionCount());
        Path manifestPath = workDirectory.resolve("manifest.csv");
        manifestCsv.write(manifestPath, summary.workItems());

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new DistributedPartitionSummary(
                workDirectory,
                manifestPath,
                activeRoutes.size(),
                summary.rawDatagrams(),
                summary.cleanedDatagrams(),
                summary.skippedInvalidDatagrams(),
                options.partitionCount(),
                runtimeMillis);
    }
}
