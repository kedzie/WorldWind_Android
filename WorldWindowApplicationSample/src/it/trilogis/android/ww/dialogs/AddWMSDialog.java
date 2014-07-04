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

import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import it.trilogis.android.ww.R;
import it.trilogis.android.ww.WorldWindowActivity;

/**
 * @author Nicola Dorigatti
 */
public class AddWMSDialog extends DialogFragment {
    private static final String TAG = "AddWMSDialog";
    private Thread downloadThread;
    LayerInfoAdapter mListViewAdapter = null;
    private ListView mListView;

    private OnAddWMSLayersListener mListener;

    public interface OnAddWMSLayersListener {
        public void onAddWMSLayers(List<Layer> layersToAdd);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        String defaultWMSURL = WorldWindowActivity.DEFAULT_WMS_URL;

        if (null != arguments) {
            defaultWMSURL = arguments.getString("WMSURL");
            if (null == defaultWMSURL || defaultWMSURL.trim().isEmpty())
                defaultWMSURL = WorldWindowActivity.DEFAULT_WMS_URL;
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_wms_add, null);

        // Lookup views
        mListView = (ListView) view.findViewById(R.id.wms_layerslistview);
        mListViewAdapter = new LayerInfoAdapter(getActivity(), layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListView.setAdapter(mListViewAdapter);

        final LinearLayout mainLayout = (LinearLayout) view.findViewById(R.id.main_add_wms_layout);
        mainLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        final EditText urlEditText = (EditText) view.findViewById(R.id.wms_url_et);
        urlEditText.setText(defaultWMSURL);
        final Button getCapabilitiesButton = (Button) view.findViewById(R.id.get_capabilities_btn);
        getCapabilitiesButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
                final String WMSURLtoUSE = urlEditText.getText().toString();
                // TODO Should Check url validity before starting to download WMS capabilities
                if (downloadThread == null) {
                    getCapabilitiesButton.setEnabled(false);
                    downloadThread = new Thread(new Runnable() {

                        public void run() {
                            downloadCapabilities(WMSURLtoUSE);
                            downloadThread = null;
                            AddWMSDialog.this.getActivity().runOnUiThread(new Runnable() {

                                public void run() {
                                    // update listVIew
                                    updateLayerInfoList(getActivity());
                                    getCapabilitiesButton.setEnabled(true);
                                }
                            });
                        }
                    });
                    downloadThread.start();
                }

            }
        });

        builder.setView(view);
        builder.setPositiveButton(getString(android.R.string.ok), new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                ArrayList<Layer> retval = new ArrayList<Layer>();
                LayerInfo[] infos = mListViewAdapter.getLayerInfos();
                for (LayerInfo info : infos) {
                    if (info.selected) {
                        Layer layer = createLayer(info.caps, info.params);
                        if (null != layer) {
                            retval.add(layer);
                        }
                    }
                }
                Toast.makeText(getActivity(), "Created layers that will be added to worldWind: " + retval.size(), Toast.LENGTH_LONG).show();
                if (null != mListener) {
                    mListener.onAddWMSLayers(retval);
                }
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void setOnAddWMSLayersListener(OnAddWMSLayersListener listener) {
        mListener = listener;
    }

    private void downloadCapabilities(String wmsURL) {
        try {
            layerInfos.clear();
            URI serverURI = new URI(wmsURL.trim());
            WMSCapabilities caps = WMSCapabilities.retrieve(serverURI);
            caps.parse();
            final List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
            if (namedLayerCaps == null)
                return;
            for (WMSLayerCapabilities lc : namedLayerCaps) {
                Set<WMSLayerStyle> styles = lc.getStyles();

                if (styles == null || styles.size() == 0) {
                    // Log.d(TAG, "Null or empty styles!");
                    LayerInfo layerInfo = createLayerInfo(caps, lc, null);
                    layerInfos.add(layerInfo);
                } else {
                    // Log.d(TAG, "Styles is not null and not empty");
                    for (WMSLayerStyle style : styles) {
                        LayerInfo layerInfo = createLayerInfo(caps, lc, style);
                        layerInfos.add(layerInfo);
                    }
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void updateLayerInfoList(Context context) {
        mListViewAdapter = new LayerInfoAdapter(getActivity(), layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListView.setAdapter(mListViewAdapter);
        // mListViewAdapter.setLayerInfos(layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListViewAdapter.notifyDataSetChanged();
        Log.i(TAG, "Updated listview");
    }

    private Layer createLayer(WMSCapabilities caps, AVListImpl params) {
        AVList configParams = params.copy(); // Copy to insulate changes from the caller.

        // Some wms servers are slow, so increase the timeouts and limits used by world wind's retrievers.
        configParams.setValue(AVKey.URL_CONNECT_TIMEOUT, 30000);
        configParams.setValue(AVKey.URL_READ_TIMEOUT, 30000);
        configParams.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

        try {
            String factoryKey = AVKey.LAYER_FACTORY;
            Factory factory = (Factory) WorldWind.createConfigurationComponent(factoryKey);
            return (Layer) factory.createFromConfigSource(caps, params);
        } catch (Exception e) {
            Log.e(TAG, "Exception creating layer WMS: " + e.getMessage());
            // Ignore the exception, and just return null.
        }

        return null;
    }

    // ------------LAYER INFO UTILS
    protected final TreeSet<LayerInfo> layerInfos = new TreeSet<LayerInfo>(new Comparator<LayerInfo>() {
        public int compare(LayerInfo infoA, LayerInfo infoB) {
            String nameA = infoA.getName();
            String nameB = infoB.getName();
            return nameA.compareTo(nameB);
        }
    });

    protected LayerInfo createLayerInfo(WMSCapabilities caps, WMSLayerCapabilities layerCaps, WMSLayerStyle style) {
        // Create the layer info specified by the layer's capabilities entry and
        // the selected style.

        LayerInfo linfo = new LayerInfo();
        linfo.caps = caps;
        linfo.params = new AVListImpl();
        linfo.params.setValue(AVKey.LAYER_NAMES, layerCaps.getName());
        if (style != null)
            linfo.params.setValue(AVKey.STYLE_NAMES, style.getName());
        String abs = layerCaps.getLayerAbstract();
        if (!WWUtil.isEmpty(abs))
            linfo.params.setValue(AVKey.LAYER_ABSTRACT, abs);

        linfo.params.setValue(AVKey.DISPLAY_NAME, makeTitle(caps, linfo));

        return linfo;
    }

    protected static String makeTitle(WMSCapabilities caps, LayerInfo layerInfo) {
        String layerNames = layerInfo.params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = layerInfo.params.getStringValue(AVKey.STYLE_NAMES);
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++) {
            if (sb.length() > 0)
                sb.append(", ");

            String layerName = lNames[i];
            WMSLayerCapabilities lc = caps.getLayerByName(layerName);
            String layerTitle = lc.getTitle();
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i)
                continue;

            String styleName = sNames[i];
            WMSLayerStyle style = lc.getStyleByName(styleName);
            if (style == null)
                continue;

            sb.append(" : ");
            String styleTitle = style.getTitle();
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }

    protected static class LayerInfo {
        protected WMSCapabilities caps;
        protected AVListImpl params = new AVListImpl();
        protected boolean selected = false;

        protected String getTitle() {
            return params.getStringValue(AVKey.DISPLAY_NAME);
        }

        protected String getName() {
            return params.getStringValue(AVKey.LAYER_NAMES);
        }

        protected String getAbstract() {
            return params.getStringValue(AVKey.LAYER_ABSTRACT);
        }
    }

    private class LayerInfoAdapter extends ArrayAdapter<LayerInfo> {

        private LayerInfo[] types;

        public LayerInfoAdapter(Activity context, LayerInfo[] types) {
            super(context, android.R.layout.simple_list_item_1, types);
            this.types = types;
        }

        public LayerInfo[] getLayerInfos() {
            return types;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayerInfo layerType = types[position];
            View retval = convertView;
            if (retval == null) {
                retval = new CheckBox(getContext());
            }
            ((CheckBox) retval).setText(layerType.getTitle()+"-"+layerType.caps.getImageFormats());
            ((CheckBox) retval).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    layerType.selected = buttonView.isChecked();
                }
            });
            ((CheckBox) retval).setChecked(layerType.selected);
            return retval;
        }
    }
}
