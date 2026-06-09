package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.SpeedSegment;
import edu.icesi.sitmmio.service.SpeedSegmentCalculator;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class WorkerProcessor {
    private final HaversineDistanceCalculator distanceCalculator;
    private final PartialRouteMonthAggregator aggregator;
    private final PartialResultCsv partialResultCsv;

    public WorkerProcessor() {
        this(new HaversineDistanceCalculator(), new PartialRouteMonthAggregator(), new PartialResultCsv());
    }

    WorkerProcessor(
            HaversineDistanceCalculator distanceCalculator,
            PartialRouteMonthAggregator aggregator,
            PartialResultCsv partialResultCsv
    ) {
        this.distanceCalculator = distanceCalculator;
        this.aggregator = aggregator;
        this.partialResultCsv = partialResultCsv;
    }

    public WorkerRunSummary process(PartitionWorkItem workItem, Duration maxGap, double maxSpeedKmh)
            throws IOException {
        long startNanos = System.nanoTime();
        List<GpsPoint> sortedPoints = CleanedPartitionCsv.read(workItem.partitionPath()).stream()
                .sorted(Comparator.comparing(GpsPoint::routeId)
                        .thenComparing(GpsPoint::busId)
                        .thenComparing(GpsPoint::timestamp))
                .collect(Collectors.toList());

        SpeedSegmentCalculator segmentCalculator = new SpeedSegmentCalculator(
                distanceCalculator,
                maxGap,
                maxSpeedKmh);
        List<SpeedSegment> segments = segmentCalculator.calculateSegments(sortedPoints);
        List<PartialRouteMonthAggregate> partials = aggregator.aggregate(segments);
        partialResultCsv.write(workItem.partialResultPath(), partials);

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new WorkerRunSummary(
                workItem.partitionPath(),
                workItem.partialResultPath(),
                sortedPoints.size(),
                segments.size(),
                partials.size(),
                runtimeMillis);
    }
}
