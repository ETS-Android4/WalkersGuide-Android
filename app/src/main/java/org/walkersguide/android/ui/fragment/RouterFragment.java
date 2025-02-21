package org.walkersguide.android.ui.fragment;

import org.walkersguide.android.ui.activity.toolbar.tabs.MainActivity;
import androidx.fragment.app.FragmentResultListener;
import org.walkersguide.android.data.profile.ProfileGroup;
import org.walkersguide.android.sensor.bearing.AcceptNewBearing;
import org.walkersguide.android.data.angle.RelativeBearing;
import org.walkersguide.android.data.angle.Bearing;
import org.walkersguide.android.data.angle.bearing.BearingSensorValue;
import org.walkersguide.android.server.wg.p2p.P2pRouteRequest;
import org.walkersguide.android.ui.view.RouteObjectView;
import org.walkersguide.android.ui.view.TextViewAndActionButton;
import android.widget.Toast;
import timber.log.Timber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Vibrator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;



import org.walkersguide.android.data.object_with_id.route.RouteObject;
import org.walkersguide.android.sensor.position.AcceptNewPosition;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.DeviceSensorManager;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.ui.dialog.PlanRouteDialog;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.tts.TTSWrapper;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.data.object_with_id.Point;
import org.walkersguide.android.data.object_with_id.Route;
import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import androidx.fragment.app.FragmentResultListener;
import androidx.annotation.NonNull;
import org.walkersguide.android.ui.fragment.details.RouteDetailsFragment;
import androidx.core.view.ViewCompat;
import java.util.Locale;
import org.walkersguide.android.ui.dialog.select.SelectProfileDialog;
import org.walkersguide.android.data.profile.Profile;
import org.walkersguide.android.database.DatabaseProfile;
import org.walkersguide.android.ui.fragment.object_list.extended.ObjectListFromDatabaseFragment;
import org.walkersguide.android.data.ObjectWithId;
import org.walkersguide.android.util.Helper;
import android.os.Handler;
import android.os.Looper;


public class RouterFragment extends Fragment implements FragmentResultListener {


	// instance constructor

	public static RouterFragment newInstance() {
		RouterFragment fragment = new RouterFragment();
		return fragment;
	}


    // fragment
    private Route route;
    private RouteBroadcastReceiver routeBroadcastReceiver;
	private SettingsManager settingsManagerInstance;
    private TTSWrapper ttsWrapperInstance;

    private TextViewAndActionButton layoutRoute;
    private RouteObjectView layoutCurrentRouteObject;
    private TextView labelHeading, labelDistanceAndBearing;
    private Button buttonPreviousRouteObject, buttonNextRouteObject;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeBroadcastReceiver = new RouteBroadcastReceiver();
		settingsManagerInstance = SettingsManager.getInstance();
        ttsWrapperInstance = TTSWrapper.getInstance();

