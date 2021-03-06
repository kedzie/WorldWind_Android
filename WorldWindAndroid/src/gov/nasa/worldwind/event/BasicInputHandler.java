/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.event;

import android.graphics.Point;
import android.os.*;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.Logging;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ccrick
 * @version $Id: BasicInputHandler.java 852 2012-10-12 19:35:43Z dcollins $
 */
public class BasicInputHandler extends WWObjectImpl implements InputHandler, ScaleGestureDetector.OnScaleGestureListener
{
    protected static final int JUMP_THRESHOLD = 100;
    protected static final double PINCH_WIDTH_DELTA_THRESHOLD = 5;
    protected static final double PINCH_ROTATE_DELTA_THRESHOLD = 1;
	protected static final double DEFAULT_DRAG_SLOPE_FACTOR = 0.002;

    protected WorldWindow eventSource;
	protected List<SelectListener> selectListeners = new ArrayList<SelectListener>();

    protected float mPreviousX = -1;
    protected float mPreviousY = -1;
    protected int mPrevPointerCount = 0;

    protected float mPreviousX2 = -1;
    protected float mPreviousY2 = -1;
	protected float mInitialX=-1;
	protected float mInitialY=-1;
	protected double dragSlopeFactor = DEFAULT_DRAG_SLOPE_FACTOR;

    // Temporary properties used to avoid constant allocation when responding to input events.
    protected Point touchPoint = new Point();
	protected Point previousTouchPoint = new Point();
    protected Position position = new Position();
    protected Vec4 point1 = new Vec4();
    protected Vec4 point2 = new Vec4();

	protected ScaleGestureDetector scaleGestureDetector;
	private GestureDetector gestureDetector;
	private Position selectedPosition;

	private static final int WHAT_STOP_ANIMATIONS = 0;

