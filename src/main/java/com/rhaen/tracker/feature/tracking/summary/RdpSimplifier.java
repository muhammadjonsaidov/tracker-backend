package com.rhaen.tracker.feature.tracking.summary;

import java.util.ArrayList;
import java.util.List;

import static com.rhaen.tracker.feature.tracking.summary.GeoMath.project;

public final class RdpSimplifier {

    private RdpSimplifier() {}

    public static List<PolylineEncoder.LatLon> simplify(List<PolylineEncoder.LatLon> pts, double epsilonMeters) {
        if (pts == null || pts.size() < 3) return pts;

        double lat0 = pts.getFirst().lat(); // reference latitude for projection
        boolean[] keep = new boolean[pts.size()];
        keep[0] = true;
        keep[pts.size() - 1] = true;

        rdp(pts, 0, pts.size() - 1, epsilonMeters, keep, lat0);

        List<PolylineEncoder.LatLon> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            if (keep[i]) out.add(pts.get(i));
        }
        return out;
    }

    private static void rdp(List<PolylineEncoder.LatLon> pts, int start, int end, double eps, boolean[] keep, double lat0) {
        if (end <= start + 1) return;

        double maxDist = -1;
        int index = -1;

        var a = project(pts.get(start).lat(), pts.get(start).lon(), lat0);
        var b = project(pts.get(end).lat(), pts.get(end).lon(), lat0);

        for (int i = start + 1; i < end; i++) {
            var p = project(pts.get(i).lat(), pts.get(i).lon(), lat0);
            double d = perpendicularDistance(p.x(), p.y(), a.x(), a.y(), b.x(), b.y());
            if (d > maxDist) {
                maxDist = d;
                index = i;
            }
        }

        if (maxDist > eps && index != -1) {
            keep[index] = true;
            rdp(pts, start, index, eps, keep, lat0);
            rdp(pts, index, end, eps, keep, lat0);
        }
    }

    private static double perpendicularDistance(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) {
            double ex = px - ax;
            double ey = py - ay;
            return Math.sqrt(ex * ex + ey * ey);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        double ex = px - cx;
        double ey = py - cy;
        return Math.sqrt(ex * ex + ey * ey);
    }
}
