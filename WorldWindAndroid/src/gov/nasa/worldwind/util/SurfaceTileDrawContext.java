/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import android.graphics.Rect;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Sector;

/**
 * SurfaceTileDrawContext contains the context needed to render to an off-screen surface tile. SurfaceTileDrawContext is
 * defined by a geographic Sector and a corresponding tile viewport. The Sector maps geographic coordinates to pixels in
 * an abstract off-screen tile.
 *
 * @author dcollins
 * @version $Id: SurfaceTileDrawContext.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SurfaceTileDrawContext
{
    protected Sector sector;
    protected Rect viewport;
	protected Matrix projection;
    protected Matrix modelview;

    /**
     * Constructs a SurfaceTileDrawContext with a specified surface Sector and viewport. The Sector defines the
     * context's geographic extent, and the viewport defines the context's corresponding viewport in pixels.
     *
     * @param sector   the context's Sector.
     * @param viewport the context's viewport in pixels.
     *
     * @throws IllegalArgumentException if either the sector or viewport are null, or if the viewport width or height is
     *                                  less than or equal to zero.
     */
    public SurfaceTileDrawContext(Sector sector, Rect viewport, Matrix projectionMatrix)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.ViewportIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (viewport.width() <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (viewport.height() <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.sector = sector;
        this.viewport = viewport;
        this.modelview = Matrix.fromGeographicToViewport(sector, viewport.left, viewport.top,
            viewport.width(), viewport.height());
		this.projection = projectionMatrix;
    }

    /**
     * Constructs a SurfaceTileDrawContext with a specified surface Sector and viewport dimension. The Sector defines
     * the context's geographic extent, and the viewport defines the context's corresponding viewport's dimension in
     * pixels.
     *
     * @param sector         the context's Sector.
     * @param viewportWidth  the context's viewport width in pixels.
     * @param viewportHeight the context's viewport height in pixels.
	 * @param projectionMatrix	the projection matrix
     *
     * @throws IllegalArgumentException if the sector is null, or if the viewport width or height is less than or equal
     *                                  to zero.
     */
    public SurfaceTileDrawContext(Sector sector, int viewportWidth, int viewportHeight, Matrix projectionMatrix)
    {
        this(sector, new Rect(0, 0, viewportWidth, viewportHeight), projectionMatrix);
    }

    /**
     * Returns the context's Sector.
     *
     * @return the context's Sector.
     */
    public Sector getSector()
    {
        return this.sector;
    }

    /**
     * Returns the context's viewport.
     *
     * @return the context's viewport.
     */
    public Rect getViewport()
    {
        return this.viewport;
    }

	/**
	 * Returns a projection Matrix.
	 *
	 * @return projection matrix
	 */
	public Matrix getProjectionMatrix()
	{
		return this.projection;
	}

    /**
     * Returns a Matrix mapping geographic coordinates to pixels in the off-screen tile.
     *
     * @return Matrix mapping geographic coordinates to tile coordinates.
     */
    public Matrix getModelviewMatrix()
    {
        return this.modelview;
    }

    /**
     * Returns a Matrix mapping geographic coordinates to pixels in the off-screen tile. The reference location defines
     * the geographic coordinate origin.
     *
     * @param referenceLocation the geographic coordinate origin.
     *
     * @return Matrix mapping geographic coordinates to tile coordinates.
     *
     * @throws IllegalArgumentException if the reference location is null.
     */
    public Matrix getModelviewMatrix(LatLon referenceLocation)
    {
        if (referenceLocation == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        return this.modelview.multiply(
            Matrix.fromTranslation(referenceLocation.getLongitude().degrees, referenceLocation.getLatitude().degrees,
                0));
    }
}
