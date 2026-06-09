package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.model.RouteMonthSpeed;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class PartialResultMerger {
    public List<RouteMonthSpeed> merge(
            Set<String> activeRoutes,
            Set<YearMonth> detectedMonths,
            List<PartialRouteMonthAggregate> partialAggregates
    ) {
        Map<RouteMonthKey, PartialRouteMonthAggregate> merged = new HashMap<>();
        for (PartialRouteMonthAggregate partial : partialAggregates) {
            RouteMonthKey key = new RouteMonthKey(partial.routeId(), partial.month());
            merged.merge(key, partial, PartialRouteMonthAggregate::plus);
        }

        TreeSet<String> sortedRoutes = new TreeSet<>(activeRoutes);
        TreeSet<YearMonth> sortedMonths = new TreeSet<>(detectedMonths);
        return sortedRoutes.stream()
                .flatMap(route -> sortedMonths.stream()
                        .map(month -> merged.getOrDefault(
                                        new RouteMonthKey(route, month),
                                        emptyPartial(route, month))
                                .toRouteMonthSpeed()))
                .collect(Collectors.toList());
    }

    private static PartialRouteMonthAggregate emptyPartial(String routeId, YearMonth month) {
        return new PartialRouteMonthAggregate(routeId, month, 0.0, 0.0, 0.0, 0, 0);
    }

    private static final class RouteMonthKey {
        private final String routeId;
        private final YearMonth month;

        private RouteMonthKey(String routeId, YearMonth month) {
            this.routeId = routeId;
            this.month = month;
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
}
