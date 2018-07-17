package org.walkersguide.android.ui.fragment.pointdetails;

import android.app.Activity;

import android.content.Context;

import android.os.Bundle;

import android.support.v4.app.Fragment;

import android.text.format.DateFormat;
import android.text.TextUtils;
import android.text.util.Linkify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.basic.point.Entrance;
import org.walkersguide.android.data.basic.point.GPS;
import org.walkersguide.android.data.basic.point.Intersection;
import org.walkersguide.android.data.basic.point.PedestrianCrossing;
import org.walkersguide.android.data.basic.point.POI;
import org.walkersguide.android.data.basic.point.Point;
import org.walkersguide.android.data.basic.point.Station;
import org.walkersguide.android.data.basic.point.StreetAddress;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.helper.StringUtility;
import org.walkersguide.android.listener.FragmentCommunicator;
import org.walkersguide.android.R;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.data.station.Line;
import android.content.Intent;


public class PointDetailsFragment extends Fragment implements FragmentCommunicator {

    // constants
    private static final int TEXTVIEW_NO_AUTO_LINK = -1;
    private static final int TEXTVIEW_NO_ID = -1;
    private static final int TEXTVIEW_OUTER_BUILDING_ID = 1;

	// Store instance variables
    private PointWrapper pointWrapper;

	// ui components
    private LinearLayout layoutAttributes;