	private Handler animationHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case WHAT_STOP_ANIMATIONS:
					((BasicView) eventSource.getView()).stopAnimations();
					break;
			}
		}
	};

	public BasicInputHandler()
    {
    }

	public void addSelectListener(SelectListener listener)
	{
		this.selectListeners.add(listener);
	}

	public void removeSelectListener(SelectListener listener)
	{
		this.selectListeners.remove(listener);
	}

	protected void callSelectListeners(SelectEvent event)
	{
		for (SelectListener listener : this.selectListeners)
		{
			listener.selected(event);
		}
	}

	/**
	 * Returns the <code>factor</code> that dampens view movement when the user pans drags the cursor in a way that could
	 * cause an abrupt transition.
	 *
	 * @return factor dampening view movement when a mouse drag event would cause an abrupt transition.
	 * @see #setDragSlopeFactor
	 */
	public double getDragSlopeFactor()
	{
		return this.dragSlopeFactor;
	}

	/**
	 * Sets the <code>factor</code> that dampens view movement when a mouse drag event would cause an abrupt
	 * transition. The drag slope is the ratio of screen pixels to Cartesian distance moved, measured by the previous
	 * and current mouse points. As drag slope gets larger, it becomes more difficult to operate the view. This
	 * typically happens while dragging over and around the horizon, where movement of a few pixels can cause the view
	 * to move many kilometers. This <code>factor</code> is the amount of damping applied to the view movement in such
	 * cases. Setting <code>factor</code> to zero will disable this behavior, while setting <code>factor</code> to a
	 * positive value may dampen the effects of mouse dragging.
	 *
	 * @param factor dampening view movement when a mouse drag event would cause an abrupt transition. Must be greater
	 * than or equal to zero.
	 *
	 * @throws IllegalArgumentException if <code>factor</code> is less than zero.
	 */
	public void setDragSlopeFactor(double factor)
	{
		if (factor < 0)
		{
			String message = Logging.getMessage("generic.ArgumentOutOfRange", "factor < 0");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.dragSlopeFactor = factor;
	}

    public WorldWindow getEventSource()
    {
        return this.eventSource;
    }

    public void setEventSource(WorldWindow eventSource)
    {
		this.eventSource = eventSource;
		scaleGestureDetector = new ScaleGestureDetector(eventSource.getContext(), this);
		gestureDetector = new GestureDetector(eventSource.getContext(), new GestureDetector.SimpleOnGestureListener() {

			@Override
			public void onLongPress(MotionEvent e) {

			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}
		});
		gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
			@Override
			public boolean onSingleTapConfirmed(final MotionEvent e) {
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {

				return false;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}
		});
    }

    public boolean onTouch(final View view, MotionEvent motionEvent)
    {
		scaleGestureDetector.onTouchEvent(motionEvent);
		gestureDetector.onTouchEvent(motionEvent);

        final int pointerCount = motionEvent.getPointerCount();

        final float x = motionEvent.getX(0);
        final float y = motionEvent.getY(0);
		updateTouchPoints(x, y);

        switch (motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
//				eventSource.getSceneController().setPickPoint(touchPoint);
				mInitialX = x;
				mInitialY = y;
				this.setSelectedPosition(this.computeSelectedPosition());
                break;
            }

            // all fingers have left the tablet screen
            case MotionEvent.ACTION_UP:
            {
//				eventSource.getSceneController().setPickPoint(null);
				this.setSelectedPosition(null);
                // reset previous variables
                mPreviousX = -1;
                mPreviousY = -1;
                mPreviousX2 = -1;
                mPreviousY2 = -1;
                mPrevPointerCount = 0;

                break;
            }

            case MotionEvent.ACTION_MOVE:
                float dx = 0;
                float dy = 0;
                if (mPreviousX > -1 && mPreviousY > -1)
                {
                    dx = x - mPreviousX;
                    dy = y - mPreviousY;
                }
                // return if detect a new gesture, as indicated by a large jump
                if (Math.abs(dx) > JUMP_THRESHOLD || Math.abs(dy) > JUMP_THRESHOLD)
                    return true;

                float width = view.getWidth();
                float height = view.getHeight();
                // normalize dx, dy with screen width and height, so they are in [0, 1]
                final double xVelocity = dx / width;
                final double yVelocity = dy / height;

                if (pointerCount != 2 || mPrevPointerCount != 2)
                {
                    // reset pinch variables
                    mPreviousX2 = -1;
                    mPreviousY2 = -1;
                }

                // interpret the motionEvent
                if (pointerCount == 1)
                {
//					eventSource.getSceneController().setPickPoint(touchPoint);
					final float forwardInput = y-mPreviousY;
					final float sideInput = -(x-mPreviousX);
					final float totalForwardInput = y-mInitialY;
					final float totalSideInput = -(x-mInitialX);
                    eventSource.invokeInRenderingThread(new Runnable()
                    {
                        public void run()
                        {
//                            onHorizontalTranslateRel(forwardInput, sideInput, totalForwardInput, totalSideInput);
                            handlePan(xVelocity, yVelocity);
                        }
                    });
                }
                // handle zoom, rotate/revolve and tilt
                else if (pointerCount > 1)
                {
                    boolean upMove = dy > 0;
                    boolean downMove = dy < 0;

                    float slope = 2;    // arbitrary value indicating a vertical slope
                    if (dx != 0)
                        slope = dy / dx;

                    // separate gestures by number of fingers
                    if (pointerCount == 2)
                    {
                        float x2 = motionEvent.getX(1);
                        float y2 = motionEvent.getY(1);

                        float dy2 = 0;
                        if (mPreviousX > -1 && mPreviousY > -1)
                        {   // delta is only relevant if a previous location exists
                            dy2 = y2 - mPreviousY2;
                        }

                        final double yVelocity2 = dy2 / height;

                        // compute angle traversed
                        final double deltaPinchAngle = computeRotationAngle(x, y, x2, y2,
                            mPreviousX, mPreviousY, mPreviousX2, mPreviousY2);

                        // TODO: prevent this from confusion with pinch-rotate
                        if ((upMove || downMove) && Math.abs(slope) > 1
                            && (yVelocity > 0 && yVelocity2 > 0) || (yVelocity < 0 && yVelocity2 < 0))
                        {
                            eventSource.invokeInRenderingThread(new Runnable()
                            {
                                public void run()
                                {
                                    handleLookAtTilt(yVelocity);
                                }
                            });
                        }
                        else if (deltaPinchAngle != 0 && deltaPinchAngle > PINCH_ROTATE_DELTA_THRESHOLD)
                        {
                            eventSource.invokeInRenderingThread(new Runnable()
                            {
                                public void run()
                                {
                                    handlePinchRotate(deltaPinchAngle);
                                }
                            });
                        }

                        mPreviousX2 = x2;
                        mPreviousY2 = y2;
                    }
                    else if (pointerCount >= 3)
                    {
                        eventSource.invokeInRenderingThread(new Runnable()
                        {
                            public void run()
                            {
                                handleRestoreNorth(xVelocity, yVelocity);
                            }
                        });
                    }
                }

                eventSource.redraw();

                mPreviousX = x;
                mPreviousY = y;
                mPrevPointerCount = pointerCount;
                break;
        }

        return true;
    }

	protected void updateTouchPoints(float x, float y) {
		previousTouchPoint.set(touchPoint.x, touchPoint.y);
		touchPoint.set((int)x, (int)y);
	}

	// given the current and previous locations of two points, compute the angle of the
    // rotation they trace out
    protected double computeRotationAngle(float x, float y, float x2, float y2,
        float xPrev, float yPrev, float xPrev2, float yPrev2)
    {
        // can't compute if no previous points
        if (xPrev < 0 || yPrev < 0 || xPrev2 < 0 || yPrev2 < 0)
            return 0;

        if ((x - x2) == 0 || (xPrev - xPrev2) == 0)
            return 0;

        // 1. compute lines connecting pt1 to pt2, and pt1' to pt2'
        float slope = (y - y2) / (x - x2);
        float slopePrev = (yPrev - yPrev2) / (xPrev - xPrev2);

        // b = y - mx
        float b = y - slope * x;
        float bPrev = yPrev - slopePrev * xPrev;

        // 2. use Cramer's Rule to find the intersection of the two lines
        float det1 = -slope * 1 + slopePrev * 1;
        float det2 = b * 1 - bPrev * 1;
        float det3 = (-slope * bPrev) - (-slopePrev * b);

        // check for case where lines are parallel
        if (det1 == 0)
            return 0;

        // compute the intersection point
        float isectX = det2 / det1;
        float isectY = det3 / det1;

        // 3. use the law of Cosines to determine the angle covered

        // compute lengths of sides of triangle created by pt1, pt1Prev and the intersection pt
        double BC = Math.sqrt(Math.pow(x - isectX, 2) + Math.pow(y - isectY, 2));
        double AC = Math.sqrt(Math.pow(xPrev - isectX, 2) + Math.pow(yPrev - isectY, 2));
        double AB = Math.sqrt(Math.pow(x - xPrev, 2) + Math.pow(y - yPrev, 2));

        this.point1.set(xPrev - isectX, yPrev - isectY, 0);
        this.point2.set(x - isectX, y - isectY, 0);

        // if one finger stayed fixed, may have degenerate triangle, so use other triangle instead
        if (BC == 0 || AC == 0 || AB == 0)
        {
            BC = Math.sqrt(Math.pow(x2 - isectX, 2) + Math.pow(y2 - isectY, 2));
            AC = Math.sqrt(Math.pow(xPrev2 - isectX, 2) + Math.pow(yPrev2 - isectY, 2));
            AB = Math.sqrt(Math.pow(x2 - xPrev2, 2) + Math.pow(y2 - yPrev2, 2));

            this.point1.set(xPrev2 - isectX, yPrev2 - isectY, 0);
            this.point2.set(x2 - isectX, y2 - isectY, 0);

            if (BC == 0 || AC == 0 || AB == 0)
                return 0;
        }

        // Law of Cosines
        double num = (Math.pow(BC, 2) + Math.pow(AC, 2) - Math.pow(AB, 2));
        double denom = (2 * BC * AC);
        double BCA = Math.acos(num / denom);

        // use cross product to determine if rotation is positive or negative
        if (this.point1.cross3(this.point2).z < 0)
            BCA = 2 * Math.PI - BCA;

        return Math.toDegrees(BCA);
    }

    // computes pan using velocity of swipe motion
    protected void handlePan(double xVelocity, double yVelocity)
    {
		stopAnimations();
		BasicView view = (BasicView) eventSource.getView();

		Position pos = view.getLookAtPosition();
        Angle heading = view.getHeading();
        double range = view.getRange();

        double panScalingFactor = 0.00001f;
        double sin = heading.sin();
        double cos = heading.cos();

        double newLat = Angle.normalizedDegreesLatitude(pos.latitude.degrees
            + (cos * yVelocity + sin * xVelocity) * panScalingFactor * range);
        double newLon = Angle.normalizedDegreesLongitude(pos.longitude.degrees
            - (cos * xVelocity - sin * yVelocity) * panScalingFactor * range);

        pos.setDegrees(newLat, newLon);
    }

	private void stopAnimations() {
		animationHandler.sendEmptyMessage(WHAT_STOP_ANIMATIONS);
//		((View)eventSource).post(new Runnable() {
//			@Override
//			public void run() {
//				((BasicView) eventSource.getView()).stopAnimations();
//			}
//		});
	}

	protected Position getSelectedPosition()
	{
		return this.selectedPosition;
	}

	protected void setSelectedPosition(Position position)
	{
		this.selectedPosition = position;
	}

	protected void handlePinchRotate(double rotAngleDegrees)
    {
		stopAnimations();

		BasicView view = (BasicView) this.eventSource.getView();
        Angle angle = view.getHeading();
        double newAngle = (angle.degrees - rotAngleDegrees) % 360;

        if (newAngle < -180)
            newAngle = 360 + newAngle;
        else if (newAngle > 180)
            newAngle = newAngle - 360;

        angle.setDegrees(newAngle);
    }

    protected void handleLookAtTilt(double yVelocity)
    {
		stopAnimations();

		BasicView view = (BasicView) this.eventSource.getView();
        Angle angle = view.getTilt();
        double scalingFactor = 100;
        double newAngle = (angle.degrees + yVelocity * scalingFactor) % 360;

        if (newAngle < 0)
            newAngle = 0;
        else if (newAngle > 90)
            newAngle = 90;

        angle.setDegrees(newAngle);
    }

    protected void handleRestoreNorth(double xVelocity, double yVelocity)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Angle heading = view.getHeading();
        Angle tilt = view.getTilt();

        // interpolate to zero heading and tilt
        double headingScalingFactor = 5;
        double tiltScalingFactor = 3;
        double delta = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        double newHeading = heading.degrees + -heading.degrees * delta * headingScalingFactor;
        double newTilt = tilt.degrees + -tilt.degrees * delta * tiltScalingFactor;

        heading.setDegrees(newHeading);
        tilt.setDegrees(newTilt);
    }

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		BasicView view = (BasicView) this.eventSource.getView();
		double range = view.getRange();
		double newRange = range/detector.getScaleFactor();
		view.setRange(newRange);
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		stopAnimations();
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	protected Position computeSelectedPosition() {
		BasicView view = (BasicView) this.eventSource.getView();
		if (view == null) // include this test to ensure any derived implementation performs it
		{
			return null;
		}
		selectedPosition = new Position();
		view.computePositionFromScreenPoint(eventSource.getModel().getGlobe(), touchPoint, selectedPosition);
		return selectedPosition;
	}

