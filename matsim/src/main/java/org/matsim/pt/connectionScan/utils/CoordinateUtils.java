package org.matsim.pt.connectionScan.utils;

import org.matsim.api.core.v01.Coord;

import java.awt.geom.Point2D;

public class CoordinateUtils {

    public static Coord convert2Coord(Point2D point) {
        return new Coord(point.getX(), point.getY());
    }

    /**
     * Converts a matsim/Coord into a Point2D.
     *
     * @param matsimCoord the matsim/TransitStopFacility that contains the required matsim/Coord
     * @return the converted Point2D
     */
    public static Point2D convert2Point2D(Coord matsimCoord) {
        return new Point2D.Double(matsimCoord.getX(), matsimCoord.getY());
    }
}
