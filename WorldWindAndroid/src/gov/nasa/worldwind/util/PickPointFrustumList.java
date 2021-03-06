/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import android.graphics.Point;
import android.graphics.Rect;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.PickPointFrustum;
import gov.nasa.worldwind.geom.Vec4;

import java.util.ArrayList;

/**
 * @author Jeff Addison
 * @version $Id$
 */
public class PickPointFrustumList extends ArrayList<PickPointFrustum>
{
    public PickPointFrustumList()
    {
    }

    public PickPointFrustumList(PickPointFrustumList list)
    {
        super(list);
    }

    /**
     * Returns true if the specified point is inside the space enclosed by ALL of the frustums
     *
     * @param point the point to test.
     *
     * @return true if the specified point is inside the space enclosed by ALL the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public final boolean containsInAll(Vec4 point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (!frustum.contains(point))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the specified point is inside the space enclosed by ANY of the frustums
     *
     * @param point the point to test.
     *
     * @return true if the specified point is inside the space enclosed by ANY the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public final boolean containsInAny(Vec4 point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (frustum.contains(point))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the specified 2D screen point is inside the 2D screen rectangle enclosed by ALL of the frustums
     *
     * @param point the point to test.
     *
     * @return true if the specified point is inside the space enclosed by ALL the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public final boolean containsInAll(Point point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (!frustum.contains(point.x, point.y))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the specified 2D point is inside the 2D screen rectangle enclosed by ANY of the frustums
     *
     * @param x the x coordinate to test.
     * @param y the y coordinate to test.
     *
     * @return true if the specified point is inside the space enclosed by ANY the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public final boolean containsInAny(double x, double y)
    {
        for (PickPointFrustum frustum : this)
        {
            if (frustum.contains(x, y))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the specified 2D point is inside the 2D screen rectangle enclosed by ANY of the frustums
     *
     * @param point the point to test.
     *
     * @return true if the specified point is inside the space enclosed by ANY the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the point is null.
     */
    public final boolean containsInAny(Point point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (frustum.contains(point.x, point.y))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the specified {@link gov.nasa.worldwind.geom.Extent} intersects the space enclosed by ALL the Frustums. NOTE: Cannot be
     * true if all frustums do not intersect.
     *
     * @param extent the Extent to test.
     *
     * @return true if the specified Extent intersects the space enclosed by ALL Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the extent is null.
     */
    public final boolean intersectsAll(Extent extent)
    {
        if (extent == null)
        {
            String msg = Logging.getMessage("nullValue.ExtentIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (!frustum.intersects(extent))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the specified {@link gov.nasa.worldwind.geom.Extent} intersects the space enclosed by ANY of the Frustums.
     *
     * @param extent the Extent to test.
     *
     * @return true if the specified Extent intersects the space enclosed by ANY of the Frustums, and false otherwise.
     *
     * @throws IllegalArgumentException if the extent is null.
     */
    public final boolean intersectsAny(Extent extent)
    {
        if (extent == null)
        {
            String msg = Logging.getMessage("nullValue.ExtentIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (frustum.intersects(extent))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if a specified line segment intersects the space enclosed by ANY of the Frustums.
     *
     * @param pa one end of the segment.
     * @param pb the other end of the segment.
     *
     * @return true if the specified segment intersects the space enclosed by ANY of the Frustums, otherwise false.
     *
     * @throws IllegalArgumentException if either point is null.
     */
    public final boolean intersectsAny(Vec4 pa, Vec4 pb)
    {
        for (PickPointFrustum frustum : this)
        {
            if (frustum.intersectsSegment(pa, pb))
                return true;
        }

        return false;
    }

    /**
     * Returns true if the specified {@link android.graphics,Rect} intersects the 2D screen space enclosed by ALL the
     * Frustums. NOTE: Cannot be true if all frustums do not intersect.
     *
     * @param rect the Rectangle to test.
     *
     * @return true if the specified Rectangle intersects the 2D screen space enclosed by ALL Frustums, and false
     *         otherwise.
     *
     * @throws IllegalArgumentException if the extent is null.
     */
    public final boolean intersectsAll(Rect rect)
    {
        if (rect == null)
        {
            String msg = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (!frustum.intersects(rect))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the specified {@link android.graphics.Rect} intersects the 2D screen space enclosed by ANY of the
     * Frustums.
     *
     * @param rect the Rectangle to test.
     *
     * @return true if the specified Rectangle intersects the 2D screen space enclosed by ANY of the Frustums, and false
     *         otherwise.
     *
     * @throws IllegalArgumentException if the extent is null.
     */
    public final boolean intersectsAny(Rect rect)
    {
        if (rect == null)
        {
            String msg = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.verbose(msg);
            throw new IllegalArgumentException(msg);
        }

        for (PickPointFrustum frustum : this)
        {
            if (frustum.intersects(rect))
            {
                return true;
            }
        }

        return false;
    }
}