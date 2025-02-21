package org.walkersguide.android.util;

import org.walkersguide.android.tts.TtsSettings;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.server.wg.poi.PoiProfile;

import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.bearing.BearingSensor;
import org.walkersguide.android.sensor.shake.ShakeIntensity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


import java.util.ArrayList;


import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;
import org.walkersguide.android.server.wg.status.OSMMap;

import org.walkersguide.android.server.pt.PtUtility;

import java.util.Map;
import timber.log.Timber;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Set;
import java.lang.Long;
import java.lang.ClassNotFoundException;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.data.object_with_id.Point;
import java.util.List;
import android.text.TextUtils;
import org.walkersguide.android.data.object_with_id.Route;
import de.schildbach.pte.NetworkId;
import com.google.gson.GsonBuilder;
import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;


public class SettingsManager {
    private static final String SETTINGS_FILE_NAME = "WalkersGuide-Android-Settings";
    private static final int MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES = 100;

	// defaults
    // ui settings
    public static final MainActivity.Tab DEFAULT_SELECTED_TAB_MAIN_ACTIVITY = MainActivity.Tab.ROUTER;
    public static final boolean DEFAULT_SHOW_ACTION_BUTTON = true;
    public static final ShakeIntensity DEFAULT_SHAKE_INTENSITY = ShakeIntensity.MEDIUM;
    // bearing sensor
    public static final BearingSensor DEFAULT_BEARING_SENSOR = BearingSensor.COMPASS;
    // poi settings
    public static final int DEFAULT_SELECTED_POI_PROFILE_ID = 1;
    // p2p route settings
    public static final int DEFAULT_SELECTED_ROUTE_ID = 1;
    public static final boolean DEFAULT_AUTO_SKIP_TO_NEXT_ROUTE_POINT = true;

    // keys
    // ui settings
    private static final String KEY_SELECTED_TAB_MAIN_ACTIVITY = "selectedTabMainActivity";
    private static final String KEY_SHOW_ACTION_BUTTON = "showActionButton";
    private static final String KEY_SHAKE_INTENSITY = "shakeIntensity";
    private static final String KEY_SEARCH_TERM_HISTORY = "searchTermHistory";
    // WalkersGuide server
    private static final String KEY_WG_SERVER_URL = "wgServerUrl";
    private static final String KEY_SELECTED_MAP = "selectedMap";
    // public transport
    private static final String KEY_SELECTED_NETWORK_ID = "selectedNetworkId";
    // tts
    private static final String KEY_TTS_SETTINGS = "ttsSettings";
    // bearing sensor
    private static final String KEY_SELECTED_BEARING_SENSOR = "selectedBearingSensor";
    private static final String KEY_BEARING_SENSOR_VALUE_FROM_COMPASS = "bearingSensorValueFromCompass";
    private static final String KEY_BEARING_SENSOR_VALUE_FROM_SATELLITE = "bearingSensorValueFromSatellite";
    private static final String KEY_SIMULATED_BEARING = "simulatedBearing";
    // location sensor
    private static final String KEY_GPS_LOCATION = "gpsLocation";
    private static final String KEY_SIMULATED_POINT_ID = "simulatedPointId";
    // poi settings
    private static final String KEY_SELECTED_POI_PROFILE_ID = "selectedPoiProfileId";
    // p2p route settings
    private static final String KEY_P2P_ROUTE_REQUEST = "p2pRouteRequest";
    private static final String KEY_WAY_CLASS_SETTINGS = "wayClassWeightSettings";
    private static final String KEY_SELECTED_ROUTE_ID = "selectedRouteId";
    private static final String KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT = "autoSkipToNextRoutePoint";


    // class variables
    private static SettingsManager managerInstance;
    private SharedPreferences settings;
    private Gson gson;

