package com.rhaen.tracker.common.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeoUtils {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoUtils() {}

    /** Creates a WGS84 point (SRID=4326) from lon/lat */
    public static Point point(double lon, double lat) {
        Point p = GF.createPoint(new Coordinate(lon, lat));
        p.setSRID(4326);
        return p;
    }
}
