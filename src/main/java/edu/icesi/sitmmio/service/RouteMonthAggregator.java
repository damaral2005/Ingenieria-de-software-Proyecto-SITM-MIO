package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RouteMonthAggregator {
    public List<RouteMonthSpeed> aggregate(List<SpeedSegment> segments) {
        Map<RouteMonthKey, Accumulator> groups = new HashMap<>();

        for (SpeedSegment segment : segments) {
            RouteMonthKey key = new RouteMonthKey(segment.routeId(), segment.month());
            groups.computeIfAbsent(key, ignored -> new Accumulator()).add(segment);
        }

        return groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().toResult(entry.getKey()))
                .collect(Collectors.toList());
    }

    private static final class RouteMonthKey implements Comparable<RouteMonthKey> {
        private final String routeId;
        private final YearMonth month;

        private RouteMonthKey(String routeId, YearMonth month) {
            this.routeId = routeId;
            this.month = month;
        }

        @Override
        public int compareTo(RouteMonthKey other) {
            int routeComparison = routeId.compareTo(other.routeId);
            if (routeComparison != 0) {
                return routeComparison;
            }
            return month.compareTo(other.month);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RouteMonthKey)) {
                return false;
            }
            RouteMonthKey that = (RouteMonthKey) other;
            return routeId.equals(that.routeId) && month.equals(that.month);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeId, month);
        }
    }

    private static final class Accumulator {
        private double totalDistanceKm;
        private double totalTimeHours;
        private double totalSegmentSpeedKmh;
        private long validSegments;
        private final Set<String> observedBusIds = new HashSet<>();

        private void add(SpeedSegment segment) {
            totalDistanceKm += segment.distanceKm();
            totalTimeHours += segment.durationHours();
            totalSegmentSpeedKmh += segment.speedKmh();
            validSegments++;
            observedBusIds.add(segment.busId());
        }

        private RouteMonthSpeed toResult(RouteMonthKey key) {
            double averageSpeedKmh = totalDistanceKm / totalTimeHours;
            double averageSegmentSpeedKmh = totalSegmentSpeedKmh / validSegments;
            return new RouteMonthSpeed(
                    key.routeId,
                    key.month,
                    totalDistanceKm,
                    totalTimeHours,
                    averageSpeedKmh,
                    averageSegmentSpeedKmh,
                    validSegments,
                    observedBusIds.size());
        }
    }
}
