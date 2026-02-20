package com.rhaen.tracker.feature.tracking.summary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RdpSimplifierTest {

    @Test
    void simplify_returnsSame_forNullOrSmallInputs() {
        assertThat(RdpSimplifier.simplify(null, 5.0)).isNull();

        List<PolylineEncoder.LatLon> small = List.of(
                new PolylineEncoder.LatLon(41.0, 69.0),
                new PolylineEncoder.LatLon(41.1, 69.1)
        );
        assertThat(RdpSimplifier.simplify(small, 5.0)).isEqualTo(small);
    }

    @Test
    void simplify_line_keepsEndpoints() {
        List<PolylineEncoder.LatLon> points = List.of(
                new PolylineEncoder.LatLon(41.0000, 69.0000),
                new PolylineEncoder.LatLon(41.0002, 69.0000),
                new PolylineEncoder.LatLon(41.0004, 69.0000),
                new PolylineEncoder.LatLon(41.0006, 69.0000)
        );

        List<PolylineEncoder.LatLon> simplified = RdpSimplifier.simplify(points, 20.0);

        assertThat(simplified).hasSize(2);
        assertThat(simplified.getFirst()).isEqualTo(points.getFirst());
        assertThat(simplified.getLast()).isEqualTo(points.getLast());
    }

    @Test
    void simplify_zigzag_withSmallEpsilon_keepsMorePoints() {
        List<PolylineEncoder.LatLon> points = List.of(
                new PolylineEncoder.LatLon(41.0000, 69.0000),
                new PolylineEncoder.LatLon(41.0003, 69.0003),
                new PolylineEncoder.LatLon(41.0006, 69.0000),
                new PolylineEncoder.LatLon(41.0009, 69.0003),
                new PolylineEncoder.LatLon(41.0012, 69.0000)
        );

        List<PolylineEncoder.LatLon> simplified = RdpSimplifier.simplify(points, 1.0);

        assertThat(simplified.size()).isGreaterThan(2);
    }
}
