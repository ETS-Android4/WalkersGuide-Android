package org.walkersguide.android.sensor;

import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.sensor.bearing.BearingSensorAccuracyRating;

import org.walkersguide.android.util.GlobalInstance;
import android.annotation.TargetApi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;


import android.location.Location;
import android.location.LocationManager;

import android.Manifest;

import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.Math;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import org.json.JSONException;

import org.walkersguide.android.BuildConfig;
import org.walkersguide.android.database.util.AccessDatabase;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.point.GPS;
import org.walkersguide.android.util.SettingsManager;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import android.annotation.SuppressLint;
import java.util.Locale;
import org.walkersguide.android.util.Helper;
import timber.log.Timber;


public class PositionManager implements android.location.LocationListener {
    private DateFormat timeFormatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private DateFormat dateAndTimeFormatter = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);


    /**
     * singleton
     */

    private static PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;

    public static PositionManager getInstance() {
        if (positionManagerInstance == null){
            positionManagerInstance = getInstanceSynchronized();
        }
        return positionManagerInstance;
    }

    private static synchronized PositionManager getInstanceSynchronized() {
        if (positionManagerInstance == null){
            positionManagerInstance = new PositionManager();
        }
        return positionManagerInstance;
    }

    private PositionManager() {
        this.settingsManagerInstance = SettingsManager.getInstance();
    }


    /**
     * start and stop location updates
     */
    public static final String ACTION_LOCATION_PROVIDER_DISABLED = "action.LocationProviderDisabled";
    public static final String ACTION_FOREGROUND_LOCATION_PERMISSION_DENIED = "action.foregroundLocationPermissionDenied";

    public static boolean locationServiceEnabled() {
        Context context = GlobalInstance.getContext();
        if (! ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }
        return true;
    }

    public static boolean foregroundLocationPermissionGranted() {
        Context context = GlobalInstance.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static boolean backgroundLocationPermissionGranted() {
        Context context = GlobalInstance.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private LocationManager locationManager = null;
    private boolean gpsFixFound = false;

    @SuppressLint("MissingPermission")
    public void startGPS() {
        if (locationManager == null) {
            locationManager = (LocationManager) GlobalInstance.getContext().getSystemService(Context.LOCATION_SERVICE);

            if (! locationServiceEnabled()) {
                locationManager = null;
                Intent locationProviderDisabledIntent = new Intent(ACTION_LOCATION_PROVIDER_DISABLED);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(locationProviderDisabledIntent);

            } else if (! foregroundLocationPermissionGranted()) {
                locationManager = null;
                Intent locationPermissionDeniedIntent = new Intent(ACTION_FOREGROUND_LOCATION_PERMISSION_DENIED);
                LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(locationPermissionDeniedIntent);

            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);
                gpsFixFound = false;
            }

            /*
            if (BuildConfig.DEBUG) {
                appendToLog(
                        "newLocationAttributes",
                        dateAndTimeFormatter.format(new Date(System.currentTimeMillis())));
            }*/
        }
    }

    public void stopGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }


    /**
     * current location
     */
    public static final String ACTION_NEW_LOCATION = "new_location";
    public static final String EXTRA_NEW_LOCATION = "newLocation";
    public static final String EXTRA_IS_IMPORTANT = "isImportant";

    public Point getCurrentLocation() {
        if (this.simulationEnabled) {
            return getSimulatedLocation();
        } else {
            return getGPSLocation();
        }
    }

    public boolean hasCurrentLocation() {
        return getCurrentLocation() != null;
    }

    public void requestCurrentLocation() {
        broadcastCurrentLocation(false);
    }

    private void broadcastCurrentLocation(boolean isImportant) {
        Intent intent = new Intent(ACTION_NEW_LOCATION);
        intent.putExtra(EXTRA_NEW_LOCATION, getCurrentLocation());
        intent.putExtra(EXTRA_IS_IMPORTANT, isImportant);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }


    /**
     * location from gps
     */
    public static final String ACTION_NEW_GPS_LOCATION = "new_gps_location";

    public void requestGPSLocation() {
        broadcastGPSLocation();
    }

    private void broadcastGPSLocation() {
        Intent intent = new Intent(ACTION_NEW_GPS_LOCATION);
        intent.putExtra(EXTRA_NEW_LOCATION, getGPSLocation());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public GPS getGPSLocation() {
        return settingsManagerInstance.getGPSLocation();
    }

    @Override public void onLocationChanged(Location newLocationObject) {
        // try to create gps point from location object
        GPS.Builder gpsBuilder = new GPS.Builder(
                newLocationObject.getLatitude(), newLocationObject.getLongitude());

        // optional args
        if (newLocationObject.getProvider() != null) {
            gpsBuilder.setProvider(newLocationObject.getProvider());
        }
        if (newLocationObject.hasAccuracy()) {
            gpsBuilder.setAccuracy(newLocationObject.getAccuracy());
        }
        if (newLocationObject.hasAltitude()) {
            gpsBuilder.setAltitude(newLocationObject.getAltitude());
        }
        if (newLocationObject.getExtras() != null) {
            Bundle locationExtras = newLocationObject.getExtras();
            if (locationExtras.containsKey("satellites")
                    && locationExtras.getInt("satellites") >= 0) {
                gpsBuilder.setNumberOfSatellites(locationExtras.getInt("satellites"));
            }
        }
        if (newLocationObject.hasSpeed()) {
            // convert from m/s into km/h
            gpsBuilder.setSpeed(newLocationObject.getSpeed() * 3.6f);
        }

        // bearing sub object
        if (newLocationObject.hasBearing()) {
            gpsBuilder.setBearing(
                    new BearingSensorValue(
                        Math.round(newLocationObject.getBearing()),
                        newLocationObject.getTime(),
                        extractBearingSensorAccuracyRating(newLocationObject)));
        }

        // build
        GPS newLocation = null;
        try {
            newLocation = gpsBuilder.build();
        } catch (JSONException e) {}

        // compare
        if (isBetterLocation(getGPSLocation(), newLocation)) {
            boolean isAtLeastFiftyMetersAway = getGPSLocation() == null
                || getGPSLocation().distanceTo(newLocation) >= 50;
            settingsManagerInstance.setGPSLocation(newLocation);

            // broadcast new gps position action
            broadcastGPSLocation();
            // broadcast new location action
            if (! this.simulationEnabled) {
                broadcastCurrentLocation(
                        ! gpsFixFound && isAtLeastFiftyMetersAway);
            }

            // first gps fix
            if (! gpsFixFound) {
                gpsFixFound = true;
                // notify user about first gps fix after app start
                Helper.vibratePattern(new long[] {250, 50, 250, 50});
            }
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    private static boolean isBetterLocation(GPS currentLocation, GPS newLocation) {
        if (newLocation == null) {
            return false;
        } else if (currentLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTimestamp() - currentLocation.getTimestamp();
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 10000;
        boolean isSignificantlyNewer = timeDelta > 20000;
        boolean isMuchMuchNewer = timeDelta > 180000;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
        int accuracyThresholdValue = 15;
        boolean isMoreAccurateThanThresholdValue = newLocation.getAccuracy() <= accuracyThresholdValue;
        boolean isMoreAccurate = false;
        boolean isABitLessAccurate = false;
        boolean isSignificantlyLessAccurate = false;
        if (newLocation.getAccuracy() < (2*accuracyThresholdValue)) {
            isMoreAccurate = accuracyDelta <= 0;
            isABitLessAccurate = accuracyDelta <= 10;
            isSignificantlyLessAccurate = accuracyDelta <= 30;
        } else {
            isMoreAccurate = accuracyDelta < 0;
            isABitLessAccurate = accuracyDelta < 10;
            isSignificantlyLessAccurate = accuracyDelta < 30;
        }

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = newLocation.getProvider().equals(currentLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isNewer && isMoreAccurateThanThresholdValue) {
            return true;
        } else if (isNewer && isMoreAccurate && isFromSameProvider) {
            return true;
        } else if (isABitNewer && isABitLessAccurate && isFromSameProvider) {
            return true;
        } else if (isSignificantlyNewer && isSignificantlyLessAccurate) {
            return true;
        } else if (isMuchMuchNewer) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private BearingSensorAccuracyRating extractBearingSensorAccuracyRating(Location location) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && location.hasBearingAccuracy()) {
            int bearingAccuracyDegrees = Math.round(location.getBearingAccuracyDegrees());
            // return accuracy rating value
            if (bearingAccuracyDegrees < 10) {
                return BearingSensorAccuracyRating.HIGH;
            } else if (bearingAccuracyDegrees < 20) {
                return BearingSensorAccuracyRating.MEDIUM;
            } else {
                return BearingSensorAccuracyRating.LOW;
            }
        } else {
            return null;
        }
    }


    /**
     * location from simulation
     */

    // enable / disable simulation
    public static final String ACTION_LOCATION_SIMULATION_STATE_CHANGED = "action.locationSimulationStateChanged";
    public static final String EXTRA_SIMULATION_ENABLED = "simulationEnabled.locationSimulationStateChanged";

    private boolean simulationEnabled = false;

    public boolean getSimulationEnabled() {
        return this.simulationEnabled;
    }

    public void setSimulationEnabled(boolean enabled) {
        this.simulationEnabled = enabled;

        Intent intent = new Intent(ACTION_LOCATION_SIMULATION_STATE_CHANGED);
        intent.putExtra(EXTRA_SIMULATION_ENABLED, enabled);
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);

        broadcastCurrentLocation(true);
    }

    // change simulated location
    public static final String ACTION_NEW_SIMULATED_LOCATION = "new_simulated_location";

    public void requestSimulatedLocation() {
        broadcastSimulatedLocation();
    }

    private void broadcastSimulatedLocation() {
        Intent intent = new Intent(ACTION_NEW_SIMULATED_LOCATION);
        intent.putExtra(EXTRA_NEW_LOCATION, getSimulatedLocation());
        LocalBroadcastManager.getInstance(GlobalInstance.getContext()).sendBroadcast(intent);
    }

    public Point getSimulatedLocation() {
        return settingsManagerInstance.getSimulatedPoint();
    }

    public void setSimulatedLocation(Point newPoint) {
        if (newPoint != null) {
            settingsManagerInstance.setSimulatedPoint(newPoint);
            // add to history
            DatabaseProfile.simulatedPoints().add(newPoint);
            // broadcast new simulated location action
            broadcastSimulatedLocation();
            // broadcast new location action
            if (this.simulationEnabled) {
                broadcastCurrentLocation(true);
            }
        }
    }


    /**
     * log to text file
     */
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ROOT);

    private static void appendToLog(String fileName, String message) {
        File file = new File(
                GlobalInstance.getContext().getExternalFilesDir(null),
                String.format("%1$s.log", fileName));
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(
                    String.format(
                        "%1$s\t%2$s\n",
                        message,
                        sdf.format(new Date(System.currentTimeMillis())))
                    );
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
