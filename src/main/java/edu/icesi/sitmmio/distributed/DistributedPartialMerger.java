package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class DistributedPartialMerger {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final PartialResultCsv partialResultCsv;
    private final PartialResultMerger partialResultMerger;
    private final ResultCsvWriter resultWriter;

    public DistributedPartialMerger() {
        this(new ActiveRoutesCsvReader(), new PartialResultCsv(), new PartialResultMerger(), new ResultCsvWriter());
    }

    DistributedPartialMerger(
            ActiveRoutesCsvReader activeRoutesReader,
            PartialResultCsv partialResultCsv,
            PartialResultMerger partialResultMerger,
            ResultCsvWriter resultWriter
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.partialResultCsv = partialResultCsv;
        this.partialResultMerger = partialResultMerger;
        this.resultWriter = resultWriter;
    }

    public DistributedMergeSummary merge(
            Path linesPath,
            String activeRouteColumn,
            Path partialResultsDirectory,
            Path outputPath
    ) throws IOException {
        long startNanos = System.nanoTime();
        Set<String> activeRoutes = activeRoutesReader.read(linesPath, activeRouteColumn);
        List<Path> partialPaths = listPartialPaths(partialResultsDirectory);
        List<PartialRouteMonthAggregate> partials = new ArrayList<>();
        Set<YearMonth> months = new TreeSet<>();
        for (Path partialPath : partialPaths) {
            List<PartialRouteMonthAggregate> readPartials = partialResultCsv.read(partialPath);
            partials.addAll(readPartials);
            for (PartialRouteMonthAggregate partial : readPartials) {
                months.add(partial.month());
            }
        }

        List<RouteMonthSpeed> results = partialResultMerger.merge(activeRoutes, months, partials);
        resultWriter.write(outputPath, results);
        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        long validSegments = partials.stream()
                .mapToLong(PartialRouteMonthAggregate::validSegments)
                .sum();
        return new DistributedMergeSummary(
                outputPath,
                partialResultsDirectory,
                partialPaths.size(),
                partials.size(),
                validSegments,
                results.size(),
                runtimeMillis);
    }

    private static List<Path> listPartialPaths(Path partialResultsDirectory) throws IOException {
        try (var paths = Files.list(partialResultsDirectory)) {
            List<Path> partialPaths = paths
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
            if (partialPaths.isEmpty()) {
                throw new IOException("No partial CSV files found in " + partialResultsDirectory);
            }
            return partialPaths;
        }
    }
}
