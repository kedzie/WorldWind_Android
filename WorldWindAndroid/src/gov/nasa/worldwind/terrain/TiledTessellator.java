/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import android.graphics.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.util.BufferUtil;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.WWXML;
import java.beans.PropertyChangeEvent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import javax.xml.xpath.XPath;
import org.w3c.dom.Element;
import android.opengl.GLES20;
import android.util.Pair;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 *
 * @author dcollins
 * @version $Id: TiledTessellator.java 842 2012-10-09 23:46:47Z tgaskins $
 */
public class TiledTessellator extends WWObjectImpl
		implements Tessellator, Tile.TileFactory<TiledTessellator.TerrainTile> {

	protected static class TerrainTile extends Tile implements SectorGeometry {
		protected TiledTessellator tessellator;
		protected Extent extent;

		public TerrainTile(Sector sector, Level level, int row, int column, TiledTessellator tessellator) {
			super(sector, level, row, column);
			this.tessellator = tessellator;
		}

		public double getResolution() {
			return this.level.getTexelSize();
		}

		/** {@inheritDoc} */
		public Extent getExtent() {
			return this.extent;
		}

		public void setExtent(Extent extent) {
			this.extent = extent;
		}

		/** {@inheritDoc} */
		public boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result) {
			if (latitude == null) {
				String msg = Logging.getMessage("nullValue.LatitudeIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (longitude == null) {
				String msg = Logging.getMessage("nullValue.LongitudeIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (result == null) {
				String msg = Logging.getMessage("nullValue.ResultIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			return this.tessellator.getSurfacePoint(this, latitude, longitude, result);
		}

		public TerrainGeometry getGeometry(MemoryCache cache) {
			return (TerrainGeometry) cache.get(this.tileKey);
		}

		public void setGeometry(MemoryCache cache, TerrainGeometry geom) {
			cache.put(this.tileKey, geom);
		}

		@Override
		public long getSizeInBytes() {
			// This tile's size in bytes is computed as follows:
			// superclass: variable
			// tessellator: 4 bytes (1 32-bit reference)
			// extent: 4 bytes (1 32-bit reference)
			// total: 8 bytes + superclass' size in bytes

			return 8 + super.getSizeInBytes();
		}

		/** {@inheritDoc} */
		public void render(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.render(dc, this);
		}

		/** {@inheritDoc} */
		public void renderWireframe(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.renderWireframe(dc, this);
		}

		/** {@inheritDoc} */
		public void renderOutline(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.renderOutline(dc, this);
		}

		public void renderBoundingVolume(DrawContext dc)
		{
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.renderBoundingVolume(dc, this);
		}

		public void renderTileID(DrawContext dc)
		{
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}
			this.tessellator.renderTileID(dc, this);
		}

		/** {@inheritDoc} */
		public void beginRendering(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.beginRendering(dc, this);
		}

		/** {@inheritDoc} */
		public void endRendering(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.endRendering(dc, this);
		}

		/** {@inheritDoc} */
		public void pick(DrawContext dc, Point pickPoint) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (pickPoint == null) {
				String msg = Logging.getMessage("nullValue.PointIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.pick(dc, this, pickPoint);
		}

		@Override
		public Intersection[] intersect(Line line)
		{
			return this.tessellator.intersect(this, line);
		}

		@Override
		public Intersection[] intersect(double elevation)
		{
			return this.tessellator.intersect(this, elevation);
		}
	}

	protected static class TerrainTileList extends ArrayList<SectorGeometry> implements SectorGeometryList {
		private static final long serialVersionUID = 7740545209150322934L;
		protected Sector sector;
		protected TiledTessellator tessellator;

		public TerrainTileList(TiledTessellator tessellator) {
			this.tessellator = tessellator;
		}

		/** {@inheritDoc} */
		public Sector getSector() {
			return this.sector;
		}

		public void setSector(Sector sector) {
			this.sector = sector;
		}

		/** {@inheritDoc} */
		public boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result) {
			if (latitude == null) {
				String msg = Logging.getMessage("nullValue.LatitudeIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (longitude == null) {
				String msg = Logging.getMessage("nullValue.LongitudeIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (result == null) {
				String msg = Logging.getMessage("nullValue.ResultIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			return this.tessellator.getSurfacePoint(latitude, longitude, result);
		}

		/** {@inheritDoc} */
		public void beginRendering(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.beginRendering(dc);
		}

		/** {@inheritDoc} */
		public void endRendering(DrawContext dc) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.endRendering(dc);
		}

		/** {@inheritDoc} */
		public void pick(DrawContext dc, Point pickPoint) {
			if (dc == null) {
				String msg = Logging.getMessage("nullValue.DrawContextIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (pickPoint == null) {
				String msg = Logging.getMessage("nullValue.PointIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tessellator.pick(dc, this, pickPoint);
		}

		/**
		 * Determines if and where a ray intersects the geometry.
		 *
		 * @param line the <code>Line</code> for which an intersection is to be found.
		 *
		 * @return the <Vec4> point closest to the ray origin where an intersection has been found or null if no
		 *         intersection was found.
		 */
		public Intersection[] intersect(Line line)
		{
			if (line == null)
			{
				String msg = Logging.getMessage("nullValue.LineIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			ArrayList<SectorGeometry> sglist = new ArrayList<SectorGeometry>(this);

			Intersection[] hits;
			ArrayList<Intersection> list = new ArrayList<Intersection>();
			for (SectorGeometry sg : sglist)
			{
				if (sg.getExtent().intersects(line))
					if ((hits = sg.intersect(line)) != null)
						list.addAll(Arrays.asList(hits));
			}

			int numHits = list.size();
			if (numHits == 0)
				return null;

			hits = new Intersection[numHits];
			list.toArray(hits);

			final Vec4 origin = line.getOrigin();
			Arrays.sort(hits, new Comparator<Intersection>()
			{
				public int compare(Intersection i1, Intersection i2)
				{
					if (i1 == null && i2 == null)
						return 0;
					if (i2 == null)
						return -1;
					if (i1 == null)
						return 1;

					Vec4 v1 = i1.getIntersectionPoint();
					Vec4 v2 = i2.getIntersectionPoint();
					double d1 = origin.distanceTo3(v1);
					double d2 = origin.distanceTo3(v2);
					return Double.compare(d1, d2);
				}
			});
			return hits;
		}

		/**
		 * Determines if and where the geometry intersects the ellipsoid at a given elevation.
		 * <p/>
		 * The returned array of <code>Intersection</code> describes a list of individual segments - two
		 * <code>Intersection</code> for each, corresponding to each geometry triangle that intersects the given elevation.
		 * <p/>
		 * Note that the provided bounding <code>Sector</code> only serves as a 'hint' to avoid processing unnecessary
		 * geometry tiles. The returned intersection list may contain segments outside that sector.
		 *
		 * @param elevation the elevation for which intersections are to be found.
		 * @param sector    the sector inside which intersections are to be found.
		 *
		 * @return a list of <code>Intersection</code> pairs/segments describing a contour line at the given elevation.
		 */
		public Intersection[] intersect(double elevation, Sector sector)
		{
			if (sector == null)
			{
				String message = Logging.getMessage("nullValue.SectorIsNull");
				Logging.error(message);
				throw new IllegalArgumentException(message);
			}

			ArrayList<SectorGeometry> sglist = new ArrayList<SectorGeometry>(this);

			Intersection[] hits;
			ArrayList<Intersection> list = new ArrayList<Intersection>();
			for (SectorGeometry sg : sglist)
			{
				if (sector.intersects(sg.getSector()))
					if ((hits = sg.intersect(elevation)) != null)
						list.addAll(Arrays.asList(hits));
			}

			int numHits = list.size();
			if (numHits == 0)
				return null;

			hits = new Intersection[numHits];
			list.toArray(hits);

			return hits;
		}
	}

	protected static class TerrainGeometry implements Cacheable {
		protected Vec4 referenceCenter = new Vec4();
		protected Matrix transformMatrix = Matrix.fromIdentity();
		protected FloatBuffer points;
		protected final Object vboCacheKey = new Object();
		protected boolean mustRegnerateVbos;
		protected TerrainSharedGeometry sharedGeom;
		protected double verticalExaggeration;

		public TerrainGeometry() {
		}

		public long getSizeInBytes() {
			// This tile's size in bytes is computed as follows:
			// self: 4 bytes (1 32-bit reference)
			// referenceCenter: 36 bytes (1 32-bit reference + 4 64-bit floats)
			// transformMatrix: 132 bytes (1 32-bit reference + 16 64-bit floats)
			// points: 4 bytes + variable (1 32-bit reference + variable num of 32-bit floats)
			// vboCacheKey: 4 bytes (1 32-bit reference)
			// sharedGeom: 4 bytes (1 32-bit reference)
			// total: 184 bytes

			long size = 184;
			size += this.points != null ? 4 * this.points.capacity() : 0;
			return size;
		}
	}

	protected static class TerrainSharedGeometry {
		protected FloatBuffer texCoords;
		protected ShortBuffer indices;
		protected ShortBuffer wireframeIndices;
		protected ShortBuffer outlineIndices;
		protected final Object vboCacheKey = new Object();

		public TerrainSharedGeometry() {
		}
	}

	protected static class TerrainPickGeometry {
		protected Vec4 referenceCenter = new Vec4();
		protected Matrix transformMatrix = Matrix.fromIdentity();
		protected FloatBuffer points;
		protected ByteBuffer colors;
		protected int vertexCount;
		protected int minColorCode;
		protected int maxColorCode;

		public TerrainPickGeometry() {
		}
	}

	protected static final double DEFAULT_DETAIL_HINT_ORIGIN = 1.3;
	protected static Map<Object, TerrainSharedGeometry> sharedGeometry = new HashMap<Object, TerrainSharedGeometry>();
	protected static Map<Object, TerrainPickGeometry> pickGeometry = new HashMap<Object, TerrainPickGeometry>();
	protected static final int PICK_VERTEX_SHADER_PATH = R.raw.vertex_color_vert ;
	protected static final int PICK_FRAGMENT_SHADER_PATH = R.raw.vertex_color_frag;

	protected double detailHintOrigin = DEFAULT_DETAIL_HINT_ORIGIN;
	protected double detailHint;
	protected Globe globe;
	protected LevelSet levels;
	protected List<TerrainTile> topLevelTiles = new ArrayList<TerrainTile>();
	protected TerrainTileList currentTiles = new TerrainTileList(this);
	protected Sector currentCoverage = new Sector();
	// Data structures used to track when the elevation model changes.
	protected List<Sector> expiredSectors = new ArrayList<Sector>();
	protected List<Sector> currentExpiredSectors = new ArrayList<Sector>();
	protected final Object expiredSectorLock = new Object();
	// Temporary properties used to avoid constant reallocation of data used during tile assembly and rendering.
	protected Matrix mvpMatrix = Matrix.fromIdentity();
	protected double[] tileElevations;
	protected double[] tileRowElevations;
	protected Vec4[] tilePoints;
	protected float[] tileCoords;
	protected float[] pointBuffer = new float[12];
	// Properties used for picking.
	protected final Object pickProgramKey = new Object();
	protected boolean pickProgramCreationFailed;
	protected Color pickColor = new Color();
	protected Line pickRay = new Line();
	protected Vec4 pickedTriPoint = new Vec4();
	protected Position pickedTriPos = new Position();
	protected TextRenderer textRenderer;

	public TiledTessellator(AVList params) {
		if (params == null) {
			String msg = Logging.getMessage("nullValue.ParamsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.initWithParams(params);
	}

	public TiledTessellator(Element element) {
		if (element == null) {
			String msg = Logging.getMessage("nullValue.ElementIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.initWithConfigDoc(element);
	}

	protected void initWithParams(AVList params) {
		Object o = params.getValue(AVKey.DETAIL_HINT);
		if (o != null && o instanceof Number) this.detailHint = ((Number) o).doubleValue();

		this.levels = new LevelSet(params);
	}

	protected void initWithConfigDoc(Element element) {
		XPath xpath = WWXML.makeXPath();

		Double d = WWXML.getDouble(element, "DetailHint", xpath);
		if (d != null) this.detailHint = d;

		this.levels = new LevelSet(LevelSet.paramsFromConfigDoc(element));
	}

	public double getDetailHint() {
		return this.detailHint;
	}

	public void setDetailHint(double detailHint) {
		this.detailHint = detailHint;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		// Listen to the Globe's elevation model for changes in elevation model sectors. We mark these sectors as
		// expired and regenerate geometry for tiles intersecting these sectors.
		// noinspection StringEquality
			if (AVKey.ELEVATION_MODEL.equals(event.getPropertyName()) && event.getNewValue() instanceof Tile)
			{
				Logging.info("Marking all tile sector expired for ElevationModel change");
				Sector tileSector = ((Tile) event.getNewValue()).getSector();
				this.markSectorExpired(tileSector);
			}
			else if (AVKey.ELEVATION_MODEL.equals(event.getPropertyName()) && event.getNewValue() instanceof ElevationModel)
			{
				Logging.info("Marking all sectors expired for ElevationModel change");
				for(SectorGeometry tile : currentTiles) {
					markSectorExpired(tile.getSector());
				}
			}
			else if(AVKey.VERTICAL_EXAGGERATION.equals(event.getPropertyName()))
			{
				Logging.info(String.format("Marking all sectors expired for VerticalExaggeration change: %.2f -> %.2f",
						event.getOldValue(), event.getNewValue()));
				for(SectorGeometry tile : currentTiles)
					markSectorExpired(tile.getSector());
			}
	}

	/**
	 * Determines if and where a ray intersects a <code>TerrainTile</code> geometry.
	 *
	 * @param tile the <Code>TerrainTile</code> which geometry is to be tested for intersection.
	 * @param line the ray for which an intersection is to be found.
	 *
	 * @return an array of <code>Intersection</code> sorted by increasing distance from the line origin, or null if no
	 *         intersection was found.
	 */
	protected Intersection[] intersect(TerrainTile tile, Line line)
	{
		if (line == null)
		{
			String msg = Logging.getMessage("nullValue.LineIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		TerrainGeometry geometry = tile.getGeometry(getTerrainGeometryCache());

		if (geometry==null || geometry.points==null)
			return null;

		// Compute 'vertical' plane perpendicular to the ground, that contains the ray
		Vec4 normalV = new Vec4();
		globe.computeSurfaceNormalAtPoint(line.getOrigin(), normalV);
		line.getDirection().cross3(normalV);
		Plane verticalPlane = new Plane(normalV.x, normalV.y, normalV.z, -line.getOrigin().dot3(normalV));
		if (!tile.getExtent().intersects(verticalPlane))
			return null;

		// Compute 'horizontal' plane perpendicular to the vertical plane, that contains the ray
		Vec4 normalH = line.getDirection().cross3(normalV);
		Plane horizontalPlane = new Plane(normalH.x, normalH.y, normalH.z, -line.getOrigin().dot3(normalH));
		if (!tile.getExtent().intersects(horizontalPlane))
			return null;

		Intersection[] hits;
		ArrayList<Intersection> list = new ArrayList<Intersection>();

		short[] indices = new short[geometry.sharedGeom.indices.limit()];
		float[] coords = new float[geometry.points.limit()];
		geometry.sharedGeom.indices.rewind();
		geometry.points.rewind();
		geometry.sharedGeom.indices.get(indices, 0, indices.length);
		geometry.points.get(coords, 0, coords.length);
		geometry.sharedGeom.indices.rewind();
		geometry.points.rewind();

		int trianglesNum = geometry.sharedGeom.indices.capacity() - 2;
		double centerX = geometry.referenceCenter.x;
		double centerY = geometry.referenceCenter.y;
		double centerZ = geometry.referenceCenter.z;

		// Compute maximum cell size based on tile delta lat, density and globe radius
		double effectiveRadiusVertical = tile.extent.getEffectiveRadius(verticalPlane);
		double effectiveRadiusHorizontal = tile.extent.getEffectiveRadius(horizontalPlane);

		// Loop through all tile cells - triangle pairs
		int startIndex = (tile.getWidth() + 2) * 2 + 6; // skip first skirt row and a couple degenerate cells
		int endIndex = trianglesNum - startIndex; // ignore last skirt row and a couple degenerate cells
		int k = -1;
		for (int i = startIndex; i < endIndex; i += 2)
		{
			// Skip skirts and degenerate triangle cells - based on index sequence.
			k = k == tile.getWidth() - 1 ? -4 : k + 1; // density x terrain cells interleaved with 4 skirt and degenerate cells.
			if (k < 0)
				continue;

			// Triangle pair diagonal - v1 & v2
			int vIndex = 3 * indices[i + 1];
			Vec4 v1 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			vIndex = 3 * indices[i + 2];
			Vec4 v2 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			Vec4 cellCenter = Vec4.mix3(.5, v1, v2);

			// Test cell center distance to vertical plane
			if (Math.abs(verticalPlane.distanceTo(cellCenter)) > effectiveRadiusVertical)
				continue;

			// Test cell center distance to horizontal plane
			if (Math.abs(horizontalPlane.distanceTo(cellCenter)) > effectiveRadiusHorizontal)
				continue;

			// Prepare to test triangles - get other two vertices v0 & v3
			Vec4 p = new Vec4();
			vIndex = 3 * indices[i];
			Vec4 v0 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			vIndex = 3 * indices[i + 3];
			Vec4 v3 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			// Test triangle 1 intersection w ray
			Triangle t = new Triangle(v0, v1, v2);
			if (t.intersect(line, p))
			{
				list.add(new Intersection(p, false));
			}

			// Test triangle 2 intersection w ray
			t = new Triangle(v1, v2, v3);
			if (t.intersect(line, p))
			{
				list.add(new Intersection(p, false));
			}
		}

		int numHits = list.size();
		if (numHits == 0)
			return null;

		hits = new Intersection[numHits];
		list.toArray(hits);

		final Vec4 origin = line.getOrigin();
		Arrays.sort(hits, new Comparator<Intersection>()
		{
			public int compare(Intersection i1, Intersection i2)
			{
				if (i1 == null && i2 == null)
					return 0;
				if (i2 == null)
					return -1;
				if (i1 == null)
					return 1;

				Vec4 v1 = i1.getIntersectionPoint();
				Vec4 v2 = i2.getIntersectionPoint();
				double d1 = origin.distanceTo3(v1);
				double d2 = origin.distanceTo3(v2);
				return Double.compare(d1, d2);
			}
		});

		return hits;
	}

	protected Intersection[] intersect(TerrainTile tile, double elevation)
	{
		TerrainGeometry geometry = tile.getGeometry(getTerrainGeometryCache());

		if (geometry==null || geometry.points==null)
			return null;

		// Check whether the tile includes the intersection elevation - assume cylinder as Extent
		// TODO: replace this test with a generic test against Extent
		if (tile.getExtent() instanceof Cylinder)
		{
			Cylinder cylinder = ((Cylinder) tile.getExtent());
			if (!(globe.isPointAboveElevation(cylinder.getBottomCenter(), elevation)
					^ globe.isPointAboveElevation(cylinder.getTopCenter(), elevation)))
				return null;
		}

		Intersection[] hits;
		ArrayList<Intersection> list = new ArrayList<Intersection>();

		short[] indices = new short[geometry.sharedGeom.indices.limit()];
		float[] coords = new float[geometry.points.limit()];
		geometry.sharedGeom.indices.rewind();
		geometry.points.rewind();
		geometry.sharedGeom.indices.get(indices, 0, indices.length);
		geometry.points.get(coords, 0, coords.length);
		geometry.sharedGeom.indices.rewind();
		geometry.points.rewind();

		int trianglesNum = geometry.sharedGeom.indices.capacity() - 2;
		double centerX = geometry.referenceCenter.x;
		double centerY = geometry.referenceCenter.y;
		double centerZ = geometry.referenceCenter.z;

		// Loop through all tile cells - triangle pairs
		int startIndex = (tile.getWidth() + 2) * 2 + 6; // skip first skirt row and a couple degenerate cells
		int endIndex = trianglesNum - startIndex; // ignore last skirt row and a couple degenerate cells
		int k = -1;
		for (int i = startIndex; i < endIndex; i += 2)
		{
			// Skip skirts and degenerate triangle cells - based on indice sequence.
			k = k == tile.getWidth() - 1 ? -4 : k + 1; // density x terrain cells interleaved with 4 skirt and degenerate cells.
			if (k < 0)
				continue;

			// Get the four cell corners
			int vIndex = 3 * indices[i];
			Vec4 v0 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			vIndex = 3 * indices[i + 1];
			Vec4 v1 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			vIndex = 3 * indices[i + 2];
			Vec4 v2 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			vIndex = 3 * indices[i + 3];
			Vec4 v3 = new Vec4(
					coords[vIndex++] + centerX,
					coords[vIndex++] + centerY,
					coords[vIndex] + centerZ);

			Intersection[] inter;
			// Test triangle 1 intersection
			if ((inter = globe.intersect(new Triangle(v0, v1, v2), elevation)) != null)
			{
				list.add(inter[0]);
				list.add(inter[1]);
			}

			// Test triangle 2 intersection
			if ((inter = globe.intersect(new Triangle(v1, v2, v3), elevation)) != null)
			{
				list.add(inter[0]);
				list.add(inter[1]);
			}
		}

		int numHits = list.size();
		if (numHits == 0)
			return null;

		hits = new Intersection[numHits];
		list.toArray(hits);

		return hits;
	}

	protected boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result) {
		for (SectorGeometry tile : this.currentTiles) {
			if (tile.getSurfacePoint(latitude, longitude, result)) // Each tile tests the location against its sector.
				return true;
		}

		return false;
	}

	protected boolean getSurfacePoint(TerrainTile tile, Angle latitude, Angle longitude, Vec4 result) {
		Sector tileSector = tile.getSector();

		if (!tileSector.contains(latitude, longitude)) return false; // The location is not on the specified tile.

		MemoryCache cache = this.getTerrainGeometryCache();
		TerrainGeometry geom = tile.getGeometry(cache);

		if (geom == null) return false; // The tile geometry has not been computed yet, or has been evicted from the cache.

		this.computeSurfacePoint(tile, geom, latitude, longitude, result);
		return true;
	}

	public SectorGeometryList tessellate(DrawContext dc) {
		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.assembleExpiredSectors();
		this.assembleTiles(dc);
		this.currentExpiredSectors.clear();

		return this.currentTiles;
	}

	public TerrainTile createTile(Sector sector, Level level, int row, int column) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (level == null) {
			String msg = Logging.getMessage("nullValue.LevelIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (row < 0) {
			String msg = Logging.getMessage("generic.RowIndexOutOfRange", row);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (column < 0) {
			String msg = Logging.getMessage("generic.ColumnIndexOutOfRange", column);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return new TerrainTile(sector, level, row, column, this);
	}

	protected void assembleTiles(DrawContext dc) {
		// Start with an empty list of tile and empty coverage. An empty coverage sector is handled as a special case
		// below and in addTile. We use the sector's empty state instead of setting the coverage to null in order to
		// avoid reallocating a sector every frame.
		this.currentTiles.clear();
		this.currentCoverage.setDegrees(0, 0, 0, 0);

		if (this.topLevelTiles.isEmpty()) this.createTopLevelTiles(dc);

		for (int i = 0; i < this.topLevelTiles.size(); i++) {
			Tile tile = this.topLevelTiles.get(i);

			this.updateTileExtent(dc, (TerrainTile) tile);

			if (this.intersectsFrustum(dc, (TerrainTile) tile)) this.addTileOrDescendants(dc, (TerrainTile) tile);
		}

		this.currentTiles.setSector(this.currentCoverage.isEmpty() ? null : this.currentCoverage);
	}

	protected void createTopLevelTiles(DrawContext dc) {
		if (this.levels.getFirstLevel() == null) {
			Logging.warning(Logging.getMessage("generic.FirstLevelIsNull"));
			return;
		}

		if(globe==null) {
			this.globe = dc.getGlobe();
			dc.getGlobe().getElevationModel().addPropertyChangeListener(AVKey.ELEVATION_MODEL, this);
			dc.addPropertyChangeListener(AVKey.VERTICAL_EXAGGERATION, this);
		}

		Logging.verbose("Creating top level surface tiles");

		this.topLevelTiles.clear();
		// TODO Tile.createTilesForLevel(this.levels.getFirstLevel(), this.levels.getSector(), this, this.topLevelTiles);
		Tile.createTilesForLevel(this.levels.getFirstLevel(), this.levels.getSector(), this, this.topLevelTiles, this.levels.getTileOrigin());
		Logging.verbose("Finished top level creation: #" + topLevelTiles.size());
	}

	protected void addTileOrDescendants(DrawContext dc, TerrainTile tile) {
		this.updateTileExtent(dc, tile);

		if (this.meetsRenderCriteria(dc, tile)) {
			this.addTile(dc, tile);
			return;
		}

		MemoryCache cache = this.getTerrainTileCache();

		Tile[] subTiles = tile.subdivide(this.levels.getLevel(tile.getLevelNumber() + 1), cache, this);
		for (Tile child : subTiles) {
			// Put all sub-tiles in the terrain tile cache to avoid repeatedly allocating them each frame. Top level
			// tiles are not cached because they are held in the topLevelTiles list. Sub tiles are placed in the cache
			// here, and updated when their terrain geometry changes.
			if (!cache.contains(child.getTileKey())) cache.put(child.getTileKey(), child);

			// Add descendant tiles that intersect the LevelSet's sector and intersect the viewing frustum. If half or
			// more of the tile (in either latitude or longitude) extends beyond the LevelSet's sector, then two or
			// three of its children will be entirely outside the LevelSet's sector.
			if (this.levels.getSector().intersects(child.getSector()) && this.intersectsFrustum(dc, (TerrainTile) child)) {
				this.addTileOrDescendants(dc, (TerrainTile) child);
			}
		}
		tile.clearChildList();
	}

	protected void addTile(DrawContext dc, TerrainTile tile) {
		if (this.mustRegenerateGeometry(dc, tile)) this.regenerateGeometry(dc, tile);

		this.currentTiles.add(tile);

		// If the current coverage sector is empty, just set the coverage to the tile's sector. This ensures that we
		// don't create a coverage sector that always contains the lat/lon origin. Otherwise set the current coverage
		// to the union of itself with the tile's sector.
		if (this.currentCoverage.isEmpty()) this.currentCoverage.set(tile.getSector());
		else this.currentCoverage.union(tile.getSector());
	}

	protected boolean intersectsFrustum(DrawContext dc, TerrainTile tile) {
		Extent extent = tile.getExtent();
		return extent == null || dc.getView().getFrustumInModelCoordinates().intersects(extent);
	}

	protected boolean meetsRenderCriteria(DrawContext dc, TerrainTile tile) {
		return this.levels.isFinalLevel(tile.getLevelNumber()) || this.atBestResolution(dc, tile) || !this.needToSubdivide(dc, tile);
	}

	protected boolean atBestResolution(DrawContext dc, TerrainTile tile) {
		return tile.getResolution() <= dc.getGlobe().getBestResolution(tile.getSector());
	}

	protected boolean needToSubdivide(DrawContext dc, TerrainTile tile) {
		return tile.mustSubdivide(dc, this.getDetailFactor());
	}

	protected double getDetailFactor() {
		return this.detailHintOrigin + this.detailHint;
	}

	protected void updateTileExtent(DrawContext dc, TerrainTile tile) {
		boolean expired = this.isExpired(dc, tile);

		if (tile.getExtent() == null || expired) {
			tile.setExtent(this.computeTileExtent(dc, tile));
		}

		// Update the tile's reference points.
		Vec4[] points = tile.getReferencePoints();
		if (points == null || expired) {
			points = new Vec4[] { new Vec4(), new Vec4(), new Vec4(), new Vec4(), new Vec4() };
			tile.getSector().computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration(), points);
			tile.getSector().computeCentroidPoint(dc.getGlobe(), dc.getVerticalExaggeration(), points[4]);
			tile.setReferencePoints(points);
		}
	}

	protected Extent computeTileExtent(DrawContext dc, TerrainTile tile) {
		return Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), tile.getSector());
	}

	protected boolean mustRegenerateGeometry(DrawContext dc, TerrainTile tile) {
		return this.isExpired(dc, tile);
	}

	protected void assembleExpiredSectors() {
		synchronized (this.expiredSectorLock) {
			this.currentExpiredSectors.addAll(this.expiredSectors);
			this.expiredSectors.clear();
		}
	}

	protected void markSectorExpired(Sector sector) {
		synchronized (this.expiredSectorLock) {
			this.expiredSectors.add(sector);
		}
	}

	protected boolean isExpired(DrawContext dc, TerrainTile tile) {
		TerrainGeometry geom = tile.getGeometry(getTerrainGeometryCache());
		if(geom==null) {
//			if(WorldWindowImpl.DEBUG)
//				Logging.verbose("Generating tile geometry for first time: " + tile);
			return true;
		}
		if(geom.verticalExaggeration != dc.getVerticalExaggeration()) {
			if(WorldWindowImpl.DEBUG)
				Logging.verbose(String.format("Tile has expired due to Vertical Exaggeration. old: %.2f  new: %.2f",
						geom.verticalExaggeration, dc.getVerticalExaggeration()));
			return true;
		}

		if (this.currentExpiredSectors.isEmpty()) return false;

		Sector tileSector = tile.getSector();
		for (Sector sector : this.currentExpiredSectors) {
			if (tileSector.intersects(sector)) {
				if(WorldWindowImpl.DEBUG)
					Logging.verbose("Tile has expired due to expired sector: " + sector);
				return true;
			}
		}

		return false;
	}

	protected void regenerateGeometry(DrawContext dc, TerrainTile tile) {
		MemoryCache cache = this.getTerrainGeometryCache();
		TerrainGeometry geom = tile.getGeometry(cache);
		if (geom == null) geom = new TerrainGeometry();

		this.buildTileVertices(dc, tile, geom);
		this.buildSharedGeometry(tile, geom);
		geom.verticalExaggeration = dc.getVerticalExaggeration();
		// Update the geometry's cached size.
		tile.setGeometry(cache, geom);
		if(WorldWindowImpl.DEBUG)
			Logging.verbose("added tile geometry: " + cache.getNumObjects());
	}

	/**
	 * Returns the memory cache used to cache terrain tiles, initializing the cache if it doesn't yet exist.
	 *
	 * @return the memory cache associated with terrain tiles.
	 */
	protected MemoryCache getTerrainTileCache() {
		if (!WorldWind.getMemoryCacheSet().contains(TerrainTile.class.getName())) {
			long size = Configuration.getLongValue(AVKey.SECTOR_GEOMETRY_TILE_CACHE_SIZE);
			MemoryCache cache = new BasicMemoryCache((long) (0.8 * size), size);
			cache.setName("Tessellator Tiles");
			WorldWind.getMemoryCacheSet().put(TerrainTile.class.getName(), cache);
		}

		return WorldWind.getMemoryCacheSet().get(TerrainTile.class.getName());
	}

	/**
	 * Returns the memory cache used to cache terrain geometry, initializing the cache if it doesn't yet exist.
	 *
	 * @return the memory cache associated with terrain geometry.
	 */
	protected MemoryCache getTerrainGeometryCache() {
		if (!WorldWind.getMemoryCacheSet().contains(TerrainGeometry.class.getName())) {
			long size = Configuration.getLongValue(AVKey.SECTOR_GEOMETRY_CACHE_SIZE);
			MemoryCache cache = new BasicMemoryCache((long) (0.8 * size), size);
			cache.setName("Tessellator Geometry");
			WorldWind.getMemoryCacheSet().put(TerrainGeometry.class.getName(), cache);
			if(WorldWindowImpl.DEBUG) {
			cache.addCacheListener(new MemoryCache.CacheListener() {
				@Override
				public void entryRemoved(Object key, Object value) {
					Logging.verbose("TerrainGeometryCache entry removed: " + key);
				}

				@Override
				public void removalException(Throwable exception, Object key, Object value) {

				}
			});
			}
		}

		return WorldWind.getMemoryCacheSet().get(TerrainGeometry.class.getName());
	}

	protected void buildTileVertices(DrawContext dc, TerrainTile tile, TerrainGeometry geom) {
		// The WWAndroid terrain tessellator attempts to improves upon the WWJ tessellator's vertex construction
		// performance by exploiting the fact that each terrain tile is a regular geographic grid. The following three
		// critical differences have improved the performance of buildTileVertices by approximately 10x (from ~8ms to
		// ~0.7ms on a Samsung Galaxy Tab 10.1 32GB):
		//
		// 1) Avoid explicitly computing and storing the tile's locations by adding methods to Globe that exploit the
		// tile's grid:
		// Globe.getElevations(Sector sector, int numLat, int numLon, double targetResolution, double[] buffer)
		// Globe.computePointsFromPositions(Sector sector, int numLat, int numLon, double[] metersElevation, Vec4[] result).
		//
		// 2) Compute the world Cartesian points for each row in one call to Globe.computePointsFromPositions. Since
		// each row has constant latitude, the implementation of this method can reduce the number of computations by
		// computing latitude dependent values once per row.
		//
		// 3) Putting the world Cartesian points for each row into the TerrainGeometry's point buffer in bulk using a
		// temporary float array. This is necessary on Android because the performance of FloatBuffer.put(float) is
		// slow. Putting the points in bulk improves performance by approximately 2x (from ~1.4ms to ~0.7ms on a
		// Samsung Galaxy Tab 10.1 32GB). Note that this change would likely yield no improvement for the desktop
		// based World Wind Java.

		// Convert from the tile's cell width and height to a number of latitude and longitude vertices. The tile width
		// and height indicates the number of cell rows and columns in the tile. We add one row and column of vertices
		// because there is a row/column of vertices in between each cell.
		int numLat = tile.getLevel().getTileHeight() + 1;
		int numLon = tile.getLevel().getTileWidth() + 1;

		// Allocate arrays to hold the elevations for a tile and for each row. These arrays are properties of the
		// tessellator to avoid constantly reallocating them each time a tile is updated.
		if (this.tileElevations == null || this.tileElevations.length < numLat * numLon) this.tileElevations = new double[numLat * numLon];
		if (this.tileRowElevations == null || this.tileRowElevations.length < numLon) this.tileRowElevations = new double[numLon];

		// Get the elevation values for the tile from the Globe. Any elevations that are unknown or outside the Globe's
		// elevation model are assigned the value 0.0.
		Globe globe = dc.getGlobe();
		globe.getElevations(tile.getSector(), numLat, numLon, tile.getResolution(), this.tileElevations);

		// Adjust the tile's elevations and min elevation by the DrawContext's vertical exaggeration. We skip this step
		// if the vertical exaggeration is 1.0.
		//
		// Note: Previous versions of RectangularTessellator in the World Wind Java project contained a bug where
		// Globe.getMinElevation returned a value greater than zero if the Globe's elevation model did not span the
		// entire globe. To work around that bug, RectangularTessellator would apply the verticalExaggeration to the
		// skirt elevation only when the Globe's min elevation was less than zero, or the verticalExaggeration was less
		// than zero. That bug has been fixed in the WWAndroid project, and we can rely on Globe to return a min
		// elevation of 0 if the elevation model does not span the entire globe.
		double verticalExaggeration = dc.getVerticalExaggeration();
		double minElevation = globe.getMinElevation();
		if (verticalExaggeration != 1.0) {
			for (int i = 0; i < numLat * numLon; i++) {
				this.tileElevations[i] *= verticalExaggeration;
			}

			minElevation *= verticalExaggeration;
		}

		// Compute a local coordinate origin for the tile's world Cartesian points. We use this origin to keep each
		// world coordinate small in order to achieve the resolution we need on the Gpu.
		tile.getSector().computeCentroidPoint(globe, dc.getVerticalExaggeration(), geom.referenceCenter);
		geom.transformMatrix.setTranslation(geom.referenceCenter);

		// Re-use the tile's existing vertex buffer whenever possible. Create a new buffer if one has not been set or if
		// the tile density has changed. We clear the buffer if it is non-null and has enough capacity to ensure that
		// the previous limit does not interfere with what the new limit should be after filling the buffer. We add two
		// rows and columns of vertices to provide an outer row/column for the tile skirt.
		int numPoints = (numLat + 2) * (numLon + 2);
		if (geom.points == null || geom.points.capacity() < 3 * numPoints) geom.points = BufferUtil.newFloatBuffer(3 * numPoints);
		geom.points.clear();

		double minLat = tile.getSector().minLatitude.degrees;
		double maxLat = tile.getSector().maxLatitude.degrees;
		double minLon = tile.getSector().minLongitude.degrees;
		double maxLon = tile.getSector().maxLongitude.degrees;
		// Vertex latitudes and longitudes are separated by the cell latitude and longitude delta.
		double deltaLat = tile.getSector().getDeltaLatDegrees() / tile.getLevel().getTileHeight();
		Sector rowSector = new Sector();

		// Add redundant points with the row's minimum latitude. These points are used to display the tile's skirt, and
		// have the same locations as the first row, but are assigned the minimum elevation instead of the actual
		// elevations. buildTileRowVertices handles adding the redundant columns for the tile's skirt.
		rowSector.setDegrees(minLat, minLat, minLon, maxLon);
		Arrays.fill(this.tileRowElevations, minElevation);
		this.buildTileRowVertices(dc, rowSector, numLon, this.tileRowElevations, minElevation, geom);

		double lat = minLat;
		int elevOffset = 0;
		for (int j = 0; j < numLat; j++) {
			// Explicitly set the first and last row to minLat and maxLat, respectively, rather than using the
			// accumulated lat value. We do this to ensure that the edges of adjacent tiles line up perfectly.
			if (j == 0) lat = minLat;
			else if (j == numLat - 1) lat = maxLat;
			else lat += deltaLat;

			// Process each tile row in bulk.
			rowSector.setDegrees(lat, lat, minLon, maxLon);
			System.arraycopy(this.tileElevations, elevOffset, this.tileRowElevations, 0, numLon);
			this.buildTileRowVertices(dc, rowSector, numLon, this.tileRowElevations, minElevation, geom);

			elevOffset += numLon;
		}

		// Add redundant points with the row's maximum latitude. These points are used to display the tile's skirt, and
		// have the same locations as the last row, but are assigned the minimum elevation instead of the actual
		// elevations. buildTileRowVertices handles adding the redundant columns for the tile's skirt.
		rowSector.setDegrees(maxLat, maxLat, minLon, maxLon);
		Arrays.fill(this.tileRowElevations, minElevation);
		this.buildTileRowVertices(dc, rowSector, numLon, this.tileRowElevations, minElevation, geom);

		// Set the limit to the current position then set the position to zero. We flip the buffer because its capacity
		// may be greater than the space needed, and the GL commands that ready this buffer rely on the limit to
		// determine how many buffer elements to read.
		geom.points.flip();
		geom.mustRegnerateVbos = true;
	}

	protected void buildTileRowVertices(DrawContext dc, Sector rowSector, int width, double[] elevations, double minElevation, TerrainGeometry geom) {
		// Allocate an array of points that hold the Cartesian coordinates for each XYZ point in this row. The array
		// is a property of this tessellator to avoid constantly reallocating it each time a tile is updated.
		if (this.tilePoints == null || this.tilePoints.length < width) {
			this.tilePoints = new Vec4[width];
			for (int i = 0; i < width; i++) {
				this.tilePoints[i] = new Vec4();
			}
		}

		// Allocate an array of floats to hold the combined coordinates of each point. We populate this array then add
		// it to the tile's point buffer in bulk. Adding an entire row of points into the FloatBuffer using a temporary
		// array is approximately 2x faster than adding each coordinate individually.
		int numCoords = 3 * (width + 2);
		if (this.tileCoords == null || this.tileCoords.length < numCoords) this.tileCoords = new float[numCoords];

		Globe globe = dc.getGlobe();
		int index = 0;

		// Add a redundant point with the row's minimum latitude. This point is used to display the tile's skirt, and
		// has the same location as the row's first location, but is assigned the minimum elevation instead of the
		// location's actual elevation. We subtract the tile's reference center from the Cartesian point to keep its
		// values as near to zero as possible. This enables us to achieve the resolution we need on the Gpu.
		globe.computePointFromPosition(rowSector.minLatitude, rowSector.minLongitude, minElevation, this.tilePoints[0]);
		this.tilePoints[0].subtract3AndSet(geom.referenceCenter);
		this.tilePoints[0].toArray3f(this.tileCoords, index);
		index += 3;

		// Add points for each location in the row. We subtract the tile's reference center from the Cartesian point to
		// keep its values as near to zero as possible. This enables us to achieve the resolution we need on the Gpu.
		globe.computePointsFromPositions(rowSector, 1, width, elevations, this.tilePoints);
		for (int i = 0; i < width; i++) {
			this.tilePoints[i].subtract3AndSet(geom.referenceCenter);
			this.tilePoints[i].toArray3f(this.tileCoords, index);
			index += 3;
		}

		// Add a redundant point with the row's maximum latitude. This point is used to display the tile's skirt, and
		// has the same location as the row's last location, but is assigned the minimum elevation instead of the
		// location's actual elevation. We subtract the tile's reference center from the Cartesian point to keep its
		// values as near to zero as possible. This enables us to achieve the resolution we need on the Gpu.
		globe.computePointFromPosition(rowSector.minLatitude, rowSector.maxLongitude, minElevation, this.tilePoints[0]);
		this.tilePoints[0].subtract3AndSet(geom.referenceCenter);
		this.tilePoints[0].toArray3f(this.tileCoords, index);
		index += 3;

		// Put the row's points into the tile's point buffer in bulk. Adding an entire row of points into the
		// FloatBuffer using a temporary array is approximately 2x faster than adding each coordinate individually.
		geom.points.put(this.tileCoords, 0, numCoords);
	}

	protected void buildSharedGeometry(TerrainTile tile, TerrainGeometry geom) {
		int tileWidth = tile.getWidth();
		int tileHeight = tile.getHeight();
		Object key = Pair.create(tileWidth, tileHeight);

		TerrainSharedGeometry sharedGeom = sharedGeometry.get(key);
		if (sharedGeom == null) {
			sharedGeom = new TerrainSharedGeometry();
			sharedGeom.texCoords = this.buildTexCoords(tileWidth, tileHeight);
			sharedGeom.indices = this.buildIndices(tileWidth, tileHeight);
			sharedGeom.wireframeIndices = this.buildWireframeIndices(tileWidth, tileHeight);
			sharedGeom.outlineIndices = this.buildOutlineIndices(tileWidth, tileHeight);
			sharedGeometry.put(key, sharedGeom);
		}

		geom.sharedGeom = sharedGeom;
	}

	protected FloatBuffer buildTexCoords(int tileWidth, int tileHeight) {
		// The tile width and height indicates the number of cell rows and columns in the tile. We add one row and
		// column of vertices because there is a row/column of vertices in between each cell. We add two rows and
		// columns of vertices to provide an outer row/column for the tile skirt.
		int numLat = tileWidth + 3;
		int numLon = tileHeight + 3;
		int numPoints = numLat * numLon;
		FloatBuffer texCoords = BufferUtil.newFloatBuffer(2 * numPoints);

		double minS = 0;
		double maxS = 1;
		double minT = 0;
		double maxT = 1;
		double deltaS = (maxS - minS) / tileWidth;
		double deltaT = (maxT - minT) / tileHeight;

		double s = minS; // Horizontal texture coordinate; varies along tile width or longitude.
		double t = minT; // Vertical texture coordinate; varies along tile height or latitude.

		for (int j = 0; j < numLat; j++) {
			if (j <= 1) // First two columns repeat the min T-coordinate to provide a column for the skirt.
				t = minT;
			else if (j >= numLat - 2) // Last two columns repeat the max T-coordinate to provide a column for the skirt.
				t = maxT;
			else t += deltaT; // Non-boundary latitudes are separated by the cell latitude delta.

			for (int i = 0; i < numLon; i++) {
				if (i <= 1) // First two rows repeat the min S-coordinate to provide a row for the skirt.
					s = minS;
				else if (i >= numLon - 2) // Last two rows repeat the max S-coordinate to provide a row for the skirt.
					s = maxS;
				else s += deltaS; // Non-boundary longitudes are separated by the cell longitude delta.

				texCoords.put((float) s).put((float) t);
			}
		}

		texCoords.rewind();
		return texCoords;
	}

	protected ShortBuffer buildIndices(int tileWidth, int tileHeight) {
		// The tile width and height indicates the number of cell rows and columns in the tile. We add one row and
		// column of vertices because there is a row/column of vertices in between each cell. We add two rows and
		// columns of vertices to provide an outer row/column for the tile skirt.
		int numLat = tileHeight + 3;
		int numLon = tileWidth + 3;

		// Allocate a native short buffer to hold the indices used to draw a tile of the specified width and height as
		// a triangle strip. Shorts are the largest primitive that OpenGL ES allows for an index buffer. The largest
		// tileWidth and tileHeight that can be indexed by a short is 256x256 (excluding the extra rows and columns to
		// convert between cell count and vertex count, and the extra rows and columns for the tile skirt).
		int numIndices = 2 * (numLat - 1) * numLon + 2 * (numLat - 2);
		ShortBuffer indices = BufferUtil.newShortBuffer(numIndices);

		for (int j = 0; j < numLat - 1; j++) {
			if (j != 0) {
				// Attach the previous and next triangle strips by repeating the last and first vertices of the previous
				// and current strips, respectively. This creates a degenerate triangle between the two strips which is
				// not rasterized because it has zero area. We don't perform this step when j==0 because there is no
				// previous triangle strip to connect with.
				indices.put((short) ((numLon - 1) + (j - 1) * numLon)); // Last vertex of previous strip.
				indices.put((short) (j * numLon + numLon)); // First vertex of current strip.j
			}

			for (int i = 0; i < numLon; i++) {
				// Create a triangle strip joining each adjacent row of vertices, starting in the lower left corner and
				// proceeding upward. The first vertex starts with the upper row of vertices and moves down to create a
				// counter-clockwise winding order.
				int vertex = i + j * numLon;
				indices.put((short) (vertex + numLon));
				indices.put((short) vertex);
			}
		}

		// Reset the buffer's position to zero since it has been advanced to the limit in the code above. We call rewind
		// instead of flip because the limit is already set the the capacity, and should never be any other value
		// because the buffer is allocated with the exact space needed.
		indices.rewind();
		return indices;
	}

	protected ShortBuffer buildWireframeIndices(int tileWidth, int tileHeight) {
		// The tile width and height indicates the number of cell rows and columns in the tile. We add one row and
		// column of vertices because there is a row/column of vertices in between each cell. This outline ignores the
		// tile skirt and draws only the vertices that appear on the surface.
		int numLat = tileHeight + 1;
		int numLon = tileWidth + 1;

		int numIndices = 2 * numLat * (numLon - 1) + 2 * (numLat - 1) * numLon;
		ShortBuffer indices = BufferUtil.newShortBuffer(numIndices);

		// Add two columns of vertices to the row stride to account for the two additional vertices that provide an
		// outer row/column for the tile skirt.
		int rowStride = numLon + 2;
		// Skip the skirt row and column to start at the first interior vertex.
		int offset = rowStride + 1;

		// Add a line between each column to define the horizontal cell outlines. Starts and ends at the vertices that
		// appear on the surface, thereby ignoring the tile skirt.
		for (int j = 0; j < numLat; j++) {
			for (int i = 0; i < numLon - 1; i++) {
				int vertex = offset + i + j * rowStride;
				indices.put((short) vertex).put((short) (vertex + 1));
			}
		}

		// Add a line between each row to define the vertical cell outlines. Starts and ends at the vertices that appear
		// on the surface, thereby ignoring the tile skirt.
		for (int i = 0; i < numLon; i++) {
			for (int j = 0; j < numLat - 1; j++) {
				int vertex = offset + i + j * rowStride;
				indices.put((short) vertex).put((short) (vertex + rowStride));
			}
		}

		// Reset the buffer's position to zero since it has been advanced to the limit in the code above. We call rewind
		// instead of flip because the limit is already set the the capacity, and should never be any other value
		// because the buffer is allocated with the exact space needed.
		indices.rewind();
		return indices;
	}

	protected ShortBuffer buildOutlineIndices(int tileWidth, int tileHeight) {
		// The tile width and height indicates the number of cell rows and columns in the tile. We add one row and
		// column of vertices because there is a row/column of vertices in between each cell. This outline ignores the
		// tile skirt and draws only the vertices that appear on the surface.
		int numLat = tileHeight + 1;
		int numLon = tileWidth + 1;

		// The outline indices ignore the extra rows and columns for the tile skirt.
		int numIndices = 2 * (numLat - 1) + 2 * numLon - 1;
		ShortBuffer indices = BufferUtil.newShortBuffer(numIndices);

		// Add two columns of vertices to the row stride to account for the two additional vertices that provide an
		// outer row/column for the tile skirt.
		int rowStride = numLon + 2;

		// Bottom row. Offset by rowStride + 1 to start at the lower left corner, ignoring the tile skirt.
		int offset = rowStride + 1;
		for (int i = 0; i < numLon; i++) {
			indices.put((short) (offset + i));
		}

		// Rightmost column. Offset by rowStride - 2 to start at the lower right corner, ignoring the tile skirt. Skips
		// the bottom vertex, which is already included in the bottom row.
		offset = 2 * rowStride - 2;
		for (int j = 1; j < numLat; j++) {
			indices.put((short) (offset + j * rowStride));
		}

		// Top row. Offset by tileHeight* rowStride + 1 to start at the top left corner, ignoring the tile skirt. Skips
		// the rightmost vertex, which is already included in the rightmost column.
		offset = numLat * rowStride + 1;
		for (int i = numLon - 2; i >= 0; i--) {
			indices.put((short) (offset + i));
		}

		// Leftmost column. Offset by rowStride + 1 to start at the lower left corner, ignoring the tile skirt. Skips
		// the topmost vertex, which is already included in the top row.
		offset = rowStride + 1;
		for (int j = numLat - 2; j >= 0; j--) {
			indices.put((short) (offset + j * rowStride));
		}

		// Reset the buffer's position to zero since it has been advanced to the limit in the code above. We call rewind
		// instead of flip because the limit is already set the the capacity, and should never be any other value
		// because the buffer is allocated with the exact space needed.
		indices.rewind();
		return indices;
	}

	protected void beginRendering(DrawContext dc) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) {
			Logging.warning(Logging.getMessage("generic.NoCurrentProgram"));
			return;
		}

		// Enable the program's vertexPoint attribute, if one exists. The data for this attribute is specified when
		// beginRendering is called for each tile.
		int location = program.getAttribLocation("vertexPoint");
		if (location >= 0) GLES20.glEnableVertexAttribArray(location);
		WorldWindowImpl.glCheckError("glEnableVertexAttribArray");

		// Enable the program's vertexTexCoord attribute, if one exists. The data for this attribute is specified when
		// beginRendering is called for each tile.
		location = program.getAttribLocation("vertexTexCoord");
		if (location >= 0) GLES20.glEnableVertexAttribArray(location);
		WorldWindowImpl.glCheckError("glEnableVertexAttribArray");
	}

	protected void endRendering(DrawContext dc) {
		// Restore the array and element array buffer bindings to 0.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		WorldWindowImpl.glCheckError("glBindBuffer");
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		WorldWindowImpl.glCheckError("glBindBuffer");

		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return; // Message logged in beginRendering(DrawContext).

		// Disable the program's vertexPoint attribute, if one exists. This restores the program state modified in
		// beginRendering.
		int location = program.getAttribLocation("vertexPoint");
		if (location >= 0) GLES20.glDisableVertexAttribArray(location);
		WorldWindowImpl.glCheckError("glDisableVertexAttribArray");

		// Disable the program's vertexTexCoord attribute, if one exists. This restores the program state modified in
		// beginRendering.
		location = program.getAttribLocation("vertexTexCoord");
		if (location >= 0) GLES20.glDisableVertexAttribArray(location);
		WorldWindowImpl.glCheckError("glDisableVertexAttribArray");
	}

	protected void beginRendering(DrawContext dc, TerrainTile tile) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return;// Message logged in beginRendering(DrawContext).

		MemoryCache memCache = this.getTerrainGeometryCache();
		GpuResourceCache gpuCache = dc.getGpuResourceCache();

		TerrainGeometry geom = tile.getGeometry(memCache);
		if (geom == null) {
			Logging.warning(Logging.getMessage("Tessellator.SurfaceGeometryNotInCache", tile, memCache.getUsedCapacity()));
			return;
		}

		// TODO: 1) Separate texcoord vbo loading from index vbo.
		// TODO: 2) Put only the fill indices in vbo.
		// TODO: 3) Enable texcoord and fill index vbo in beginRendering.
		this.loadGeometryVbos(dc, geom);
		this.loadSharedGeometryVBOs(dc, geom.sharedGeom);

		// Specify the data for the program's vertexPoint attribute, if one exists. This attribute is enabled in
		// beginRendering.
		int location = program.getAttribLocation("vertexPoint");
		if (location >= 0) {
			int[] vboIds = (int[]) gpuCache.get(geom.vboCacheKey);
			if (vboIds != null) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
				WorldWindowImpl.glCheckError("glBindBuffer");
				GLES20.glVertexAttribPointer(location, 3, GLES20.GL_FLOAT, false, 0, 0);
				WorldWindowImpl.glCheckError("glVertexAttribPointer");
			} else {
				String msg = Logging.getMessage("Tessellator.SurfaceGeometryVBONotInGpuCache", tile, gpuCache.getUsedCapacity());
				Logging.warning(msg);
			}
		}

		// Specify the data for the program's vertexTexCoord attribute, if one exists. This attribute is enabled in
		// beginRendering.
		location = program.getAttribLocation("vertexTexCoord");
		if (location >= 0) {
			int[] sharedVboIds = (int[]) gpuCache.get(geom.sharedGeom.vboCacheKey);
			if (sharedVboIds != null) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sharedVboIds[0]);
				WorldWindowImpl.glCheckError("glBindBuffer");
				GLES20.glVertexAttribPointer(location, 2, GLES20.GL_FLOAT, false, 0, 0);
				WorldWindowImpl.glCheckError("glVertexAttribPointer");
			} else {
				String msg = Logging.getMessage("Tessellator.SharedGeometryVBONotInGpuCache", tile, gpuCache.getUsedCapacity());
				Logging.warning(msg);
			}
		}

		// Multiply the View's modelview-projection matrix by the tile's transform matrix to correctly transform tile
		// points into eye coordinates. This achieves the resolution we need on Gpus with limited floating point
		// precision keeping both the modelview-projection matrix and the point coordinates the Gpu uses as small as
		// possible when the eye point is near the tile.
		this.mvpMatrix.multiplyAndSet(dc.getView().getModelviewProjectionMatrix(), geom.transformMatrix);
		program.loadUniformMatrix("mvpMatrix", this.mvpMatrix);
	}

	protected void endRendering(DrawContext dc, TerrainTile tile) {
		// Intentionally left blank. All GL state is restored in endRendering(DrawContext).
	}

	protected void render(DrawContext dc, TerrainTile tile) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return; // Message logged in beginRendering(DrawContext).

		TerrainGeometry geom = tile.getGeometry(this.getTerrainGeometryCache());
		if (geom == null) return; // Message logged in beginRendering(DrawContext, TerrainTile).

		GpuResourceCache gpuCache = dc.getGpuResourceCache();
		int[] sharedVboIds = (int[]) gpuCache.get(geom.sharedGeom.vboCacheKey);
		if (sharedVboIds == null) {
			Logging.warning(Logging.getMessage("Tessellator.SharedGeometryVBONotInGpuCache", tile, gpuCache.getUsedCapacity()));
			return;
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sharedVboIds[1]);
		WorldWindowImpl.glCheckError("glBindBuffer");
		GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, geom.sharedGeom.indices.remaining(), GLES20.GL_UNSIGNED_SHORT, 0);
		WorldWindowImpl.glCheckError("glDrawElements");
	}

	protected void renderWireframe(DrawContext dc, TerrainTile tile) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return; // Message logged in beginRendering(DrawContext).

		TerrainGeometry geom = tile.getGeometry(this.getTerrainGeometryCache());
		if (geom == null) return; // Message logged in beginRendering(DrawContext, TerrainTile).

		GpuResourceCache gpuCache = dc.getGpuResourceCache();
		int[] sharedVboIds = (int[]) gpuCache.get(geom.sharedGeom.vboCacheKey);
		if (sharedVboIds == null) {
			Logging.warning(Logging.getMessage("Tessellator.SharedGeometryVBONotInGpuCache", tile, gpuCache.getUsedCapacity()));
			return;
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sharedVboIds[2]);
		WorldWindowImpl.glCheckError("glBindBuffer");
		GLES20.glDrawElements(GLES20.GL_LINES, geom.sharedGeom.wireframeIndices.remaining(), GLES20.GL_UNSIGNED_SHORT, 0);
		WorldWindowImpl.glCheckError("glDrawElements");
	}

	protected void renderOutline(DrawContext dc, TerrainTile tile) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return; // Message logged in beginRendering(DrawContext).

		TerrainGeometry geom = tile.getGeometry(this.getTerrainGeometryCache());
		if (geom == null) return; // Message logged in beginRendering(DrawContext, TerrainTile).

		GpuResourceCache gpuCache = dc.getGpuResourceCache();
		int[] sharedVboIds = (int[]) gpuCache.get(geom.sharedGeom.vboCacheKey);
		if (sharedVboIds == null) {
			Logging.warning(Logging.getMessage("Tessellator.SharedGeometryVBONotInGpuCache", tile, gpuCache.getUsedCapacity()));
			return;
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sharedVboIds[3]);
		WorldWindowImpl.glCheckError("glBindBuffer");
		GLES20.glDrawElements(GLES20.GL_LINE_STRIP, geom.sharedGeom.outlineIndices.remaining(), GLES20.GL_UNSIGNED_SHORT, 0);
		WorldWindowImpl.glCheckError("glDrawElements");
	}

	protected void renderBoundingVolume(DrawContext dc, TerrainTile tile)
	{
		Extent extent = tile.getExtent();
		if (extent == null)
			return;

		if (extent instanceof Renderable)
			((Renderable) extent).render(dc);
	}

	protected void renderTileID(DrawContext dc, TerrainTile tile)
	{
		if(textRenderer==null) {
            Paint paint = new Paint();
            paint.setColor(android.graphics.Color.RED);
			textRenderer = new TextRenderer(dc, paint);
		}

		textRenderer.beginDrawing();

		String tileLabel = Integer.toString(tile.getLevelNumber());
		double[] elevs = dc.getGlobe().getMinAndMaxElevations(tile.getSector());
		if (elevs != null)
			tileLabel += ", " + (int) elevs[0] + "/" + (int) elevs[1];

		LatLon ll = tile.getSector().getCentroid();
		Vec4 pt = new Vec4();
		dc.getView().project(dc.getGlobe().computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
				dc.getGlobe().getElevation(ll.getLatitude(), ll.getLongitude())), pt);
		textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);

        textRenderer.endDrawing();
	}

	protected void loadGeometryVbos(DrawContext dc, TerrainGeometry geom) {
		// Load the terrain geometry into the GpuResourceCache.
		GpuResourceCache cache = dc.getGpuResourceCache();
		int[] vboIds = (int[]) cache.get(geom.vboCacheKey);
		if (vboIds != null && !geom.mustRegnerateVbos) return;

		if (vboIds == null) {
			vboIds = new int[1];
			GLES20.glGenBuffers(1, vboIds, 0);
			WorldWindowImpl.glCheckError("glGenBuffers");
		}

		try {
			int sizeInBytes = 4 * geom.points.remaining();
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sizeInBytes, geom.points, GLES20.GL_STREAM_DRAW);
			WorldWindowImpl.glCheckError("glBufferData");

			// Don't overwrite these VBOs if they're already in the cache. Doing so would cause the cache to delete
			// the existing VBO objects. Since we're reusing the same VBO ids, this would delete the VBO ids we're
			// using.
			if (!cache.contains(geom.vboCacheKey)) cache.put(geom.vboCacheKey, vboIds, GpuResourceCache.VBO_BUFFERS, sizeInBytes);

			geom.mustRegnerateVbos = false;
		} finally {
			// Restore the array buffer binding to 0.
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			WorldWindowImpl.glCheckError("glBindBuffer");
		}
	}

	protected void loadSharedGeometryVBOs(DrawContext dc, TerrainSharedGeometry geom) {
		GpuResourceCache cache = dc.getGpuResourceCache();
		int[] vboIds = (int[]) cache.get(geom.vboCacheKey);
		if (vboIds != null) return;

		vboIds = new int[4];
		GLES20.glGenBuffers(4, vboIds, 0);
		WorldWindowImpl.glCheckError("glGenBuffers");

		try {
			long totalSizeInBytes = 0;
			int sizeInBytes = 4 * geom.texCoords.remaining();
			totalSizeInBytes += sizeInBytes;
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sizeInBytes, geom.texCoords, GLES20.GL_STREAM_DRAW);
			WorldWindowImpl.glCheckError("glBufferData");

			sizeInBytes = 2 * geom.indices.remaining();
			totalSizeInBytes += sizeInBytes;
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, sizeInBytes, geom.indices, GLES20.GL_STREAM_DRAW);
			WorldWindowImpl.glCheckError("glBufferData");

			sizeInBytes = 2 * geom.wireframeIndices.remaining();
			totalSizeInBytes += sizeInBytes;
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[2]);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, sizeInBytes, geom.wireframeIndices, GLES20.GL_STREAM_DRAW);
			WorldWindowImpl.glCheckError("glBufferData");

			sizeInBytes = 2 * geom.outlineIndices.remaining();
			totalSizeInBytes += sizeInBytes;
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboIds[3]);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, sizeInBytes, geom.outlineIndices, GLES20.GL_STREAM_DRAW);
			WorldWindowImpl.glCheckError("glBufferData");

			cache.put(geom.vboCacheKey, vboIds, GpuResourceCache.VBO_BUFFERS, totalSizeInBytes);
		} finally {
			// Restore the array and element array buffer bindings to 0.
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			WorldWindowImpl.glCheckError("glBindBuffer");
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
			WorldWindowImpl.glCheckError("glBindBuffer");
		}
	}

	protected void pick(DrawContext dc, SectorGeometryList sgList, Point pickPoint) {
		// Load the program used to draw the tiles in unique colors.
		GpuProgram program = this.getGpuPickProgram(dc.getGpuResourceCache());
		if (program == null) return; // Message already logged in getGpuPickProgram.

		program.bind();
		dc.setCurrentProgram(program);
		program.loadUniform1b("uUseVertexColors", true);
		program.loadUniform1f("uOpacity", 1f);
        program.loadUniformColor("uColor", Color.white());
		try {
			SectorGeometry sg = this.getPickedGeometry(dc, sgList, pickPoint);
			if (sg != null) sg.pick(dc, pickPoint);
		} finally {
			GLES20.glUseProgram(0);
			WorldWindowImpl.glCheckError("glUseProgram");
			dc.setCurrentProgram(null);
		}
	}

	protected GpuProgram getGpuPickProgram(GpuResourceCache cache) {
		if (this.pickProgramCreationFailed) return null;

		GpuProgram program = cache.getProgram(this.pickProgramKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(PICK_VERTEX_SHADER_PATH, PICK_FRAGMENT_SHADER_PATH);
				program = new GpuProgram(source);
				cache.put(this.pickProgramKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", PICK_VERTEX_SHADER_PATH, PICK_FRAGMENT_SHADER_PATH);
				Logging.error(msg);
				this.pickProgramCreationFailed = true;
			}
		}

		return program;
	}

	protected SectorGeometry getPickedGeometry(DrawContext dc, SectorGeometryList sgList, Point pickPoint) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return null;

		int location = program.getAttribLocation("vertexColor");
		if (location < 0) return null;

		if (sgList.isEmpty()) return null;

		// Assign the minimum color code to the color used to draw the first SectorGeometry.
		int color = dc.getUniquePickColor();
		int minColorCode = color;
		int maxColorCode;

		// Draw the terrain tiles in unique colors. The unique colors for each tile are sequential, such that the the
		// color used to draw the i'th SectorGeometry is equal to minColorCode + i.
		sgList.beginRendering(dc);
		try {
			for (int i = 0; i < sgList.size(); i++) {
				// Get a unique pick color for every SectorGeometry except the first. The color for the first tile is
				// allocated before this loop. We must perform this step even if a tile is outside the pick frustum to
				// ensure that the pick colors can be used to compute an index into the SectorGeometryList.
				if (i > 0) color = dc.getUniquePickColor();

				SectorGeometry sg = sgList.get(i);

				if(!dc.getPickFrustums().intersectsAny(sg.getExtent()))
					continue;

				sg.beginRendering(dc);
				try {
					// Convert the pick color from a packed 32-bit RGB color int to a RGB color with separate components
					// in the range [0.0, 1.0].
					this.pickColor.set(color, false);
					// Specify the generic vertex attribute value for "vertexColor" to be used for all vertices. Since
					// the "vertexColor" attrib array is not enabled, this constant value is used instead. OpenGL
					// expands this 3-component RGB color to a 4-component RGBA color, where the alpha component is 1.0.
					GLES20.glVertexAttrib3f(location, (float) this.pickColor.r, (float) this.pickColor.g, (float) this.pickColor.b);
					WorldWindowImpl.glCheckError("glVertexAttrib3f");
					sg.render(dc);
				} finally {
					sg.endRendering(dc);
				}
			}
		} finally {
			sgList.endRendering(dc);
		}

		// Assign the maximum color code to the color used to draw the last SectorGeometry. If only one SectorGeometry
		// is in the list, this is equal to minColorCode.
		maxColorCode = color;

		// Determine which terrain tile was picked by reading the color at the pick point. Nothing is picked if the
		// color code is outside the range of unique pick colors used for the tiles.
		int colorCode = dc.getPickColor(pickPoint);
		if (colorCode < minColorCode || colorCode > maxColorCode) return null;

		// If the picked color corresponds to one of the SectorGeometry tiles, we compute the index by subtracting the
		// first color code from the picked color code. Since the color codes are sequential, this results in an index
		// into the list we traversed above.
		return sgList.get(colorCode - minColorCode);
	}

	protected void pick(DrawContext dc, TerrainTile tile, Point pickPoint) {
		// Create vertex geometry for the terrain tile that displays each triangle in a unique pick color. Colors are
		// assigned sequentially for each triangle. The method buildPickGeometry returns null if the terrain tile's
		// geometry is not in the cache. This should never happen, but we check anyway.
		TerrainPickGeometry geom = this.buildPickGeometry(dc, tile);
		if (geom == null) return;

		// Draw the tile's triangles in unique pick colors. Each triangle is drawn in a 24-bit RGB color. The colors
		// are treated as a 24-bit integer that increases sequentially with each triangle. The first triangle's color
		// is geom.minColorCode, the last triangle's color is geom.maxColorCode, and the i'th triangle's color is
		// geom.minColorCode + i.
		this.drawTrianglesInPickColors(dc, geom);

		// Determine which triangle in the terrain tile was picked, and create a PickedObject containing the exact
		// geographic position of the picked point. The PickedObject returned by resolvePick is null if
		// drawTrianglesInPickColors draws incorrect colors, if a ray through the pick point cannot be computed because
		// either of the modelview or projection matrices are non-singular, or if we cannot compute an intersection
		// between the ray and the picked triangle. This should never happen if this method is called to resolve the
		// picked triangle after determining this tile is picked, but we check anyway. We don't log a message to ensure
		// this method can be reused in a context where the picked tile is not known before this method is called.
		PickedObject po = this.resolvePick(dc, geom, pickPoint);
		if (po != null) dc.addPickedObject(po);
	}

	protected TerrainPickGeometry buildPickGeometry(DrawContext dc, TerrainTile tile) {
		MemoryCache cache = this.getTerrainGeometryCache();

		TerrainGeometry geom = tile.getGeometry(cache);
		if (geom == null) {
			Logging.warning(Logging.getMessage("Tessellator.SurfaceGeometryNotInCache", tile, cache.getUsedCapacity()));
			return null;
		}

		int tileWidth = tile.getWidth();
		int tileHeight = tile.getHeight();
		Object key = Pair.create(tileWidth, tileHeight);

		TerrainPickGeometry pickGeom = pickGeometry.get(key);
		if (pickGeom == null) {
			pickGeom = new TerrainPickGeometry();
			pickGeometry.put(key, pickGeom);
		}

		this.buildPickGeometry(dc, tileWidth, tileHeight, geom, pickGeom);

		return pickGeom;
	}

	protected void buildPickGeometry(DrawContext dc, int tileWidth, int tileHeight, TerrainGeometry geom, TerrainPickGeometry pickGeom) {
		// The tile width and height indicates the number of cell rows and columns in the tile. We add one row and
		// column of vertices because there is a row/column of vertices in between each cell. We add two rows and
		// columns of vertices to provide an outer row/column for the tile skirt.
		int numLat = tileHeight + 3;
		int numLon = tileWidth + 3;

		// The pick geometry's points are a collection of independent triangles. There are two triangles (6 points) for
		// each tile cell, and the number of cells in each dimension is one less than the number of vertices.
		int numPoints = 6 * (numLat - 1) * (numLon - 1);

		// Allocate native byte buffers to hold the tile's pick points and pick colors. Re-use the existing buffers
		// whenever possible. Create a new buffer if one has not been set or if the tile density has changed. We clear
		// the buffer if it is non-null and has enough capacity to ensure that the previous limit does not interfere
		// with what the new limit should be after filling the buffer.
		if (pickGeom.points == null || pickGeom.points.capacity() < 3 * numPoints) pickGeom.points = BufferUtil.newFloatBuffer(3 * numPoints);
		if (pickGeom.colors == null || pickGeom.colors.capacity() < 3 * numPoints) pickGeom.colors = BufferUtil.newByteBuffer(3 * numPoints);
		pickGeom.points.clear();
		pickGeom.colors.clear();

		// Assign the minimum color code to the color used to draw the triangle.
		int color = dc.getUniquePickColor();
		pickGeom.minColorCode = color;

		// Allocate a buffer to hold the XYZ coordinates of the four vertices defining each tile cell.
		float[] corners = new float[12];
		// Allocate a buffer to hold the RGB values of the unique pick colors.
		byte[] colors = new byte[9];

		for (int j = 0; j < numLat - 1; j++) {
			for (int i = 0; i < numLon - 1; i++) {
				// Get the four vertices defining this cell. Store the lower-left coordinate at index 0, the lower-right
				// coordinate at index 3, the upper-left coordinate at index 6, and the upper-right coordinate at index
				// 9.
				geom.points.position(3 * (i + j * numLon));
				geom.points.get(corners, 0, 6);
				geom.points.position(3 * (i + (j + 1) * numLon));
				geom.points.get(corners, 6, 6);

				// Add the vertices and colors for the two triangles in each tile cell. The vertices for both triangles
				// are arranged in counter-clockwise order. Each triangle is composed of three vertices from the cell's
				// four corner vertices, and are composed to exactly match the triangles created by the triangle-strip
				// tessellation of buildIndices.

				// First triangle. The first triangle is composed of the upper-left, lower-left, and upper-right
				// vertices from this cell. This triangulation is consistent with the triangle-strip defined by
				// buildIndices.
				pickGeom.points.put(corners, 6, 3); // Upper-left vertex.
				pickGeom.points.put(corners, 0, 3); // Lower-left vertex.
				pickGeom.points.put(corners, 9, 3); // Upper-right vertex.

				if (i != 0 || j != 0) // The first triangle's color is allocated before this loop.
					color = dc.getUniquePickColor();
				colors[0] = colors[3] = colors[6] = (byte) Color.getColorIntRed(color);
				colors[1] = colors[4] = colors[7] = (byte) Color.getColorIntGreen(color);
				colors[2] = colors[5] = colors[8] = (byte) Color.getColorIntBlue(color);
				pickGeom.colors.put(colors); // Colors for all three vertices of the fist triangle.

				// Second triangle. The second triangle is composed of the upper-right, lower-left, and lower-right
				// vertices from this cell. This triangulation is consistent with the triangle-strip defined by
				// buildIndices.
				// Upper-right vertex.
				pickGeom.points.put(corners, 9, 3); // Upper-right vertex.
				pickGeom.points.put(corners, 0, 3); // Lower-left vertex.
				pickGeom.points.put(corners, 3, 3); // Lower-right vertex.

				color = dc.getUniquePickColor();
				colors[0] = colors[3] = colors[6] = (byte) Color.getColorIntRed(color);
				colors[1] = colors[4] = colors[7] = (byte) Color.getColorIntGreen(color);
				colors[2] = colors[5] = colors[8] = (byte) Color.getColorIntBlue(color);
				pickGeom.colors.put(colors); // Colors for all three vertices of the fist triangle.
			}
		}

		// Restore the position of the tile point buffer.
		geom.points.rewind();

		// Set the pick geometry's reference center and transform matrix to be equivalent to the terrain geometry's
		// properties of the same names. Since we're using the same local coordinates for pick geometry points, we need
		// to use the same reference center and transform matrix.
		pickGeom.referenceCenter.set(geom.referenceCenter);
		pickGeom.transformMatrix.set(geom.transformMatrix);
		// Set the limit to the current position then set the position to zero. We flip the buffer because its capacity
		// may be greater than the space needed, and the GL commands that ready this buffer rely on the limit to
		// determine how many buffer elements to read.
		pickGeom.points.flip();
		pickGeom.colors.flip();
		// Assign the vertex count to the number of triangle vertices computed above. Assign the maximum color code to
		// the color used to draw the last triangle.
		pickGeom.vertexCount = numPoints;
		pickGeom.maxColorCode = color;
	}

	protected void drawTrianglesInPickColors(DrawContext dc, TerrainPickGeometry geom) {
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) {
			Logging.warning(Logging.getMessage("generic.NoCurrentProgram"));
			return;
		}

		// Enable and specify the data for the program's vertexPoint attribute, if one exists.
		int pointLocation = program.getAttribLocation("vertexPoint");
		if (pointLocation >= 0) {
			GLES20.glEnableVertexAttribArray(pointLocation);
			WorldWindowImpl.glCheckError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, geom.points);
			WorldWindowImpl.glCheckError("glVertexAttribPointer");
		}

		// Enable and specify the data for the program's vertexPoint attribute, if one exists. We specify a 3-element
		// tuple of unsigned bytes per which are normalized to the range [0, 1] and expanded to a 4-element tuple. The
		// resultant tuple stored in the GPU is equivalent to (r/255, g/255, b/255, 255).
		int colorLocation = program.getAttribLocation("vertexColor");
		if (colorLocation >= 0) {
			GLES20.glEnableVertexAttribArray(colorLocation);
			WorldWindowImpl.glCheckError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(colorLocation, 3, GLES20.GL_UNSIGNED_BYTE, true, 0, geom.colors);
			WorldWindowImpl.glCheckError("glVertexAttribPointer");
		}

		// Multiply the View's modelview-projection matrix by the tile's transform matrix to correctly transform tile
		// points into eye coordinates. This achieves the resolution we need on Gpus with limited floating point
		// precision keeping both the modelview-projection matrix and the point coordinates the Gpu uses as small as
		// possible when the eye point is near the tile.
		this.mvpMatrix.multiplyAndSet(dc.getView().getModelviewProjectionMatrix(), geom.transformMatrix);
		program.loadUniformMatrix("mvpMatrix", this.mvpMatrix);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, geom.vertexCount);
		WorldWindowImpl.glCheckError("glDrawArrays");

		if (pointLocation >= 0) GLES20.glDisableVertexAttribArray(pointLocation);
		WorldWindowImpl.glCheckError("glDisableVertexAttribArray");
		if (colorLocation >= 0) GLES20.glDisableVertexAttribArray(colorLocation);
		WorldWindowImpl.glCheckError("glDisableVertexAttribArray");
	}

	protected PickedObject resolvePick(DrawContext dc, TerrainPickGeometry geom, Point pickPoint) {
		// Determine which triangle was picked by reading the color at the pick point. Nothing is picked if the
		// color code is outside the range of unique pick colors used for the tiles.
		int colorCode = dc.getPickColor(pickPoint);
		if (colorCode < geom.minColorCode && colorCode <= geom.maxColorCode) return null;

		// Compute a ray that starts at the model coordinate eye point and passes through the pick point.
		// View.computeRayFromScreenPoint returns false if the ray cannot be computed, which indicates that the View's
		// modelview or projection matrices are singular. This should never happen, but we check anyway.
		if (!dc.getView().computeRayFromScreenPoint(pickPoint, this.pickRay)) return null;

		// Compute the picked point on the triangle. computePickedPoint returns false if the line does not intersect the
		// triangle. This should never happen, but we check anyway. We compute the triangle index by subtracting the
		// first color code from the picked color code. Since the color codes are sequential, this results in an index
		// into the list of triangles we rendered.
		if (!this.computePickPoint(this.pickRay, geom, colorCode - geom.minColorCode, this.pickedTriPoint)) return null;

		// Compute the position that corresponds to the model coordinate point.
		dc.getGlobe().computePositionFromPoint(this.pickedTriPoint, this.pickedTriPos);
		// Create a new position to use both the picked object and the object's position. Draw this position's elevation
		// from the elevation model, not the geode.
		Position pp = this.pickedTriPos.copy();
		pp.elevation = dc.getGlobe().getElevation(pp) * dc.getVerticalExaggeration();

		Vec4 test = new Vec4();
		dc.getView().project(this.pickedTriPoint, test);

		// Create a new PickedObject representing the picked terrain position.
		return new PickedObject(pickPoint, colorCode, pp, pp, true);
	}

	protected boolean computePickPoint(Line line, TerrainPickGeometry geom, int index, Vec4 result) {
		// Allocate a buffer to hold the XYZ coordinates of the three vertices defining the triangle.
		float[] points = this.pointBuffer; // Holds up to 12 coordinates.
		Vec4 center = geom.referenceCenter;

		// Get the coordinates for the three vertices defining this triangle. We multiply the index by 9 to convert
		// between triangle count and primitive count. The pick geometry's triangles are assumed to have a
		// counter-clockwise ordering. We add the pick geometry's reference center to each vertex in order to transform
		// it from local coordinates to model coordinates.
		geom.points.position(9 * index);
		geom.points.get(points, 0, 9);
		geom.points.rewind();

		return this.computeTriangleIntersection(line, points[0] + center.x, points[1] + center.y, points[2] + center.z, points[3] + center.x, points[4] + center.y, points[5]
				+ center.z, points[6] + center.x, points[7] + center.y, points[8] + center.z, result);
	}

	protected void computeSurfacePoint(TerrainTile tile, TerrainGeometry geom, Angle latitude, Angle longitude, Vec4 result) {
		Sector tileSector = tile.getSector();
		int tileWidth = tile.getWidth();
		int tileHeight = tile.getHeight();

		double lat = latitude.degrees;
		double lon = longitude.degrees;
		double minLat = tileSector.minLatitude.degrees;
		double maxLat = tileSector.maxLatitude.degrees;
		double minLon = tileSector.minLongitude.degrees;
		double maxLon = tileSector.maxLongitude.degrees;

		// Compute the location's horizontal (s) and vertical (t) parameterized coordinates within the tile's 2D grid of
		// points as a floating-point value in the range [0, tileWidth] and [0, tileHeight]. These coordinates indicate
		// which cell contains the location, as well as the location's placement within the cell. Note that this method
		// assumes that the caller has tested whether the location is contained within the tile's sector. We do this to
		// avoid redundant tests.
		double s = (lon - minLon) / (maxLon - minLon) * tileWidth;
		double t = (lat - minLat) / (maxLat - minLat) * tileHeight;

		// Get the coordinates for the four vertices defining the cell this point is in. Tile vertices start in the
		// lower left corner and proceed in row major fashion across the tile. The tile contains three more vertices
		// per row or column than the tile width or height: one to convert between cell count and vertex count, and two
		// for the skirt surrounding the tile. We convert from parameterized coordinates to the vertex position by
		// adding an offset of 1 to the s and t indices for the extra skirt vertices, multiplying the t index by the row
		// stride, and adding the s index. We then convert from vertex position to coordinate position by multiplying
		// the vertex position by 3. Vertices in the points array are organized in the following order: lower-left,
		// lower-right, upper-left, upper-right. The cell's diagonal starts at the lower-left vertex and ends at the
		// upper-right vertex.
		int si = (s < tileWidth ? (int) s : tileWidth - 1) + 1;
		int ti = (t < tileHeight ? (int) t : tileHeight - 1) + 1;
		int rowStride = tileWidth + 3;
		float[] points = this.pointBuffer; // Holds up to 12 coordinates.
		geom.points.position(3 * (si + ti * rowStride)); // lower-left and lower-right vertices.
		geom.points.get(points, 0, 6);
		geom.points.position(3 * (si + (ti + 1) * rowStride)); // upper-left and upper-right vertices.
		geom.points.get(points, 6, 6);
		geom.points.rewind();

		// Compute the location's corresponding point on the cell in tile local coordinates, given the fractional
		// portion of the parameterized s and t coordinates. These values indicates the location's relative placement
		// within the cell. The cell's vertices are defined in the following order: lower-left, lower-right, upper-left,
		// upper-right. The cell's diagonal starts at the lower-left vertex and ends at the upper-right vertex.
		double sf = (s < tileWidth ? s - (int) s : 1);
		double tf = (t < tileHeight ? t - (int) t : 1);
		this.computePointInCell(sf, tf,
				points[0], points[1], points[2],
				points[3], points[4], points[5],
				points[6], points[7], points[8],
				points[9], points[10], points[11],
				result);

		// Add the tile geometry's reference center to the result in order to transform it from tile local coordinates
		// to model coordinates.
		result.add3AndSet(geom.referenceCenter);
	}

	/**
	 * Computes the model coordinate intersection of a specified line with a triangle specified by individual
	 * coordinates.
	 *
	 * @param line
	 *            the line to test.
	 * @param vax
	 *            the X coordinate of the first vertex of the triangle.
	 * @param vay
	 *            the Y coordinate of the first vertex of the triangle.
	 * @param vaz
	 *            the Z coordinate of the first vertex of the triangle.
	 * @param vbx
	 *            the X coordinate of the second vertex of the triangle.
	 * @param vby
	 *            the Y coordinate of the second vertex of the triangle.
	 * @param vbz
	 *            the Z coordinate of the second vertex of the triangle.
	 * @param vcx
	 *            the X coordinate of the third vertex of the triangle.
	 * @param vcy
	 *            the Y coordinate of the third vertex of the triangle.
	 * @param vcz
	 *            the Z coordinate of the third vertex of the triangle.
	 * @param result
	 *            contains the point of intersection after this method returns, if the line intersects this
	 *            triangle.
	 * @return <code>true</code> if the line intersects this triangle, and <code>false</code> otherwise.
	 */
	protected boolean computeTriangleIntersection(Line line, double vax, double vay, double vaz, double vbx, double vby, double vbz, double vcx, double vcy, double vcz, Vec4 result) {
		final double EPSILON = (double) 0.00001f;

		Vec4 origin = line.getOrigin();
		Vec4 dir = line.getDirection();

		// find vectors for two edges sharing Point a: vb - va and vc - va
		double edge1x = vbx - vax;
		double edge1y = vby - vay;
		double edge1z = vbz - vaz;

		double edge2x = vcx - vax;
		double edge2y = vcy - vay;
		double edge2z = vcz - vaz;

		// Compute cross product of edge1 and edge2.
		double nx = (edge1y * edge2z) - (edge1z * edge2y);
		double ny = (edge1z * edge2x) - (edge1x * edge2z);
		double nz = (edge1x * edge2y) - (edge1y * edge2x);

		// Distance from vertA to ray origin: origin - va
		double tvecx = origin.x - vax;
		double tvecy = origin.y - vay;
		double tvecz = origin.z - vaz;

		// Compute dot product of N and ray direction.
		double b = nx * dir.x + ny * dir.y + nz * dir.z;
		if (b > -EPSILON && b < EPSILON) // ray is parallel to triangle plane
			return false;

		double t = -(nx * tvecx + ny * tvecy + nz * tvecz) / b;
		line.getPointAt(t, result);

		return true;
	}

	/**
	 * Computes the point in tile local coordinates of a location within a tile's cell specified by individual
	 * coordinates.
	 *
	 * @param s
	 *            a parameterized horizontal coordinate within the tile's 2D grid of points as a floating-point value
	 *            in the range [0, tileWidth].
	 * @param t
	 *            a parameterized vertical coordinate within the tile's 2D grid of points as a floating-point value
	 *            in the range [0, tileHeight].
	 * @param llx
	 *            the X coordinate of the cell's lower left corner.
	 * @param lly
	 *            the Y coordinate of the cell's lower left corner.
	 * @param llz
	 *            the Z coordinate of the cell's lower left corner.
	 * @param lrx
	 *            the X coordinate of the cell's lower right corner.
	 * @param lry
	 *            the Y coordinate of the cell's lower right corner.
	 * @param lrz
	 *            the Z coordinate of the cell's lower right corner.
	 * @param ulx
	 *            the X coordinate of the cell's upper left corner.
	 * @param uly
	 *            the Y coordinate of the cell's upper left corner.
	 * @param ulz
	 *            the Z coordinate of the cell's upper left corner.
	 * @param urx
	 *            the X coordinate of the cell's upper right corner.
	 * @param ury
	 *            the Y coordinate of the cell's upper right corner.
	 * @param urz
	 *            the Z coordinate of the cell's upper right corner.
	 * @param result
	 *            contains the tile local coordinates of the point in the cell after this method returns.
	 */
	protected void computePointInCell(double s, double t, double llx, double lly, double llz, double lrx, double lry, double lrz, double ulx, double uly, double ulz, double urx,
									  double ury, double urz, Vec4 result) {
		if (s < t) // The point is in the lower-right triangle.
		{
			double oneMinusS = 1 - s;
			result.x = lrx + oneMinusS * (llx - lrx) + t * (urx - lrx);
			result.y = lry + oneMinusS * (lly - lry) + t * (ury - lry);
			result.z = lrz + oneMinusS * (llz - lrz) + t * (urz - lrz);
		} else // The point is in the upper-left triangle, or on the diagonal between the two triangles.
		{
			double oneMinusT = 1 - t;
			result.x = ulx + s * (urx - ulx) + oneMinusT * (llx - ulx);
			result.y = uly + s * (ury - uly) + oneMinusT * (lly - uly);
			result.z = ulz + s * (urz - ulz) + oneMinusT * (llz - ulz);
		}
	}

	/**
	 * Computes the point in tile local coordinates of a location within a tile's cell specified by individual
	 * coordinates.
	 *
	 * @param s
	 *            a parameterized horizontal coordinate within the tile's 2D grid of points as a floating-point value
	 *            in the range [0, 1].
	 * @param t
	 *            a parameterized vertical coordinate within the tile's 2D grid of points as a floating-point value
	 *            in the range [0, 1].
	 * @param llx
	 *            the X coordinate of the cell's lower left corner.
	 * @param lly
	 *            the Y coordinate of the cell's lower left corner.
	 * @param llz
	 *            the Z coordinate of the cell's lower left corner.
	 * @param lrx
	 *            the X coordinate of the cell's lower right corner.
	 * @param lry
	 *            the Y coordinate of the cell's lower right corner.
	 * @param lrz
	 *            the Z coordinate of the cell's lower right corner.
	 * @param ulx
	 *            the X coordinate of the cell's upper left corner.
	 * @param uly
	 *            the Y coordinate of the cell's upper left corner.
	 * @param ulz
	 *            the Z coordinate of the cell's upper left corner.
	 * @param urx
	 *            the X coordinate of the cell's upper right corner.
	 * @param ury
	 *            the Y coordinate of the cell's upper right corner.
	 * @param urz
	 *            the Z coordinate of the cell's upper right corner.
	 * @param result
	 *            contains the tile local coordinates of the point in the cell after this method returns.
	 */
	protected void computePointInCellBarycentric(double s, double t, double llx, double lly, double llz, double lrx, double lry, double lrz, double ulx, double uly, double ulz, double urx,
									  double ury, double urz, Vec4 result) {
		Vec4 barycentric = new Vec4();
		if (s < t) // The point is in the lower-right triangle.
		{
			barycentric2D(LOWER_RIGHT, UPPER_RIGHT, LOWER_LEFT, new Vec4(s, t), barycentric);
			result.x = lrx*barycentric.x + urx*barycentric.y + llx*barycentric.z;
			result.y = lry*barycentric.x + ury*barycentric.y + lly*barycentric.z;
			result.z = lrz*barycentric.x + urz*barycentric.y + llz*barycentric.z;
		} else // The point is in the upper-left triangle, or on the diagonal between the two triangles.
		{
			barycentric2D(UPPER_LEFT, LOWER_LEFT, UPPER_RIGHT, new Vec4(s, t), barycentric);
			result.x = ulx*barycentric.x + llx*barycentric.y + urx*barycentric.z;
			result.y = uly*barycentric.x + lly*barycentric.y + ury*barycentric.z;
			result.z = ulz*barycentric.x + llz*barycentric.y + urz*barycentric.z;
		}
	}

	private final Vec4 LOWER_LEFT = new Vec4(0, 0);
	private final Vec4 LOWER_RIGHT = new Vec4(1, 0);
	private final Vec4 UPPER_RIGHT = new Vec4(1, 1);
	private final Vec4 UPPER_LEFT = new Vec4(0, 1);

	/**
	 * Get barycentric coordinate from 2D coordinate in triangle a,b,c
	 * @param a	First point in triangle
	 * @param b	Second point in Triangle
	 * @param c	Third point in triangle
	 * @param p	point in triangle
	 * @param result	barycentric coordinate (α, β, γ) for p
	 */
	public void barycentric2D(Vec4 a, Vec4 b, Vec4 c, Vec4 p, Vec4 result) {
		double A = (b.x-a.x)*(c.y-a.y)-(c.x-a.x)*(b.y-a.y);
		double Ab = (a.x-c.x)*(p.y-c.y)-(p.x-c.x)*(a.y-c.y);
		double Ac = (b.x-a.x)*(p.y-a.y)-(p.x-a.x)*(b.y-a.y);
		double β = Ab / A;
		double γ = Ac / A;
		double α = 1 - β - γ;
		result.set(α, β, γ);
	}
}
