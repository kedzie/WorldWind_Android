/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import android.graphics.Point;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author dcollins
 * @version $Id: SectorGeometry.java 802 2012-09-26 01:37:27Z dcollins $
 */
public interface SectorGeometry
{
    Sector getSector();

    Extent getExtent();

    /**
     * Computes the point in model coordinates on the geometry's surface at the specified location.
     *
     * @param latitude  the latitude of the point to compute.
     * @param longitude the longitude of the point to compute.
     * @param result    contains the model coordinate point in meters, relative to an origin of (0, 0, 0) after this
     *                  method exits. The result parameter is left unchanged if this method returns <code>false</code>.
     *
     * @return <code>true</code> if the specified location is within this geometry's sector and its internal geometry
     *         exists, otherwise <code>false</code>.
     *
     * @throws IllegalArgumentException if any of the latitude, longitude or result are <code>null</code>.
     */
    boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result);

    void render(DrawContext dc);

    void renderWireframe(DrawContext dc);

    void renderOutline(DrawContext dc);

	/**
	 * Displays the geometry's bounding volume.
	 *
	 * @param dc the current draw context.
	 *
	 * @throws IllegalArgumentException if the draw context is null.
	 */
	void renderBoundingVolume(DrawContext dc);

	/**
	 * Displays on the geometry's surface the tessellator level and the minimum and maximum elevations of the sector.
	 *
	 * @param dc the current draw context.
	 *
	 * @throws IllegalArgumentException if the draw context is null.
	 */
	void renderTileID(DrawContext dc);

    void beginRendering(DrawContext dc);

    void endRendering(DrawContext dc);

    void pick(DrawContext dc, Point pickPoint);

	/**
	 * Computes the Cartesian coordinates of a line's intersections with the geometry.
	 *
	 * @param line the line to intersect.
	 *
	 * @return the Cartesian coordinates of each intersection, or null if there is no intersection or no internal
	 *         geometry has been computed.
	 *
	 * @throws IllegalArgumentException if the line is null.
	 */
	Intersection[] intersect(Line line);

	/**
	 * Computes the geometry's intersections with a globe at a specified elevation.
	 *
	 * @param elevation the elevation for which intersection points are to be found.
	 *
	 * @return an array of intersection pairs, or null if no intersections were found. The returned array of
	 *         intersections describes a list of individual segments - two <code>Intersection</code> elements for each,
	 *         corresponding to each geometry triangle that intersects the given elevation.
	 */
	Intersection[] intersect(double elevation);

}
