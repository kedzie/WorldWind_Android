/*
 * Copyright (C) 2013 Trilogis S.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.trilogis.android.ww.dialogs;

import android.app.*;
import android.widget.*;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import it.trilogis.android.ww.R;
import it.trilogis.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;
import it.trilogis.android.ww.view.DragListView;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import gov.nasa.worldwind.terrain.ElevationModel;

/**
 * @author Marek Kedzierski
 */
public class TocFragment extends Fragment {
    private static final String TAG = "TocFragment";
    private LayerArrayAdapter mListViewAdapter;
    private DragListView mListView;
	WorldWindow wwd;
	private SceneController sceneController;
	private ElevationModel elevationModel;

	private static final double VERT_EXAGG_INCREMENT = 0.1;
	private Button lowerButton;
	private Button raiseButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.toc_view_dialog, null);
		mListView = (DragListView) view.findViewById(android.R.id.list);

		View removerow = view.findViewById(R.id.removeView);
		mListView.setTrashcan((ImageView) removerow);
		mListView.setDropListener(onDrop);
		mListView.setRemoveListener(onRemove);

		lowerButton = (Button) view.findViewById(R.id.lowerButton);
		lowerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sceneController.setVerticalExaggeration(
						Math.max(0, sceneController.getVerticalExaggeration()-VERT_EXAGG_INCREMENT));
				updateVerticalExaggerationButtonState();
			}
		});
		raiseButton = (Button) view.findViewById(R.id.raiseButton);
		raiseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sceneController.setVerticalExaggeration(
						sceneController.getVerticalExaggeration() + VERT_EXAGG_INCREMENT);
				updateVerticalExaggerationButtonState();
			}
		});

		// Init WorldWind Data
		if(wwd!=null)
			initWorldWindLayerAdapter();
		return view;
	}

	private void updateVerticalExaggerationButtonState() {
		lowerButton.setEnabled(sceneController.getVerticalExaggeration()!=0);
	}

    /*
     * (non-Javadoc)
     * @see android.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mListViewAdapter != null) {
            mListViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Set the class parameter that will be used to fill adapter and perform operations.
     * 
     * @param wwd
     *            The WorldWind context to be used.
     */
    public void setWorldWindData(WorldWindow wwd) {
        if (null == wwd) {
            Log.e(TAG, "Setting null world wind data!!!");
            return;
        }
        this.wwd = wwd;
		sceneController = wwd.getSceneController();
        // Initialize list view, adapters and so on...
        initWorldWindLayerAdapter();
    }

    /**
	 * 
	 */
    private void initWorldWindLayerAdapter() {
        if (null == wwd) {
            Log.e(TAG, "Trying to initialize layer adapter with not valid WorldWind context!!");
            return;
        }
        if (null == mListView) {
            Log.e(TAG, "Trying to initialize layer list view, but list view is null!!");
            return;
        }
        LayerList layers = wwd.getModel().getLayers();
        mListViewAdapter = new LayerArrayAdapter(getActivity(), layers);
        mListView.setAdapter(mListViewAdapter);
    }

    private DragListView.DropListener onDrop = new DragListView.DropListener() {
        public void drop(int from, int to) {
            Layer item = mListViewAdapter.getItem(from);
            mListViewAdapter.remove(item);
            mListViewAdapter.insert(item, to);
        }
    };

    private DragListView.RemoveListener onRemove = new DragListView.RemoveListener() {
        public void remove(int which) {
            mListViewAdapter.remove(mListViewAdapter.getItem(which));
        }
    };

    private class LayerArrayAdapter extends ArrayAdapter<Layer> {

        private final List<Layer> list;

        public LayerArrayAdapter(Activity context, List<Layer> list) {
            super(context, R.layout.toc_list_view_item, list);
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Layer layer = list.get(position);
            View retval = convertView;
            if (retval == null) {
                LayoutInflater inflator = getActivity().getLayoutInflater();
                retval = inflator.inflate(R.layout.toc_list_view_item, null);
            }
            CheckBox checkbox = (CheckBox) retval.findViewById(R.id.toc_item_checkbox);
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    layer.setEnabled(buttonView.isChecked());
                }
            });
            checkbox.setChecked(layer.isEnabled());
            checkbox.setText(layer.getName());
            return retval;
        }
    }

    // ===================== ADD WMS ================================0
    private void openAddWMSDialog() {
        AddWMSDialog wmsLayersDialog = new AddWMSDialog();
        wmsLayersDialog.setOnAddWMSLayersListener(mListener);
        wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
    }

    private OnAddWMSLayersListener mListener = new OnAddWMSLayersListener() {

        public void onAddWMSLayers(List<Layer> layersToAdd) {

            if (null == layersToAdd || layersToAdd.isEmpty() || null == wwd) {
                Log.w(TAG, "Null or empty layers/WorldWindContext to add!");
                return;
            }
            for (Layer lyr : layersToAdd) {
                boolean added = wwd.getModel().getLayers().addIfAbsent(lyr);
                Log.d(TAG, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!");

            }
            if (mListViewAdapter != null) {
                mListViewAdapter.notifyDataSetChanged();
            }

        }
    };
}
