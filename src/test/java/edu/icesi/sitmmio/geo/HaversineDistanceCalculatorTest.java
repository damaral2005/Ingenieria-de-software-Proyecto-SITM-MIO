package edu.icesi.sitmmio.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HaversineDistanceCalculatorTest {
    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void returnsZeroForSamePoint() {
        assertEquals(0.0, calculator.calculateKm(3.4516, -76.5320, 3.4516, -76.5320));
    }

    @Test
    void calculatesApproximateDistanceBetweenCaliAndPalmira() {
        double distance = calculator.calculateKm(3.4516, -76.5320, 3.5394, -76.3036);

        assertEquals(27.2, distance, 0.5);
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateKm(91.0, -76.5320, 3.4516, -76.5320));
    }
}
