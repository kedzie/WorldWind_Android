/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package it.trilogis.android.ww.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import gov.nasa.worldwind.util.Logging;
import it.trilogis.android.ww.R;

import java.util.List;

/**
 * Preference headers
 *
 * Created by kedzie on 4/16/14.
 */
public class SettingsActivity extends PreferenceActivity {

	public static final String PREF_ENABLE_ELEVATION="elevation";
	public static final String PREF_VERTICAL_EXAGGERATION="verticalExaggeration";
	public static final String PREF_HORIZON_MULTIPLIER = "farDistance";
	public static final String PREF_COLLISION_DETECTION = "detectCollisions";
	public static final String PREF_DEBUG_LOGGING="logging";
	public static final String PREF_STATS="showStats";
	public static final String PREF_TILE_ID = "drawTileIds";
	public static final String PREF_TILE_BOUNDRIES = "drawTileBoundries";
	public static final String PREF_ELEV_TILE_ID = "drawElevationTileIds";
	public static final String PREF_ELEV_TILE_BOUNDRIES = "drawElevationTileBoundries";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setDisplayShowHomeEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}
		return false;
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		Logging.verbose("isValidFragment " + fragmentName);
		return true;
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	public static class PerformancePreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.performance_preferences);
		}

		@Override
		public void onPause() {
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

		@Override
		public void onResume() {
			super.onResume();
			final SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			onSharedPreferenceChanged(sharedPreferences, PREF_HORIZON_MULTIPLIER);
			onSharedPreferenceChanged(sharedPreferences, PREF_VERTICAL_EXAGGERATION);
			sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if(key.equals(PREF_HORIZON_MULTIPLIER))
				findPreference(key).setSummary("Horizon Distance Multiplier: " + sharedPreferences.getString(key, ""));
			else if(key.equals(PREF_VERTICAL_EXAGGERATION))
				findPreference(key).setSummary("Vertical Exaggeration: " + sharedPreferences.getString(key, ""));
		}
	}

	public static class DebugPreferenceFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.debug_preferences);
		}


	}
}

