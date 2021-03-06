/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.geom.Sector;

import java.util.List;

/**
 * Common interface for renderables that are drawn on the Globe's surface terrain, such as {@link
 * SurfaceShape}. SurfaceObject implements the {@link Renderable}
 * and {@link gov.nasa.worldwind.render.PreRenderable} interfaces, so a surface object may be aggregated within any
 * layer or within some arbitrary rendering code.
 * <p/>
 * SurfaceObjects automatically aggregate themselves in the DrawContext's ordered surface renderable queue by calling
 * {@link DrawContext#addOrderedSurfaceRenderable(gov.nasa.worldwind.render.OrderedRenderable)} during the preRender,
 * pick, and render stages. This enables SurfaceObjects to be processed in bulk, and reduces texture memory consumption
 * by sharing rendering resources amongst multiple SurfaceObjects.
 * <p/>
 * Implementations of SurfaceObject require that {@link #preRender(gov.nasa.worldwind.render.DrawContext)} is called before {@link
 * #render(gov.nasa.worldwind.render.DrawContext)} and {@link #pick(gov.nasa.worldwind.render.DrawContext, android.graphics.Point)}, and that preRender is called at the appropriate
 * stage in the current rendering cycle. Calling preRender locks in the SurfaceObject's visual appearance for any
 * subsequent calls to pick or render until the next call preRender.
 *
 * @author dcollins
 * @version $Id: SurfaceObject.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface SurfaceObject extends OrderedRenderable, PreRenderable, ExtentHolder,  AVList
{
    /**
     * Indicates whether the surface object should be drawn during rendering.
     *
     * @return true if the object is to be drawn, otherwise false.
     */
    boolean isVisible();

    /**
     * Specifies whether the surface object should be drawn during rendering.
     *
     * @param visible true if the object is to be drawn, otherwise false.
     */
    void setVisible(boolean visible);

    /**
     * Returns an object that uniquely identifies the surface object's state on the specified draw context. This object
     * is guaranteed to be globally unique; an equality test with a state key from another always returns false.
     *
     * @param dc the draw context the state key relates to.
     *
     * @return an object representing surface object's current state.
     */
    Object getStateKey(DrawContext dc);

    /**
     * Returns zero to indicate that the surface object's distance from the eye is unknown. SurfaceObjects are processed
     * on the DrawContext's ordered surface renderable queue. Ordered surface renderables do not utilize the
     * renderable's distance from the eye to determine draw order.
     *
     * @return zero, to indicate that the object's distance from the eye is unknown.
     */
    double getDistanceFromEye();

    /**
     * Returns the delegate owner of the surface object. If non-null, the returned object replaces the surface object as
     * the pickable object returned during picking. If null, the surface object itself is the pickable object returned
     * during picking.
     *
     * @return the object used as the pickable object returned during picking, or null to indicate the the surface
     *         object is returned during picking.
     */
    Object getDelegateOwner();

    /**
     * Specifies the delegate owner of the surface object. If non-null, the delegate owner replaces the surface object
     * as the pickable object returned during picking. If null, the surface object itself is the pickable object
     * returned during picking.
     *
     * @param owner the object to use as the pickable object returned during picking, or null to return the surface
     *              object.
     */
    void setDelegateOwner(Object owner);

    /**
     * Returns a {@link java.util.List} of {@link gov.nasa.worldwind.geom.Sector} instances that bound the surface
     * object on the specified DrawContext.
     *
     * @param dc the DrawContext the surface object is related to.
     *
     * @return the surface object's bounding Sectors.
     */
    List<Sector> getSectors(DrawContext dc);
}
