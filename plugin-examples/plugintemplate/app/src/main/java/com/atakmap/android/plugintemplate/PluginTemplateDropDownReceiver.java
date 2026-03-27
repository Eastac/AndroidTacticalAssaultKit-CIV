
package com.atakmap.android.plugintemplate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapMode;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugintemplate.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;

public class PluginTemplateDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = PluginTemplateDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_PLUGIN";
    private static final String PREF_USE_ONLY_GPS_BEARING = "useOnlyGPSBearing";
    private static final String PREF_COMPASS_MAPMODE = "compass_mapmode";

    private static final String PLUGIN_PREFIX =
            "com.atakmap.android.plugintemplate.heading.";
    private static final String PREF_PREV_USE_ONLY_GPS_BEARING =
            PLUGIN_PREFIX + "prevUseOnlyGPSBearing";
    private static final String PREF_PREV_COMPASS_MAPMODE =
            PLUGIN_PREFIX + "prevCompassMapmode";
    private static final String PREF_HAS_SNAPSHOT =
            PLUGIN_PREFIX + "hasSnapshot";

    private final View templateView;
    private final SharedPreferences atakPrefs;

    private final Switch useOnlyGpsBearingSwitch;
    private final Button restoreButton;
    private final TextView statusText;

    private boolean suppressSwitchCallback;
    private final Context pluginContext;

    /**************************** CONSTRUCTOR *****************************/

    public PluginTemplateDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.pluginContext = context;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);
        atakPrefs = PreferenceManager.getDefaultSharedPreferences(
                mapView.getContext());

        useOnlyGpsBearingSwitch = templateView
                .findViewById(R.id.switch_use_only_gps_bearing);
        restoreButton = templateView.findViewById(R.id.button_restore_previous);
        statusText = templateView.findViewById(R.id.text_heading_status);

        useOnlyGpsBearingSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (suppressSwitchCallback)
                            return;

                        if (isChecked) {
                            applyUseOnlyGpsBearingMode();
                        } else {
                            atakPrefs.edit()
                                    .putBoolean(PREF_USE_ONLY_GPS_BEARING, false)
                                    .apply();
                            refreshUi();
                        }
                    }
                });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restorePreviousSettings();
            }
        });

        refreshUi();

    }

    private void applyUseOnlyGpsBearingMode() {
        capturePreviousValuesIfNeeded();

        atakPrefs.edit().putBoolean(PREF_USE_ONLY_GPS_BEARING, true).apply();

        final int mapMode = getCurrentMapModeValue();
        if (mapMode == MapMode.MAGNETIC_UP.getValue()) {
            setMapMode(MapMode.TRACK_UP);
        }

        refreshUi();
    }

    private void capturePreviousValuesIfNeeded() {
        if (atakPrefs.getBoolean(PREF_HAS_SNAPSHOT, false))
            return;

        final boolean previousUseOnlyGpsBearing = atakPrefs.getBoolean(
                PREF_USE_ONLY_GPS_BEARING, false);

        final String previousMapMode = atakPrefs.getString(PREF_COMPASS_MAPMODE,
                Integer.toString(MapMode.NORTH_UP.getValue()));

        atakPrefs.edit()
                .putBoolean(PREF_PREV_USE_ONLY_GPS_BEARING,
                        previousUseOnlyGpsBearing)
                .putString(PREF_PREV_COMPASS_MAPMODE, previousMapMode)
                .putBoolean(PREF_HAS_SNAPSHOT, true)
                .apply();
    }

    private void restorePreviousSettings() {
        if (!atakPrefs.getBoolean(PREF_HAS_SNAPSHOT, false)) {
            Log.d(TAG, "No saved heading snapshot to restore");
            refreshUi();
            return;
        }

        final boolean previousUseOnlyGpsBearing = atakPrefs.getBoolean(
                PREF_PREV_USE_ONLY_GPS_BEARING, false);
        final String previousMapMode = atakPrefs.getString(
                PREF_PREV_COMPASS_MAPMODE,
                Integer.toString(MapMode.NORTH_UP.getValue()));

        atakPrefs.edit()
                .putBoolean(PREF_USE_ONLY_GPS_BEARING,
                        previousUseOnlyGpsBearing)
                .remove(PREF_PREV_USE_ONLY_GPS_BEARING)
                .remove(PREF_PREV_COMPASS_MAPMODE)
                .putBoolean(PREF_HAS_SNAPSHOT, false)
                .apply();

        try {
            final int restoredMode = Integer.parseInt(previousMapMode);
            setMapMode(MapMode.findFromValue(restoredMode));
        } catch (Exception e) {
            Log.w(TAG, "Could not restore map mode: " + previousMapMode, e);
        }

        refreshUi();
    }

    private int getCurrentMapModeValue() {
        try {
            return Integer.parseInt(
                    atakPrefs.getString(PREF_COMPASS_MAPMODE,
                            Integer.toString(MapMode.NORTH_UP.getValue())));
        } catch (Exception e) {
            return MapMode.NORTH_UP.getValue();
        }
    }

    private void setMapMode(MapMode mode) {
        if (mode == null || mode == MapMode.UNDEFINED)
            mode = MapMode.NORTH_UP;

        AtakBroadcast.getInstance().sendBroadcast(new Intent(mode.getIntent()));
    }

    private void refreshUi() {
        final boolean enabled = atakPrefs.getBoolean(PREF_USE_ONLY_GPS_BEARING,
                false);
        final int mapMode = getCurrentMapModeValue();
        final boolean hasSnapshot = atakPrefs.getBoolean(PREF_HAS_SNAPSHOT,
                false);

        suppressSwitchCallback = true;
        useOnlyGpsBearingSwitch.setChecked(enabled);
        suppressSwitchCallback = false;

        restoreButton.setEnabled(hasSnapshot);

        final String mapModeLabel = mapModeToLabel(mapMode);
        statusText.setText(pluginContext.getString(
                R.string.heading_status_template,
                Boolean.toString(enabled),
                mapModeLabel,
                Boolean.toString(hasSnapshot)));
    }

    private String mapModeToLabel(int mapModeValue) {
        switch (MapMode.findFromValue(mapModeValue)) {
            case TRACK_UP:
                return "TRACK_UP";
            case MAGNETIC_UP:
                return "MAGNETIC_UP";
            case USER_DEFINED_UP:
                return "USER_DEFINED_UP";
            case NORTH_UP:
                return "NORTH_UP";
            default:
                return "UNDEFINED";
        }
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            refreshUi();
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