        getChildFragmentManager()
            .setFragmentResultListener(
                    SelectProfileDialog.REQUEST_SELECT_PROFILE, this, this);
        getChildFragmentManager()
            .setFragmentResultListener(
                    ObjectListFragment.REQUEST_SELECT_OBJECT, this, this);
    }

    @Override public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        if (requestKey.equals(SelectProfileDialog.REQUEST_SELECT_PROFILE)) {
            Profile selectedProfile = (Profile) bundle.getSerializable(SelectProfileDialog.EXTRA_PROFILE);
            if (selectedProfile instanceof DatabaseProfile) {
                ObjectListFromDatabaseFragment.createDialog((DatabaseProfile) selectedProfile, true)
                    .show(getChildFragmentManager(), "SelectPointDialog");
            }
        } else if (requestKey.equals(ObjectListFragment.REQUEST_SELECT_OBJECT)) {
            ObjectWithId newObject = (ObjectWithId) bundle.getSerializable(ObjectListFragment.EXTRA_OBJECT_WITH_ID);
            Timber.d("selected: %1$s", newObject);
            if (newObject instanceof Route) {
                MainActivity.loadRoute(
                        RouterFragment.this.getContext(), (Route) newObject);
            }
        }
    }


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_router_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // check or uncheck filter result menu item
        MenuItem menuItemAutoSkipToNextRoutePoint = menu.findItem(R.id.menuItemAutoSkipToNextRoutePoint);
        if (menuItemAutoSkipToNextRoutePoint != null) {
            menuItemAutoSkipToNextRoutePoint.setChecked(
                    settingsManagerInstance.getAutoSkipToNextRoutePoint());
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemLoadPreviousRoute) {
            SelectProfileDialog.newInstance(ProfileGroup.LOAD_PREVIOUS_ROUTE)
                .show(getChildFragmentManager(), "SelectProfileDialog");

        } else if (item.getItemId() == R.id.menuItemRecalculate
                || item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition
                || item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
            P2pRouteRequest p2pRouteRequest = settingsManagerInstance.getP2pRouteRequest();
            if (route == null) {
                Toast.makeText(
                        getActivity(),
                        GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected),
                        Toast.LENGTH_LONG).show();
                return true;
            }

            // start point
            if (item.getItemId() == R.id.menuItemRecalculateWithCurrentPosition) {
                Point currentLocation = PositionManager.getInstance().getCurrentLocation();
                if (currentLocation != null) {
                    p2pRouteRequest.setStartPoint(currentLocation);
                } else {
                    Toast.makeText(
                            getActivity(),
                            GlobalInstance.getStringResource(R.string.errorNoLocationFound),
                            Toast.LENGTH_LONG).show();
                    return true;
                }
            } else if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                p2pRouteRequest.setStartPoint(route.getDestinationPoint());
            } else {
                p2pRouteRequest.setStartPoint(route.getStartPoint());
            }

            // destination point
            if (item.getItemId() == R.id.menuItemRecalculateReturnRoute) {
                p2pRouteRequest.setDestinationPoint(route.getStartPoint());
            } else {
                p2pRouteRequest.setDestinationPoint(route.getDestinationPoint());
            }

            // set via points and show plan route dialog
            p2pRouteRequest.setViaPoint1(route.getViaPoint1());
            p2pRouteRequest.setViaPoint2(route.getViaPoint2());
            p2pRouteRequest.setViaPoint3(route.getViaPoint3());
            settingsManagerInstance.setP2pRouteRequest(p2pRouteRequest);
            PlanRouteDialog.newInstance()
                .show(getActivity().getSupportFragmentManager(), "PlanRouteDialog");

        } else if (item.getItemId() == R.id.menuItemSkipToFirstRoutePoint) {
            if (route != null) {
                route.jumpToRouteObjectAt(0);
                updateUiExceptDistanceLabel();
            }

        } else if (item.getItemId() == R.id.menuItemAutoSkipToNextRoutePoint) {
            settingsManagerInstance.setAutoSkipToNextRoutePoint(
                    ! settingsManagerInstance.getAutoSkipToNextRoutePoint());

        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    /**
     * create view
     */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_router, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        // content layout
        layoutRoute = (TextViewAndActionButton) view.findViewById(R.id.layoutRoute);
        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        layoutCurrentRouteObject = (RouteObjectView) view.findViewById(R.id.layoutCurrentRouteObject);

        // bottom layout
        labelDistanceAndBearing = (TextView) view.findViewById(R.id.labelDistanceAndBearing);

        buttonPreviousRouteObject = (Button) view.findViewById(R.id.buttonPreviousRouteObject);
        buttonPreviousRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasPreviousRouteObject()) {
                    onPostProcessSkipRouteObjectManually(
                            route.skipToPreviousRouteObject());
                }
            }
        });

        buttonNextRouteObject = (Button) view.findViewById(R.id.buttonNextRouteObject);
        buttonNextRouteObject.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route.hasNextRouteObject()) {
                    onPostProcessSkipRouteObjectManually(
                            route.skipToNextRouteObject());
                }
            }
        });
    }


    /**
     * pause and resume
     */
    private AcceptNewPosition acceptNewPositionForTtsAnnouncement;

    @Override public void onPause() {
        super.onPause();
        if (route != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(routeBroadcastReceiver);
        }
    }

    @Override public void onResume() {
        super.onResume();
        Timber.d("onResume");
        layoutRoute.setVisibility(View.GONE);
        layoutCurrentRouteObject.setVisibility(View.GONE);
        labelDistanceAndBearing.setVisibility(View.GONE);
        buttonPreviousRouteObject.setVisibility(View.GONE);
        buttonNextRouteObject.setVisibility(View.GONE);

        // load route from settings
        route = settingsManagerInstance.getSelectedRoute();
        if (route != null) {
            layoutRoute.setVisibility(View.VISIBLE);
            layoutCurrentRouteObject.setVisibility(View.VISIBLE);
            labelDistanceAndBearing.setVisibility(View.VISIBLE);
            buttonPreviousRouteObject.setVisibility(View.VISIBLE);
            buttonNextRouteObject.setVisibility(View.VISIBLE);
            updateUiExceptDistanceLabel();

            // broadcast filter
            IntentFilter filter = new IntentFilter();
            filter.addAction(PositionManager.ACTION_NEW_LOCATION);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING);
            filter.addAction(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(routeBroadcastReceiver, filter);

            // request current location for labelDistanceAndBearing field
            acceptNewPositionForTtsAnnouncement = AcceptNewPosition.newInstanceForTtsAnnouncement();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    // wait, until onResume is finished and the ui has focus
                    PositionManager.getInstance().requestCurrentLocation();
                }
            }, 200);

        } else {
            labelHeading.setText(
                    GlobalInstance.getStringResource(R.string.errorWgReqNoRouteSelected));
        }
    }

    private void onPostProcessSkipRouteObjectManually(boolean skipWasSuccessful) {
        if (skipWasSuccessful) {
            ttsWrapperInstance.announce(
                    route.getCurrentRouteObject().formatSegmentInstruction());
        }
        updateUiExceptDistanceLabel();
        updateDistanceAndBearingLabel(route.getCurrentRouteObject());
    }

    private void updateUiExceptDistanceLabel() {
        layoutRoute.configureAsSingleObject(route);
        labelHeading.setText(
                String.format(
                    GlobalInstance.getStringResource(R.string.labelRoutePosition),
                    route.getCurrentPosition() + 1,
                    route.getRouteObjectList().size())
                );
        layoutCurrentRouteObject.configureAsSingleObject(route.getCurrentRouteObject());
    }

    private void updateDistanceAndBearingLabel(RouteObject currentRouteObject) {
        labelDistanceAndBearing.setText(
                currentRouteObject.getPoint().formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter));
    }


    private class RouteBroadcastReceiver extends BroadcastReceiver {
        private static final int SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS = 30;

        // distance label
        private AcceptNewPosition acceptNewPositionForDistanceLabel = AcceptNewPosition.newInstanceForDistanceLabelUpdate();
        private AcceptNewBearing acceptNewBearing = AcceptNewBearing.newInstanceForDistanceLabelUpdate();

        private RouteObject lastRouteObject = null;
        private boolean announceNextInstruction = false;

        private boolean shortlyBeforeArrivalAnnounced, arrivalAnnounced;
        private long arrivalTime;

        @Override public void onReceive(Context context, Intent intent) {
            RouteObject currentRouteObject = route.getCurrentRouteObject();

            if (! getActivity().hasWindowFocus()) {
                if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)
                        && intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION) != null
                        && intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                }
                return;
            }

            if (! currentRouteObject.equals(lastRouteObject)) {
                // skipped to next route object
                this.lastRouteObject = currentRouteObject;
                this.shortlyBeforeArrivalAnnounced = false;
                this.arrivalAnnounced = false;
                this.arrivalTime = System.currentTimeMillis();
            }

            if (intent.getAction().equals(PositionManager.ACTION_NEW_LOCATION)) {
                Point currentLocation = (Point) intent.getSerializableExtra(PositionManager.EXTRA_NEW_LOCATION);
                if (currentLocation != null) {
                    if (intent.getBooleanExtra(PositionManager.EXTRA_IS_IMPORTANT, false)
                            || acceptNewPositionForDistanceLabel.updatePoint(currentLocation)) {
                        updateDistanceAndBearingLabel(currentRouteObject);
                    }
                    if (acceptNewPositionForTtsAnnouncement.updatePoint(currentLocation)) {
                        if (this.announceNextInstruction) {
                            ttsWrapperInstance.announce(
                                    currentRouteObject.formatSegmentInstruction());
                            this.announceNextInstruction = false;
                        } else {
                            ttsWrapperInstance.announce(
                                    currentRouteObject.getPoint().formatDistanceAndRelativeBearingFromCurrentLocation(R.plurals.meter));
                        }
                    }
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING)) {
                Bearing currentBearing = (Bearing) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (currentBearing != null
                        && acceptNewBearing.updateBearing(currentBearing)) {
                    updateDistanceAndBearingLabel(currentRouteObject);
                }

            } else if (intent.getAction().equals(DeviceSensorManager.ACTION_NEW_BEARING_VALUE_FROM_SATELLITE)) {
                BearingSensorValue bearingValueFromSatellite = (BearingSensorValue) intent.getSerializableExtra(DeviceSensorManager.EXTRA_BEARING);
                if (bearingValueFromSatellite != null) {
                    // announce shortly before arrival
                    if (
                               ! shortlyBeforeArrivalAnnounced
                            && ! currentRouteObject.getIsFirstRouteObject()
                            && currentRouteObject.getPoint().distanceFromCurrentLocation() < SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS
                            && currentRouteObject.getSegment().getDistance() > SHORTLY_BEFORE_ARRIVAL_THRESHOLD_IN_METERS) {
                        announceShortlyBeforeArrival(currentRouteObject);
                    }
                    // announce arrival
                    if (
                               ! arrivalAnnounced
                            && nextRouteObjectWithinRange(currentRouteObject, bearingValueFromSatellite)
                            && ! walkingReverseDetected(currentRouteObject, bearingValueFromSatellite)
                            && (System.currentTimeMillis() - arrivalTime) > 5000) {
                        announceArrival(currentRouteObject);
                    }
                }
            }
        }

        private boolean nextRouteObjectWithinRange(RouteObject currentRouteObject, BearingSensorValue bearingValueFromSatellite) {
            if (! PositionManager.getInstance().hasCurrentLocation()) {
                return false;
            }

            Integer distance = currentRouteObject
                .getPoint()
                .distanceFromCurrentLocation();
            RelativeBearing relativeBearing = currentRouteObject
                .getPoint()
                .bearingFromCurrentLocation()
                .relativeTo(bearingValueFromSatellite);

            if (distance < 25
                    && relativeBearing.withinRange(80, 280)) {
                return true;
            } else if (distance < 20
                    && relativeBearing.withinRange(65, 295)) {
                return true;
            } else if (distance < 15
                    && relativeBearing.withinRange(50, 310)) {
                return true;
            } else if (distance < 10
                    && relativeBearing.withinRange(35, 325)) {
                return true;
            } else if (distance < 5) {
                return true;
            }
            return false;
        }

        private boolean walkingReverseDetected(RouteObject currentRouteObject, BearingSensorValue bearingValueFromSatellite) {
            if (currentRouteObject.getIsFirstRouteObject() || currentRouteObject.getIsLastRouteObject()) {
                return false;
            }
            return currentRouteObject
                .getSegment()
                .getBearing()
                .shiftBy(currentRouteObject.getTurn().getDegree())
                .shiftBy(-180)      // reverse
                .relativeTo(bearingValueFromSatellite)
                .getDirection() == RelativeBearing.Direction.STRAIGHT_AHEAD;
        }

        private void announceShortlyBeforeArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            ttsWrapperInstance.announce(
                    route.formatShortlyBeforeArrivalAtPointMessage());
        }

        private void announceArrival(RouteObject currentRouteObject) {
            shortlyBeforeArrivalAnnounced = true;
            arrivalAnnounced = true;
            ttsWrapperInstance.announce(
                    route.formatArrivalAtPointMessage());
            Helper.vibrateOnce(Helper.VIBRATION_DURATION_LONG);

            // auto jump to next route point
            if (route.hasNextRouteObject()
                    && settingsManagerInstance.getAutoSkipToNextRoutePoint()) {
                this.announceNextInstruction = route.skipToNextRouteObject();
                updateUiExceptDistanceLabel();
                updateDistanceAndBearingLabel(currentRouteObject);
            }
        }
    }

}