	// newInstance constructor for creating fragment with arguments
	public static PointDetailsFragment newInstance(PointWrapper pointWrapper) {
		PointDetailsFragment pointDetailsFragmentInstance = new PointDetailsFragment();
        Bundle args = new Bundle();
        try {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, pointWrapper.toJson().toString());
        } catch (JSONException e) {
            args.putString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
        }
        pointDetailsFragmentInstance.setArguments(args);
		return pointDetailsFragmentInstance;
	}

	@Override public void onAttach(Context context) {
		super.onAttach(context);
		Activity activity;
		if (context instanceof Activity) {
			activity = (Activity) context;
			// instanciate FragmentCommunicator interface to get data from MainActivity
			((PointDetailsActivity) activity).pointDetailsFragmentCommunicator = this;
		}
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_point_details, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        try {
            pointWrapper = new PointWrapper(
                    getActivity(), new JSONObject(getArguments().getString(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "")));
        } catch (JSONException e) {
            pointWrapper = null;
        }

        // attributes layout
		layoutAttributes = (LinearLayout) view.findViewById(R.id.layoutAttributes);
    }

    @Override public void onFragmentEnabled() {
        layoutAttributes.removeAllViews();

        if (pointWrapper != null
                && pointWrapper.getPoint() instanceof Point) {
            Point point = (Point) pointWrapper.getPoint();

            if (pointWrapper.getPoint() instanceof Entrance) {
                Entrance entrance = (Entrance) pointWrapper.getPoint();
                if (! entrance.getLabel().equals("")) {
                   	addTextView(
                            TEXTVIEW_NO_ID,
                			getResources().getString(R.string.labelPointEntranceAttributesHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                   	addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s",
            			        getResources().getString(R.string.labelPointEntranceLabel),
                                entrance.getLabel()),
                    false, TEXTVIEW_NO_AUTO_LINK);
                }

            } else if (pointWrapper.getPoint() instanceof GPS) {
                GPS gps = (GPS) pointWrapper.getPoint();
                // heading
                addTextView(
                        TEXTVIEW_NO_ID,
                        getResources().getString(R.string.labelPointGPSDetailsHeading),
                        true, TEXTVIEW_NO_AUTO_LINK);
                // provider
                if (gps.getNumberOfSatellites() >= 0) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s, %3$d %4$s",
                                getResources().getString(R.string.labelGPSProvider),
                                gps.getProvider(),
                                gps.getNumberOfSatellites(),
                                getResources().getString(R.string.unitSatellites)),
                            false, TEXTVIEW_NO_AUTO_LINK);
                } else {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s",
                                getResources().getString(R.string.labelGPSProvider),
                                gps.getProvider()),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
                // accuracy
                if (gps.getAccuracy() >= 0.0) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$d %3$s",
                                getResources().getString(R.string.labelGPSAccuracy),
                                Math.round(gps.getAccuracy()),
                                getResources().getString(R.string.unitMeters)),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
                // altitude
                if (gps.getAltitude() >= 0.0) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$d %3$s",
                                getResources().getString(R.string.labelGPSAltitude),
                                Math.round(gps.getAltitude()),
                                getResources().getString(R.string.unitMeters)),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
                // timestamp
                if (gps.getTime() >= 0) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s %3$s",
                                getResources().getString(R.string.labelGPSTime),
                                DateFormat.getDateFormat(getActivity()).format(new Date(gps.getTime())),
                                DateFormat.getTimeFormat(getActivity()).format(new Date(gps.getTime()))),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }

            } else if (pointWrapper.getPoint() instanceof Intersection) {
                Intersection intersection  = (Intersection) pointWrapper.getPoint();
                // heading
                addTextView(
                        TEXTVIEW_NO_ID,
                        getResources().getString(R.string.labelPointIntersectionAttributesHeading),
                        true, TEXTVIEW_NO_AUTO_LINK);
                // number of streets
                addTextView(
                        TEXTVIEW_NO_ID,
                        String.format(
                            "%1$s: %2$d",
                            getResources().getString(R.string.labelPointIntersectionNumberOfStreets),
                            intersection.getNumberOfStreets()),
                        false, TEXTVIEW_NO_AUTO_LINK);
                addTextView(
                        TEXTVIEW_NO_ID,
                        String.format(
                            "%1$s: %2$d",
                            getResources().getString(R.string.labelPointIntersectionNumberOfBigStreets),
                            intersection.getNumberOfStreetsWithName()),
                        false, TEXTVIEW_NO_AUTO_LINK);
                // pedestrian crossings nearby
                addTextView(
                        TEXTVIEW_NO_ID,
                        String.format(
                            "%1$s: %2$d",
                            getResources().getString(R.string.labelPointIntersectionNumberOfPedestrianCrossingsNearBy),
                            intersection.getPedestrianCrossingList().size()),
                        false, TEXTVIEW_NO_AUTO_LINK);

            } else if (pointWrapper.getPoint() instanceof PedestrianCrossing) {
                PedestrianCrossing pedestrianCrossing = (PedestrianCrossing) pointWrapper.getPoint();
                if (pedestrianCrossing.getTrafficSignalsSound() != -1
                        || pedestrianCrossing.getTrafficSignalsVibration() != -1) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            getResources().getString(R.string.labelPointTrafficSignalsAttributesHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                    // traffic signals sound
                    String trafficSignalsSoundValue = null;
                    switch (pedestrianCrossing.getTrafficSignalsSound()) {
                        case PedestrianCrossing.TRAFFIC_SIGNAL.NO:
                            trafficSignalsSoundValue = getResources().getString(R.string.dialogNo);
                            break;
                        case PedestrianCrossing.TRAFFIC_SIGNAL.YES:
                            trafficSignalsSoundValue = getResources().getString(R.string.dialogYes);
                            break;
                        default:
                            trafficSignalsSoundValue = null;
                    }
                    if (trafficSignalsSoundValue != null) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointTrafficSignalsSound),
                                    trafficSignalsSoundValue),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                    // traffic signals vibration
                    String trafficSignalsVibrationValue = null;
                    switch (pedestrianCrossing.getTrafficSignalsVibration()) {
                        case PedestrianCrossing.TRAFFIC_SIGNAL.NO:
                            trafficSignalsVibrationValue = getResources().getString(R.string.dialogNo);
                            break;
                        case PedestrianCrossing.TRAFFIC_SIGNAL.YES:
                            trafficSignalsVibrationValue = getResources().getString(R.string.dialogYes);
                            break;
                        default:
                            trafficSignalsVibrationValue = null;
                    }
                    if (trafficSignalsVibrationValue != null) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointTrafficSignalsVibration),
                                    trafficSignalsVibrationValue),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }

            } else if (pointWrapper.getPoint() instanceof POI) {
                POI poi = (POI) pointWrapper.getPoint();

                if (pointWrapper.getPoint() instanceof Station) {
                    Station station = (Station) pointWrapper.getPoint();
                    // vehicle list
                    if (! station.getVehicleList().isEmpty()) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                getResources().getString(R.string.labelPointStationAttributesHeading),
                                true, TEXTVIEW_NO_AUTO_LINK);
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointStationVehicleTypes),
                                    TextUtils.join(", ", station.getVehicleList())),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                    // lines
                    if (! station.getLineList().isEmpty()) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                getResources().getString(R.string.labelPointStationLinesHeading),
                                true, TEXTVIEW_NO_AUTO_LINK);
                        for (Line line : station.getLineList()) {
                            addTextView(
                                    TEXTVIEW_NO_ID,
                                    line.getDescription(),
                                    false, TEXTVIEW_NO_AUTO_LINK);
                        }
                    }
                }

                // poi building attributes
                if (poi.getOuterBuilding() != null
                        || ! poi.getEntranceList().isEmpty()) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            getResources().getString(R.string.labelPointPOIBuildingHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                    // outer building
                    if (poi.getOuterBuilding() != null) {
                        addTextView(
                                TEXTVIEW_OUTER_BUILDING_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIBuildingIsInside),
                                    poi.getOuterBuilding().toString()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                        // add click listener
                        TextView labelOuterBuilding = (TextView) layoutAttributes.findViewById(TEXTVIEW_OUTER_BUILDING_ID);
                        if (labelOuterBuilding != null) {
                            labelOuterBuilding.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                                    try {
                                        detailsIntent.putExtra(
                                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED,
                                                ((POI) pointWrapper.getPoint()).getOuterBuilding().toJson().toString());
                                    } catch (JSONException e) {
                                        detailsIntent.putExtra(Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                                    }
                                    startActivity(detailsIntent);
                                }
                            });
                        }
                    }
                    // number of entrances
                    if (! poi.getEntranceList().isEmpty()) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$d",
                                    getResources().getString(R.string.labelPointPOIBuildingNumberOfEntrances),
                                    poi.getEntranceList().size()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }

                // contact attributes
                if (! TextUtils.isEmpty(poi.getAddress())
                        || ! TextUtils.isEmpty(poi.getEmail())
                        || ! TextUtils.isEmpty(poi.getPhone())
                        || ! TextUtils.isEmpty(poi.getWebsite())
                        || ! TextUtils.isEmpty(poi.getOpeningHours())) {
                    // heading
                   	addTextView(
                            TEXTVIEW_NO_ID,
                			getResources().getString(R.string.labelPointPOIContactHeading),
                            true, TEXTVIEW_NO_AUTO_LINK);
                    // post address
                    if (! TextUtils.isEmpty(poi.getAddress())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIContactPostAddress),
                                    poi.getAddress()),
                                false, Linkify.MAP_ADDRESSES);
                    }
                    // email
                    if (! TextUtils.isEmpty(poi.getEmail())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIContactEMailAddress),
                                    poi.getEmail()),
                                false, Linkify.EMAIL_ADDRESSES);
                    }
                    // phone
                    if (! TextUtils.isEmpty(poi.getPhone())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIContactPhoneNumber),
                                    poi.getPhone()),
                                false, Linkify.PHONE_NUMBERS);
                    }
                    // website
                    if (! TextUtils.isEmpty(poi.getWebsite())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIContactWebsite),
                                    poi.getWebsite()),
                                false, Linkify.WEB_URLS);
                    }
                    // opening hours
                    if (! TextUtils.isEmpty(poi.getOpeningHours())) {
                        addTextView(
                                TEXTVIEW_NO_ID,
                                String.format(
                                    "%1$s: %2$s",
                                    getResources().getString(R.string.labelPointPOIContactOpeningHours),
                                    poi.getOpeningHours()),
                                false, TEXTVIEW_NO_AUTO_LINK);
                    }
                }
            }

            // accessibility attributes
            if (point.getTactilePaving() != -1
                    || point.getWheelchair() != -1) {
               	addTextView(
                        TEXTVIEW_NO_ID,
            			getResources().getString(R.string.labelPointAccessibilityHeading),
                        true, TEXTVIEW_NO_AUTO_LINK);
                // tactile paving
                String tactilePavingValue = null;
                switch (point.getTactilePaving()) {
                    case Point.TACTILE_PAVING.NO:
                        tactilePavingValue = getResources().getString(R.string.dialogNo);
                        break;
                    case Point.TACTILE_PAVING.YES:
                        tactilePavingValue = getResources().getString(R.string.dialogYes);
                        break;
                    default:
                        tactilePavingValue = null;
                }
                if (tactilePavingValue != null) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s",
                            	getResources().getString(R.string.labelPointTactilePaving),
                                tactilePavingValue),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
                // wheelchair
                String wheelchairValue = null;
                switch (point.getWheelchair()) {
                    case Point.WHEELCHAIR.NO:
                        wheelchairValue = getResources().getString(R.string.dialogNo);
                        break;
                    case Point.WHEELCHAIR.LIMITED:
                        wheelchairValue = getResources().getString(R.string.dialogLimited);
                        break;
                    case Point.WHEELCHAIR.YES:
                        wheelchairValue = getResources().getString(R.string.dialogYes);
                        break;
                    default:
                        wheelchairValue = null;
                }
                if (wheelchairValue != null) {
                    addTextView(
                            TEXTVIEW_NO_ID,
                            String.format(
                                "%1$s: %2$s",
                            	getResources().getString(R.string.labelPointWheelchair),
                                wheelchairValue),
                            false, TEXTVIEW_NO_AUTO_LINK);
                }
            }

            // coordinates
           	addTextView(
                    TEXTVIEW_NO_ID,
        			getResources().getString(R.string.labelPointCoordinatesHeading),
                    true, TEXTVIEW_NO_AUTO_LINK);
           	addTextView(
                    TEXTVIEW_NO_ID,
                    String.format(
                        "%1$s: %2$f",
                        getResources().getString(R.string.labelGPSLatitude),
                        point.getLatitude()),
                    false, TEXTVIEW_NO_AUTO_LINK);
           	addTextView(
                    TEXTVIEW_NO_ID,
                    String.format(
                        "%1$s: %2$f",
                        getResources().getString(R.string.labelGPSLongitude),
                        point.getLongitude()),
                    false, TEXTVIEW_NO_AUTO_LINK);
        }
    }

	@Override public void onFragmentDisabled() {
    }

    private void addTextView(int id, String text, boolean isHeading, int autoLink) {
    	TextView label = new TextView(getActivity());
        // set id
        if (id != TEXTVIEW_NO_ID) {
            label.setId(id);
        }
        // layout params
        LayoutParams lp = new LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isHeading && layoutAttributes.getChildCount() > 0) {
            ((MarginLayoutParams) lp).topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        }
    	label.setLayoutParams(lp);
        // text
        if (isHeading) {
            label.setText(StringUtility.boldAndRed(text));
        } else {
            label.setText(text);
        }
        // auto link param
        if (autoLink != TEXTVIEW_NO_AUTO_LINK) {
            label.setAutoLinkMask(autoLink);
        }
        // focusable
		label.setFocusable(true);
        // add to attributes layout
    	layoutAttributes.addView(label);
    }

}
