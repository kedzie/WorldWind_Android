/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package it.trilogis.android.ww;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.ElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.WWIO;
import it.trilogis.android.ww.dialogs.AddWMSDialog;
import it.trilogis.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;
import it.trilogis.android.ww.dialogs.TocFragment;
import it.trilogis.android.ww.view.SettingsActivity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static it.trilogis.android.ww.view.SettingsActivity.*;

/**
 * @author Nicola Dorigatti
 */
public class WorldWindowActivity extends Activity {
	static {
		System.setProperty("gov.nasa.worldwind.app.config.document", "config/sample.xml");
	}

	private static final String TAG = "WorldWindowActivity";

	public final static String DEFAULT_WMS_URL = "http://data.worldwind.arc.nasa.gov/wms";

	public static final int INITIAL_LATITUDE = 56;
	public static final int INITIAL_LONGITUDE = 9;

	protected WorldWindowGLTextureView wwd;
	private ElevationModel mElevationModel;
	private BasicView mView;
	private RenderableLayer layer;

	private TextView mFrameRateText;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private TocFragment mTocFragment;

	private boolean mUseElevation;

	private Handler statHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			StringBuffer sb = new StringBuffer();
			Map<String, PerformanceStatistic> stats = wwd.getPerFrameStatistics();
			if(stats!=null) {
				sb.append(stats.get(PerformanceStatistic.FRAME_RATE));
				for (PerformanceStatistic stat : stats.values()) {
					if (stat != null && !stat.getKey().equals(PerformanceStatistic.FRAME_RATE))
						sb.append('\n').append(stat);
				}
			}
			sb.append(String.format("\nVertical Exaggeration: %.2f", wwd.getSceneController().getVerticalExaggeration()));
			mFrameRateText.setText(sb.toString());
		}
	};

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.debug_preferences, false);
		PreferenceManager.setDefaultValues(this, R.xml.performance_preferences, false);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setDisplayShowHomeEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Setting the location of the file store on Android as cache directory. Doing this, when user has no space left
		// on the device, if he asks to the system to free Cache of apps, all the MB/GB of WorldWindApplication will be cleared!
		File fileDir = getCacheDir();// getFilesDir();
		Logging.info("Application cache directory: " + fileDir);
		if (null != fileDir && fileDir.exists() && fileDir.canWrite()) {
			File output = new File(fileDir, ".nomedia");
			if (!output.exists()) {
				try {
					output.createNewFile();
				} catch (IOException e) {
					Log.e(TAG, "IOException while creating .nomedia: " + e.getMessage());
				}
			}
		}
		System.setProperty("gov.nasa.worldwind.platform.user.store", fileDir.getAbsolutePath());

		WWIO.setContext(this);

		setContentView(R.layout.main);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close );

		wwd = (WorldWindowGLTextureView) findViewById(R.id.wwd);
		wwd.setFrameRate(60);
		Set<String> keys = new HashSet<String>();
		keys.add(PerformanceStatistic.ALL);
		wwd.setPerFrameStatisticsKeys(keys);

		mTocFragment = (TocFragment) getFragmentManager().findFragmentById(R.id.tocFragment);

		wwd.addPropertyChangeListener(PerformanceStatistic.FRAME_RATE, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				statHandler.obtainMessage(0, event.getNewValue()).sendToTarget();
			}
		});

		wwd.setModel((Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME));
		mElevationModel = wwd.getModel().getGlobe().getElevationModel();

		StatusBar statusBar = (StatusBar) findViewById(R.id.statusBar);
		statusBar.setEventSource(wwd);

		mFrameRateText = (TextView) findViewById(R.id.perframeStatText);
		mFrameRateText.bringToFront();

		mView = (BasicView) wwd.getView();

		mView.setLookAtPosition(Position.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE, 0));
		mView.setRange(100);

		layer = new RenderableLayer();
		layer.setName("Renderable");

		SurfaceQuad quad = new SurfaceQuad(LatLon.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE), 20, 20);
		BasicShapeAttributes quadAttrs = new BasicShapeAttributes();
		quadAttrs.setEnableInterior(true);
		quadAttrs.setEnableOutline(true);
		quadAttrs.setInteriorMaterial(new Material(Color.randomColor()));
		quadAttrs.setOutlineMaterial(new Material(Color.randomColor()));
		quadAttrs.setOutlineOpacity(.8f);
		quadAttrs.setInteriorOpacity(.5f);
		quadAttrs.setOutlineWidth(12f);
		quad.setAttributes(quadAttrs);
		layer.addRenderable(quad);

		SurfaceQuad quad2 = new SurfaceQuad(LatLon.fromDegrees(INITIAL_LATITUDE+.0004, INITIAL_LONGITUDE+.0004), 25, 30);
		BasicShapeAttributes quad2Attrs = new BasicShapeAttributes();
		quad2Attrs.setEnableInterior(true);
		quad2Attrs.setEnableOutline(true);
		quad2Attrs.setInteriorMaterial(new Material(Color.randomColor()));
		quad2Attrs.setOutlineMaterial(new Material(Color.randomColor()));
		quad2Attrs.setOutlineOpacity(.8f);
		quad2Attrs.setInteriorOpacity(.4f);
		quad2Attrs.setOutlineWidth(12f);
		quad2.setAttributes(quad2Attrs);
		layer.addRenderable(quad2);

		ArrayList<Position> positions = new ArrayList<Position>(5);
		final float radius = .001f;
		final int pathHeight = 5;
		positions.add(Position.fromDegrees(INITIAL_LATITUDE + radius, INITIAL_LONGITUDE + radius, pathHeight));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE + radius, INITIAL_LONGITUDE - radius, pathHeight));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE - radius, INITIAL_LONGITUDE - radius, pathHeight));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE - radius, INITIAL_LONGITUDE + radius, pathHeight));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE + radius, INITIAL_LONGITUDE + radius, pathHeight));

		Path field = new Path();
		field.setTerrainConformance(10f);
		field.setAltitudeMode(AVKey.CLAMP_TO_GROUND);
		field.setFollowTerrain(true);
		field.setPathType(AVKey.GREAT_CIRCLE);
		field.setPositions(positions);
		field.setExtrude(false);
		field.setDrawVerticals(false);
		field.setShowPositions(false);
		field.setShowPositionsScale(4d);
		field.setPositionColors(new Path.PositionColors() {
			@Override
			public Color getColor(Position position, int ordinal) {
				return Color.randomColor();
			}
		});

		BasicShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setEnableInterior(false);
		attrs.setEnableOutline(true);
		attrs.setOutlineWidth(6f);
		field.setAttributes(attrs);
		layer.addRenderable(field);

		layer.addRenderable(field);

		insertBeforeLayer(CompassLayer.class, layer);

		wwd.getSceneController().addPropertyChangeListener(AVKey.VERTICAL_EXAGGERATION, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				Logging.info(String.format("Vertical Exaggeration: %.2f->%.2f", event.getOldValue(), event.getNewValue()));
				statHandler.sendEmptyMessage(0);
				if(mUseElevation && event.getNewValue().equals(Double.valueOf(0))) {
					Logging.info("Using Zero elevation model");
					wwd.getModel().getGlobe().setElevationModel(new ZeroElevationModel());
				} else if(mUseElevation && event.getOldValue().equals(Double.valueOf(0))) {
					Logging.info("Using normal elevation model");
					wwd.getModel().getGlobe().setElevationModel(mElevationModel);
				}
			}
		});

		updatePreferences();
		mTocFragment.setWorldWindData(wwd);
	}

	public void insertBeforeLayer(Class<? extends Layer> target, Layer layer) {
		// Insert the layer into the layer list just before the compass.
		LayerList layers = wwd.getModel().getLayers();
		int targetPosition = layers.indexOf(searchSpecificLayer(target));
		layers.add(targetPosition, layer);
	}

	private <T extends Layer> T searchSpecificLayer(Class<T> classToSearch) {
		for (Layer lyr : wwd.getModel().getLayers()) {
			if (classToSearch.isInstance(lyr))
				return classToSearch.cast(lyr);
		}
		return null;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		super.onPause();
		wwd.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		wwd.onResume();
	}

	@Override
	protected void onDestroy() {
		wwd.onSurfaceDestroyed();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_add_wms:
				AddWMSDialog wmsLayersDialog = new AddWMSDialog();
				wmsLayersDialog.setOnAddWMSLayersListener(new OnAddWMSLayersListener() {

					public void onAddWMSLayers(List<Layer> layersToAdd) {
						for (Layer lyr : layersToAdd) {
							boolean added = WorldWindowActivity.this.wwd.getModel().getLayers().addIfAbsent(lyr);
							Toast.makeText(WorldWindowActivity.this, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!", Toast.LENGTH_LONG).show();
						}
					}
				});
				wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
				return true;
			case R.id.menu_zoom_in:
				AnimatorSet zoomIn = new AnimatorSet();
				zoomIn.setDuration(3000);
				zoomIn.setInterpolator(new AccelerateDecelerateInterpolator());
				zoomIn.play(mView.createRangeAnimator(wwd, 40d))
						.before(mView.createTiltAnimator(wwd, Angle.fromDegrees(45)));
				mView.animate(zoomIn);

				return true;
			case R.id.menu_zoom_out:
				AnimatorSet zoomOut = new AnimatorSet().setDuration(3000);
				zoomOut.setInterpolator(new AccelerateDecelerateInterpolator());
				zoomOut.play(mView.createRangeAnimator(wwd, 100d))
					.with(mView.createTiltAnimator(wwd, Angle.fromDegrees(0)));
				mView.animate(zoomOut);

				return true;
			case R.id.menu_preferences:
				startActivityForResult(new Intent(this, SettingsActivity.class), 0);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		updatePreferences();
	}

	/**
	 * Update parameters to reflect preferences
	 */
	private void updatePreferences() {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		WorldWindowImpl.DEBUG = mPrefs.getBoolean(PREF_DEBUG_LOGGING, true);
		mFrameRateText.setVisibility(mPrefs.getBoolean(PREF_STATS, false) ? android.view.View.VISIBLE : android.view.View.GONE);

		wwd.invokeInRenderingThread(new Runnable() {
			@Override
			public void run() {
				SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(WorldWindowActivity.this);
				for(Layer l : wwd.getModel().getLayers()) {
					if(l instanceof TiledImageLayer) {
						((TiledImageLayer)l).setDrawTileIDs(mPrefs.getBoolean(PREF_TILE_ID, false));
						((TiledImageLayer)l).setDrawBoundingVolumes(mPrefs.getBoolean(PREF_TILE_BOUNDRIES, false));
					}
				}
				wwd.getModel().setShowTessellationBoundingVolumes(mPrefs.getBoolean(PREF_ELEV_TILE_BOUNDRIES, false));
				wwd.getModel().setShowTessellationTileIds(mPrefs.getBoolean(PREF_ELEV_TILE_ID, false));

				final double verticalExaggeration = Double.valueOf(mPrefs.getString(PREF_VERTICAL_EXAGGERATION, "1"));

				mUseElevation = mPrefs.getBoolean(PREF_ENABLE_ELEVATION, true);

				wwd.getSceneController().setVerticalExaggeration(verticalExaggeration);

				if(!mUseElevation) {
					wwd.getModel().getGlobe().setElevationModel(new ZeroElevationModel());
				} else {
					wwd.getModel().getGlobe().setElevationModel(mElevationModel);
				}
				mView.setFarDistanceMultiplier(Float.valueOf(mPrefs.getString(PREF_HORIZON_MULTIPLIER, "1")));
				mView.setDetectCollisions(mPrefs.getBoolean(PREF_COLLISION_DETECTION, false));
			}
		});
		wwd.requestRender();
	}
}