    public static SettingsManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized SettingsManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new SettingsManager();
        }
        return managerInstance;
    }

    private SettingsManager() {
		this.settings = GlobalInstance.getContext().getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .create();

        // remove deprecated settings
        if (settings.contains("generalSettings")) {
            Editor editor = settings.edit();
            editor.remove("generalSettings");
            editor.apply();
        }
        if (settings.contains("directionSettings")) {
            Editor editor = settings.edit();
            editor.remove("directionSettings");
            editor.apply();
        }
        if (settings.contains("locationSettings")) {
            Editor editor = settings.edit();
            editor.remove("locationSettings");
            editor.apply();
        }
        if (settings.contains("poiSettings")) {
            Editor editor = settings.edit();
            editor.remove("poiSettings");
            editor.apply();
        }
        if (settings.contains("routeSettings")) {
            Editor editor = settings.edit();
            editor.remove("routeSettings");
            editor.apply();
        }
        if (settings.contains("serverSettings")) {
            Editor editor = settings.edit();
            editor.remove("serverSettings");
            editor.apply();
        }
	}


    /**
     * ui settings
     */

    public MainActivity.Tab getSelectedTabForMainActivity() {
        MainActivity.Tab selectedTab = null;
        try {
            selectedTab = gson.fromJson(
                    settings.getString(KEY_SELECTED_TAB_MAIN_ACTIVITY, ""),
                    MainActivity.Tab.class);
        } catch (ClassCastException e) {}
        return selectedTab != null ? selectedTab : DEFAULT_SELECTED_TAB_MAIN_ACTIVITY;
    }

    public void setSelectedTabForMainActivity(MainActivity.Tab newTab) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SELECTED_TAB_MAIN_ACTIVITY, gson.toJson(newTab));
        editor.apply();
    }

    public boolean getShowActionButton() {
        return settings.getBoolean(KEY_SHOW_ACTION_BUTTON, DEFAULT_SHOW_ACTION_BUTTON);
    }

    public void setShowActionButton(boolean showActionButton) {
        Editor editor = settings.edit();
        editor.putBoolean(KEY_SHOW_ACTION_BUTTON, showActionButton);
        editor.apply();
    }

    public ShakeIntensity getSelectedShakeIntensity() {
        ShakeIntensity shakeIntensity = null;
        try {
            shakeIntensity = gson.fromJson(
                settings.getString(KEY_SHAKE_INTENSITY, ""),
                ShakeIntensity.class);
        } catch (ClassCastException e) {
            // convert legacy value
            shakeIntensity = ShakeIntensity.getShakeIntensityForThreshold(
                    settings.getInt(KEY_SHAKE_INTENSITY, DEFAULT_SHAKE_INTENSITY.threshold));
        }
        return shakeIntensity != null ? shakeIntensity : DEFAULT_SHAKE_INTENSITY;
    }

    public void setSelectedShakeIntensity(ShakeIntensity newShakeIntensity) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SHAKE_INTENSITY, gson.toJson(newShakeIntensity));
        editor.apply();
    }

    // search term history

    public ArrayList<String> getSearchTermHistory() {
        ArrayList<String> searchTermHistory = gson.fromJson(
                settings.getString(KEY_SEARCH_TERM_HISTORY, "[]"),
                new TypeToken<List<String>>() {}.getType());
        if (searchTermHistory == null) {
            searchTermHistory = new ArrayList<String>();
        }
        return searchTermHistory;
    }

    public void addToSearchTermHistory(String newSearchTerm) {
        final int MIN_LENGTH = 3;
        if (newSearchTerm != null && newSearchTerm.length() >= MIN_LENGTH) {
            ArrayList<String> searchTermHistory = getSearchTermHistory();

            // add every single word at least four chars long
            for (String word : newSearchTerm.split("\\s")) {
                if (word.length() >= MIN_LENGTH) {
                    if (searchTermHistory.contains(word)) {
                        searchTermHistory.remove(word);
                    }
                    searchTermHistory.add(0, word);
                }
            }

            // add complete phrase
            if (searchTermHistory.contains(newSearchTerm)) {
                searchTermHistory.remove(newSearchTerm);
            }
            searchTermHistory.add(0, newSearchTerm);

            // clear odd entries
            int numberOfOddEntries = searchTermHistory.size() - MAX_NUMBER_OF_SEARCH_TERM_HISTORY_ENTRIES;
            if (numberOfOddEntries > 0) {
                searchTermHistory.subList(
                        searchTermHistory.size() - numberOfOddEntries,
                        searchTermHistory.size())
                    .clear();
            }

            // save
            Editor editor = settings.edit();
            editor.putString(
                    KEY_SEARCH_TERM_HISTORY,
                    gson.toJson(
                        searchTermHistory,
                        new TypeToken<List<String>>() {}.getType()));
            editor.apply();
        }
    }

    public void clearSearchTermHistory() {
        if (settings.contains(KEY_SEARCH_TERM_HISTORY)) {
            Editor editor = settings.edit();
            editor.remove(KEY_SEARCH_TERM_HISTORY);
            editor.apply();
        }
    }


    /**
     * WalkersGuide server
     */

    public String getServerURL() {
        return settings.getString(KEY_WG_SERVER_URL, BuildConfig.SERVER_URL);
    }

    public void setServerURL(String newServerURL) {
        Editor editor = settings.edit();
        editor.putString(KEY_WG_SERVER_URL, newServerURL);
        editor.apply();
        // clear caches
        GlobalInstance.getInstance().clearCaches();
    }

    public OSMMap getSelectedMap() {
        return gson.fromJson(
                settings.getString(KEY_SELECTED_MAP, ""),
                OSMMap.class);
    }

    public void setSelectedMap(OSMMap newMap) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SELECTED_MAP, gson.toJson(newMap));
        editor.apply();
        // clear caches
        GlobalInstance.getInstance().clearCaches();
    }


    /**
     * public transport
     */

    public NetworkId getSelectedNetworkId() {
        NetworkId networkId = gson.fromJson(
                settings.getString(KEY_SELECTED_NETWORK_ID, ""),
                NetworkId.class);
        return PtUtility.findNetworkProvider(networkId) != null ? networkId : null;
    }

    public void setSelectedNetworkId(NetworkId newId) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SELECTED_NETWORK_ID, gson.toJson(newId));
        editor.apply();
    }


    /**
     * tts settings
     */

    public TtsSettings getTtsSettings() {
        TtsSettings ttsSettings = null;
        try {
            ttsSettings = gson.fromJson(
                    settings.getString(KEY_TTS_SETTINGS, ""),
                    TtsSettings.class);
        } catch (ClassCastException e) {}
        if (ttsSettings == null) {
            ttsSettings = TtsSettings.getDefault();
        }
        return ttsSettings;
    }

    public void setTtsSettings(TtsSettings newTtsSettings) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_TTS_SETTINGS, gson.toJson(newTtsSettings));
        editor.apply();
    }


    /**
     * sensor settings
     */

    // bearing

    public BearingSensor getSelectedBearingSensor() {
        BearingSensor selectedBearingSensor  = gson.fromJson(
                settings.getString(KEY_SELECTED_BEARING_SENSOR, ""),
                BearingSensor.class);
        return selectedBearingSensor != null ? selectedBearingSensor : DEFAULT_BEARING_SENSOR;
    }

    public void setSelectedBearingSensor(BearingSensor newBearingSensor) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SELECTED_BEARING_SENSOR, gson.toJson(newBearingSensor));
        editor.apply();
    }

    public BearingSensorValue getBearingSensorValue(BearingSensor sensor) {
        if (sensor == BearingSensor.COMPASS) {
            return gson.fromJson(
                    settings.getString(KEY_BEARING_SENSOR_VALUE_FROM_COMPASS, ""),
                    BearingSensorValue.class);
        } else if (sensor == BearingSensor.SATELLITE) {
            return gson.fromJson(
                    settings.getString(KEY_BEARING_SENSOR_VALUE_FROM_SATELLITE, ""),
                    BearingSensorValue.class);
        } else {
            return null;
        }
    }

    public void setBearingSensorValue(BearingSensor sensor, BearingSensorValue newValue) {
        Editor editor = settings.edit();
        if (sensor == BearingSensor.COMPASS) {
            editor.putString(
                    KEY_BEARING_SENSOR_VALUE_FROM_COMPASS, gson.toJson(newValue));
        } else if (sensor == BearingSensor.SATELLITE) {
            editor.putString(
                    KEY_BEARING_SENSOR_VALUE_FROM_SATELLITE, gson.toJson(newValue));
        }
        editor.apply();
    }

    public Bearing getSimulatedBearing() {
        return gson.fromJson(
                settings.getString(KEY_SIMULATED_BEARING, ""),
                Bearing.class);
    }

    public void setSimulatedBearing(Bearing newValue) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_SIMULATED_BEARING, gson.toJson(newValue));
        editor.apply();
    }


    // location

    public GPS getGPSLocation() {
        return gson.fromJson(
                settings.getString(KEY_GPS_LOCATION, ""), GPS.class);
    }

    public void setGPSLocation(GPS newLocation) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_GPS_LOCATION, gson.toJson(newLocation));
        editor.apply();
    }

    public Point getSimulatedPoint() {
        return Point.load(
                settings.getLong(KEY_SIMULATED_POINT_ID, -1));
    }

    public void setSimulatedPoint(Point newPoint) {
        Editor editor = settings.edit();
        if (newPoint != null
                && DatabaseProfile.allPoints().add(newPoint)) {
            editor.putLong(KEY_SIMULATED_POINT_ID, newPoint.getId());
        } else if (settings.contains(KEY_SIMULATED_POINT_ID)) {
            editor.remove(KEY_SIMULATED_POINT_ID);
        }
        editor.apply();
    }


    /**
     * poi settings
     */

    public PoiProfile getSelectedPoiProfile() {
        return PoiProfile.load(
                settings.getLong(KEY_SELECTED_POI_PROFILE_ID, DEFAULT_SELECTED_POI_PROFILE_ID));
    }

    public void setSelectedPoiProfile(PoiProfile newProfile) {
        Editor editor = settings.edit();
        if (newProfile != null) {
            editor.putLong(KEY_SELECTED_POI_PROFILE_ID, newProfile.getId());
        } else {
            editor.putLong(KEY_SELECTED_POI_PROFILE_ID, DEFAULT_SELECTED_POI_PROFILE_ID);
        }
        editor.apply();
    }


    /**
     * p2p route settings
     */

    public P2pRouteRequest getP2pRouteRequest() {
        P2pRouteRequest p2pRouteRequest = null;
        try {
            p2pRouteRequest = gson.fromJson(
                    settings.getString(KEY_P2P_ROUTE_REQUEST, ""),
                    P2pRouteRequest.class);
        } catch (ClassCastException e) {}
        if (p2pRouteRequest == null) {
            p2pRouteRequest = P2pRouteRequest.getDefault();
        }
        return p2pRouteRequest;
    }

    public void setP2pRouteRequest(P2pRouteRequest newP2pRouteRequest) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_P2P_ROUTE_REQUEST, gson.toJson(newP2pRouteRequest));
        editor.apply();
    }

    public WayClassWeightSettings getWayClassWeightSettings() {
        WayClassWeightSettings wayClassWeightSettings = null;
        try {
            wayClassWeightSettings = gson.fromJson(
                    settings.getString(KEY_WAY_CLASS_SETTINGS, ""),
                    WayClassWeightSettings.class);
        } catch (ClassCastException e) {}
        if (wayClassWeightSettings == null) {
            wayClassWeightSettings = WayClassWeightSettings.getDefault();
        }
        return wayClassWeightSettings;
    }

    public void setWayClassWeightSettings(WayClassWeightSettings newWayClassWeightSettings) {
        Editor editor = settings.edit();
        editor.putString(
                KEY_WAY_CLASS_SETTINGS, gson.toJson(newWayClassWeightSettings));
        editor.apply();
    }


    public Route getSelectedRoute() {
        return Route.load(
                settings.getLong(KEY_SELECTED_ROUTE_ID, DEFAULT_SELECTED_ROUTE_ID));
    }

    public void setSelectedRoute(Route newRoute) {
        Editor editor = settings.edit();
        if (newRoute != null
                && DatabaseProfile.allRoutes().add(newRoute)) {
            editor.putLong(KEY_SELECTED_ROUTE_ID, newRoute.getId());
        } else {
            editor.putLong(KEY_SELECTED_ROUTE_ID, DEFAULT_SELECTED_ROUTE_ID);
        }
        editor.apply();
    }

    public boolean getAutoSkipToNextRoutePoint() {
        return settings.getBoolean(KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT, DEFAULT_AUTO_SKIP_TO_NEXT_ROUTE_POINT);
    }

    public void setAutoSkipToNextRoutePoint(boolean newValue) {
        Editor editor = settings.edit();
        editor.putBoolean(KEY_AUTO_SKIP_TO_NEXT_ROUTE_POINT, newValue);
        editor.apply();
    }


    /**
     * import and export settings
     */

    public boolean importSettings(File sourceFile) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Map<String, Object> map = null;
        boolean success = true;
        try {
            fis = new FileInputStream(sourceFile);
            ois = new ObjectInputStream(fis);
            map = (Map) ois.readObject();
        } catch(IOException | ClassNotFoundException e) {
            Timber.e("Settings import failed: %1$s", e.getMessage());
            success = false;
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {}
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
            if (! success) {
                return false;
            }
        }

        // restore settings
        SharedPreferences.Editor editor = this.settings.edit();
        editor.clear();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Boolean) {
                editor.putBoolean(e.getKey(), (Boolean)e.getValue());
            } else if (e.getValue() instanceof String) {
                editor.putString(e.getKey(), (String)e.getValue());
            } else if (e.getValue() instanceof Integer) {
                editor.putInt(e.getKey(), (int)e.getValue());
            } else if (e.getValue() instanceof Float) {
                editor.putFloat(e.getKey(), (float)e.getValue());
            } else if (e.getValue() instanceof Long) {
                editor.putLong(e.getKey(), (Long) e.getValue());
            } else if (e.getValue() instanceof Set) {
                editor.putStringSet(e.getKey(), (Set<String>) e.getValue());
            } else {
                Timber.e("Settings type %1$s is unknown", e.getValue().getClass().getName());
            }
        }
        success = editor.commit();

        return success;
    }

    public boolean exportSettings(File destinationFile) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        boolean success = true;
        try {
            fos = new FileOutputStream(destinationFile);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(settings.getAll());
        } catch(IOException e) {
            Timber.e("Settings export failed: %1$s", e.getMessage());
            success = false;
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {}
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
            return success;
        }
    }

}