/*
	protected Position computeSelectedPosition()
	{
		PickedObjectList pickedObjects = this.eventSource.getObjectsAtCurrentPosition();
		if (pickedObjects != null)
		{
			PickedObject top =  pickedObjects.getTopPickedObject();
			if (top != null && top.isTerrain())
			{
				return top.getPosition();
			}
		}
		return null;
	}
*/
	protected Vec4 computeSelectedPointAt(Point point)
	{
		if (this.getSelectedPosition() == null)
		{
			return null;
		}

		BasicView view = (BasicView) this.eventSource.getView();
		if (view == null)
		{
			return null;
		}
		// Reject a selected position if its elevation is above the eye elevation. When that happens, the user is
		// essentially dragging along the inside of a sphere, and the effects of dragging are reversed. To the user
		// this behavior appears unpredictable.
		double elevation = this.getSelectedPosition().elevation;
		if (view.getEyePosition().elevation <= elevation)
		{
			return null;
		}

		// Intersect with a somewhat larger or smaller Globe which will pass through the selected point, but has the
		// same proportions as the actual Globe. This will simulate dragging the selected position more accurately.
		Line ray = new Line();
		view.computeRayFromScreenPoint(point, ray);
		Intersection[] intersections = this.eventSource.getModel().getGlobe().intersect(ray);
		if (intersections == null || intersections.length == 0)
		{
			return null;
		}

		return ray.nearestIntersectionPoint(intersections);
	}

	protected LatLon getChangeInLocation(Point point1, Point point2, Vec4 vec1, Vec4 vec2)
	{
		// Modify the distance we'll actually travel based on the slope of world distance travelled to screen
		// distance travelled . A large slope means the user made a small change in screen space which resulted
		// in a large change in world space. We want to reduce the impact of that change to something reasonable.

		double dragSlope = this.computeDragSlope(point1, point2, vec1, vec2);
		double dragSlopeFactor = this.getDragSlopeFactor();
		double scale = 1.0 / (1.0 + dragSlopeFactor * dragSlope * dragSlope);

		Position pos1 = this.eventSource.getModel().getGlobe().computePositionFromPoint(vec1);
		Position pos2 = this.eventSource.getModel().getGlobe().computePositionFromPoint(vec2);
		LatLon adjustedLocation = LatLon.interpolateGreatCircle(scale, pos1, pos2);

		// Return the distance to travel in angular degrees.
		return pos1.subtract(adjustedLocation);
	}

	public double computeDragSlope(Point point1, Point point2, Vec4 vec1, Vec4 vec2)
	{
		BasicView view = (BasicView) eventSource.getView();
		if (view == null)
		{
			return 0.0;
		}

		// Compute the screen space distance between point1 and point2.
		double dx = point2.x - point1.x;
		double dy = point2.y - point1.y;
		double pixelDistance = Math.sqrt(dx * dx + dy * dy);

		// Determine the distance from the eye to the point on the forward vector closest to vec1 and vec2
		double d = view.getEyePoint().distanceTo3(vec1);
		// Compute the size of a screen pixel at the nearest of the two distances.
		double pixelSize = view.computePixelSizeAtDistance(d);

		// Return the ratio of world distance to screen distance.
		double slope = vec1.distanceTo3(vec2) / (pixelDistance * pixelSize);
		if (slope < 1.0)
			slope = 1.0;

		return slope - 1.0;
	}

	protected void onHorizontalTranslateAbs(Angle latitudeChange, Angle longitudeChange)
	{
		stopAnimations();

		BasicView view = (BasicView) this.eventSource.getView();
		if (view == null) // include this test to ensure any derived implementation performs it
		{
			return;
		}

		if (latitudeChange.equals(Angle.ZERO) && longitudeChange.equals(Angle.ZERO))
		{
			return;
		}

		Position newPosition = view.getLookAtPosition().add(new Position(
					latitudeChange, longitudeChange, 0.0));

		view.setLookAtPosition(newPosition);
	}

	protected void onHorizontalTranslateRel(double forwardInput, double sideInput, double totalForwardInput, double totalSideInput)
	{
		stopAnimations();

		// Normalize the forward and right magnitudes.
		double length = Math.sqrt(forwardInput * forwardInput + sideInput * sideInput);
		if (length > 0.0)
		{
			forwardInput /= length;
			sideInput /= length;
		}

		Point point = touchPoint;
		Point lastPoint = previousTouchPoint;
		if (getSelectedPosition() == null)
		{
			// Compute the current selected position if none exists. This happens if the user starts dragging when
			// the cursor is off the globe, then drags the cursor onto the globe.
			setSelectedPosition(computeSelectedPosition());
		}
		else if (computeSelectedPosition() == null)
		{
			// User dragged the cursor off the globe. Clear the selected position to ensure a new one will be
			// computed if the user drags the cursor back to the globe.
			setSelectedPosition(null);
		}
		else if (computeSelectedPointAt(point) == null || computeSelectedPointAt(lastPoint) == null)
		{
			// User selected a position that is won't work for dragging. Probably the selected elevation is above the
			// eye elevation, in which case dragging becomes unpredictable. Clear the selected position to ensure
			// a new one will be computed if the user drags the cursor to a valid position.
			setSelectedPosition(null);
		}

		Vec4 vec = computeSelectedPointAt(point);
		Vec4 lastVec = computeSelectedPointAt(lastPoint);

		// Cursor is on the globe, pan between the two positions.
		if (vec != null && lastVec != null)
		{
			// Compute the change in view location given two screen points and corresponding world vectors.
			LatLon latlon = getChangeInLocation(lastPoint, point, lastVec, vec);
			onHorizontalTranslateAbs(latlon.getLatitude(), latlon.getLongitude());
			return;
		}

		forwardInput = point.y - lastPoint.y;
		sideInput = -(point.x-lastPoint.x);

		// Cursor is off the globe, we potentially want to simulate globe dragging.
		// or this is a keyboard event.
		Angle forwardChange = Angle.fromDegrees(
				forwardInput * getScaleValueHorizTransRel());
		Angle sideChange = Angle.fromDegrees(
				sideInput * getScaleValueHorizTransRel());
		onHorizontalTranslateRel(forwardChange, sideChange);
	}

	protected void onHorizontalTranslateRel(Angle forwardChange, Angle sideChange)
	{
		BasicView view = (BasicView) this.eventSource.getView();
		if (view == null) // include this test to ensure any derived implementation performs it
		{
			return;
		}

		if (forwardChange.equals(Angle.ZERO) && sideChange.equals(Angle.ZERO))
		{
			return;
		}

		double sinHeading = view.getHeading().sin();
		double cosHeading = view.getHeading().cos();
		double latChange = cosHeading * forwardChange.getDegrees() - sinHeading * sideChange.getDegrees();
		double lonChange = sinHeading * forwardChange.getDegrees() + cosHeading * sideChange.getDegrees();
		Position newPosition = view.getLookAtPosition().add(Position.fromDegrees(latChange, lonChange, 0.0));

		view.setLookAtPosition(newPosition);
	}

	protected double getScaleValueHorizTransRel()
	{
		BasicView view = (BasicView) this.eventSource.getView();
		if (view == null)
		{
			return 0.0;
		}
		double[] range = { 0.00001, 0.2};
		// If this is an OrbitView, we use the zoom value to set the scale
		double radius = this.eventSource.getModel().getGlobe().getRadius();
		double t = getScaleValue(range[0], range[1],
				view.getRange(), 3.0 * radius, true);
		return (t);
	}

	protected double getScaleValue(double minValue, double maxValue,
								   double value, double range, boolean isExp)
	{
		double t = value / range;
		t = t < 0 ? 0 : (t > 1 ? 1 : t);
		if (isExp)
		{
			t = Math.pow(2.0, t) - 1.0;
		}
		return(minValue * (1.0 - t) + maxValue * t);
	}

}