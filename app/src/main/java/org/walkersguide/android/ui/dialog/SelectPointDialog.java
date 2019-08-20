package org.walkersguide.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v4.app.DialogFragment;

import android.text.TextUtils;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.data.basic.wrapper.PointWrapper;
import org.walkersguide.android.data.profile.HistoryPointProfile;
import org.walkersguide.android.helper.PointUtility;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.sensor.PositionManager;
import org.walkersguide.android.server.AddressManager;
import org.walkersguide.android.server.AddressManager.AddressListener;
import org.walkersguide.android.ui.activity.PointDetailsActivity;
import org.walkersguide.android.ui.dialog.SimpleMessageDialog.ChildDialogCloseListener;
import org.walkersguide.android.ui.fragment.main.POIFragment;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;


public class SelectPointDialog extends DialogFragment implements ChildDialogCloseListener {

    private ChildDialogCloseListener childDialogCloseListener;
    private AccessDatabase accessDatabaseInstance;
    private PositionManager positionManagerInstance;
    private SettingsManager settingsManagerInstance;
    private int pointPutInto;
    private PointWrapper selectedPoint;
    private int[] selectFromArray;

    public static SelectPointDialog newInstance(int pointPutInto) {
        SelectPointDialog selectPointDialogInstance = new SelectPointDialog();
        Bundle args = new Bundle();
        args.putInt("pointPutInto", pointPutInto);
        selectPointDialogInstance.setArguments(args);
        return selectPointDialogInstance;
    }

    @Override public void onAttach(Context context){
        super.onAttach(context);
        if (getTargetFragment() != null
                && getTargetFragment() instanceof ChildDialogCloseListener) {
            childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
        }
        accessDatabaseInstance = AccessDatabase.getInstance(context);
        positionManagerInstance = PositionManager.getInstance(context);
        settingsManagerInstance = SettingsManager.getInstance(context);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        pointPutInto = getArguments().getInt("pointPutInto");
        selectedPoint = null;

        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                selectedPoint = settingsManagerInstance.getRouteSettings().getStartPoint();
                selectFromArray = Constants.PointSelectFromValueArray;
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                selectedPoint = settingsManagerInstance.getRouteSettings().getDestinationPoint();
                selectFromArray = Constants.PointSelectFromValueArray;
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                selectedPoint = settingsManagerInstance.getLocationSettings().getSimulatedLocation();
                selectFromArray = Constants.PointSelectFromValueArrayWithoutCurrentLocation;
                break;
            default:
                if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                    // via point
                    ArrayList<PointWrapper> viaPointList = settingsManagerInstance.getRouteSettings().getViaPointList();
                    int viaPointIndex = pointPutInto - Constants.POINT_PUT_INTO.VIA;
                    if (viaPointIndex >= 0 && viaPointIndex < viaPointList.size()) {
                        selectedPoint = viaPointList.get(viaPointIndex);
                    }
                    selectFromArray = Constants.PointSelectFromValueArrayWithoutCurrentLocation;
                }
                break;
        }

        String dialogTitle;
        switch (pointPutInto) {
            case Constants.POINT_PUT_INTO.START:
                if (selectedPoint == null) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameStart);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonStartPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            case Constants.POINT_PUT_INTO.DESTINATION:
                if (selectedPoint == null) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameDestination);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonDestinationPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            case Constants.POINT_PUT_INTO.SIMULATION:
                if (selectedPoint == null) {
                    dialogTitle = getResources().getString(R.string.selectPointDialogNameSimulation);
                } else {
                    dialogTitle = String.format(
                            "%1$s: %2$s",
                            getResources().getString(R.string.buttonSimulationPoint),
                            selectedPoint.getPoint().getName());
                }
                break;
            default:
                if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                    // via points
                    if (selectedPoint == null) {
                        dialogTitle = String.format(
                                getResources().getString(R.string.selectPointDialogNameVia),
                                (pointPutInto - Constants.POINT_PUT_INTO.VIA) + 1);
                    } else {
                        dialogTitle = String.format(
                                "%1$s %2$d: %3$s",
                                getResources().getString(R.string.buttonViaPoint),
                                (pointPutInto - Constants.POINT_PUT_INTO.VIA) + 1,
                                selectedPoint.getPoint().getName());
                    }
                } else {
                    dialogTitle = "";
                }
                break;
        }

        String[] formattedPointSelectFromArray = new String[selectFromArray.length];
        for (int i=0; i<selectFromArray.length; i++) {
            switch (selectFromArray[i]) {
                case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromCurrentLocation);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromEnterAddress);
                    break;
                case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromEnterCoordinates);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromHistoryPoints);
                    break;
                case Constants.POINT_SELECT_FROM.FROM_POI:
                    formattedPointSelectFromArray[i] = getResources().getString(R.string.pointSelectFromPOI);
                    break;
                default:
                    formattedPointSelectFromArray[i] = String.valueOf(selectFromArray[i]);
                    break;
            }
        }

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(dialogTitle)
            .setSingleChoiceItems(
                    formattedPointSelectFromArray,
                    -1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int pointSelectFromIndex = -1;
                            try {
                                pointSelectFromIndex = selectFromArray[which];
                            } catch (ArrayIndexOutOfBoundsException e) {
                                pointSelectFromIndex = -1;
                            } finally {
                                if (pointSelectFromIndex > -1) {
                                    executeAction(pointSelectFromIndex);
                                }
                            }
                        }
                    })
            .setPositiveButton(
                    getResources().getString(R.string.dialogDetails),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogRemove),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNegativeButton(
                    getResources().getString(R.string.dialogCancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setOnKeyListener(
                    new Dialog.OnKeyListener() {
                        @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                close();
                                return true;
                            }
                            return false;
                        }
                    })
            .create();
    }

    @Override public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog)getDialog();
        if(dialog != null) {
            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (selectedPoint != null) {
                buttonPositive.setVisibility(View.VISIBLE);
            } else {
                buttonPositive.setVisibility(View.GONE);
            }
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent detailsIntent = new Intent(getActivity(), PointDetailsActivity.class);
                    try {
                        detailsIntent.putExtra(
                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, selectedPoint.toJson().toString());
                    } catch (JSONException e) {
                        detailsIntent.putExtra(
                                Constants.POINT_DETAILS_ACTIVITY_EXTRA.JSON_POINT_SERIALIZED, "");
                    }
                    startActivity(detailsIntent);
                }
            });
            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (pointPutInto >= Constants.POINT_PUT_INTO.VIA) {
                buttonNeutral.setVisibility(View.VISIBLE);
            } else {
                buttonNeutral.setVisibility(View.GONE);
            }
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    settingsManagerInstance.getRouteSettings().removeViaPointAtIndex(
                            pointPutInto-Constants.POINT_PUT_INTO.VIA);
                    close();
                }
            });
            // negative button
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    close();
                }
            });
        }
    }

    @Override public void childDialogClosed() {
        close();
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        childDialogCloseListener = null;
    }

    private void executeAction(int pointSelectFromIndex) {
        switch (pointSelectFromIndex) {
            case Constants.POINT_SELECT_FROM.CURRENT_LOCATION:
                PointWrapper currentLocation = positionManagerInstance.getCurrentLocation();
                if (currentLocation == null) {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.errorNoLocationFound))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                } else {
                    PointUtility.putNewPoint(
                            getActivity(), currentLocation, pointPutInto);
                    close();
                }
                break;
            case Constants.POINT_SELECT_FROM.ENTER_ADDRESS:
                EnterAddressDialog enterAddressDialog = EnterAddressDialog.newInstance(pointPutInto);
                enterAddressDialog.setTargetFragment(SelectPointDialog.this, 1);
                enterAddressDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterAddressDialog");
                break;
            case Constants.POINT_SELECT_FROM.ENTER_COORDINATES:
                EnterCoordinatesDialog enterCoordinatesDialog = EnterCoordinatesDialog.newInstance(pointPutInto);
                enterCoordinatesDialog.setTargetFragment(SelectPointDialog.this, 1);
                enterCoordinatesDialog.show(
                        getActivity().getSupportFragmentManager(), "EnterCoordinatesDialog");
                break;
            case Constants.POINT_SELECT_FROM.FROM_HISTORY_POINTS:
                POIFragment selectHistoryPointDialog = POIFragment.newInstance(
                        POIFragment.ContentType.HISTORY_POINTS, pointPutInto);
                selectHistoryPointDialog.setTargetFragment(SelectPointDialog.this, 1);
                selectHistoryPointDialog.show(getActivity().getSupportFragmentManager(), "SelectHistoryPointDialog");
                break;
            case Constants.POINT_SELECT_FROM.FROM_POI:
                POIFragment selectPOIDialog = POIFragment.newInstance(
                        POIFragment.ContentType.POI, pointPutInto);
                selectPOIDialog.setTargetFragment(SelectPointDialog.this, 1);
                selectPOIDialog.show(getActivity().getSupportFragmentManager(), "SelectPOIDialog");
                break;
            default:
                break;
        }
    }

    private void close() {
        if (childDialogCloseListener != null) {
            childDialogCloseListener.childDialogClosed();
        }
        dismiss();
    }


    public static class EnterAddressDialog extends DialogFragment implements AddressListener {

        // Store instance variables
        private ChildDialogCloseListener childDialogCloseListener;
        private AccessDatabase accessDatabaseInstance;
        private PositionManager positionManagerInstance;
        private SettingsManager settingsManagerInstance;
        private InputMethodManager imm;
        private AddressManager addressManagerRequest;
        private int pointPutInto;
        private AutoCompleteTextView editAddress;
        private Switch buttonNearbyCurrentLocation;

        public static EnterAddressDialog newInstance(int pointPutInto) {
            EnterAddressDialog enterAddressDialogInstance = new EnterAddressDialog();
            Bundle args = new Bundle();
            args.putInt("pointPutInto", pointPutInto);
            enterAddressDialogInstance.setArguments(args);
            return enterAddressDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            accessDatabaseInstance = AccessDatabase.getInstance(context);
            positionManagerInstance = PositionManager.getInstance(context);
            settingsManagerInstance = SettingsManager.getInstance(context);
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            addressManagerRequest = null;
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointPutInto = getArguments().getInt("pointPutInto");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_enter_address, nullParent);

            editAddress = (AutoCompleteTextView) view.findViewById(R.id.editInput);
            editAddress.setHint(getResources().getString(R.string.editHintAddress));
            editAddress.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToGetCoordinatesForAddress();
                        return true;
                    }
                    return false;
                }
            });
            // add auto complete suggestions
            ArrayAdapter<String> searchTermHistoryAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManagerInstance.getSearchTermHistory().getSearchTermList());
            editAddress.setAdapter(searchTermHistoryAdapter);

            ImageButton buttonClearInput = (ImageButton) view.findViewById(R.id.buttonClearInput);
            buttonClearInput.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // clear edit text
                    editAddress.setText("");
                    // show keyboard
                    imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            buttonNearbyCurrentLocation = (Switch) view.findViewById(R.id.buttonNearbyCurrentLocation);
            if (savedInstanceState != null) {
                buttonNearbyCurrentLocation.setChecked(
                        savedInstanceState.getBoolean("nearbyCurrentLocationIsChecked"));
            } else {
                buttonNearbyCurrentLocation.setChecked(true);
            }

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.enterAddressDialogName))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        tryToGetCoordinatesForAddress();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
            // show keyboard
            new Handler().postDelayed(
                    new Runnable() {
                        @Override public void run() {
                            imm.showSoftInput(editAddress, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 50);
        }

        @Override public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putBoolean(
                    "nearbyCurrentLocationIsChecked", buttonNearbyCurrentLocation.isChecked());
        }

        private void tryToGetCoordinatesForAddress() {
            String address = editAddress.getText().toString().trim();
            if (address.equals("")) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.messageAddressMissing),
                        Toast.LENGTH_LONG).show();
            } else {
                addressManagerRequest = new AddressManager(
                        getActivity(),
                        EnterAddressDialog.this,
                        address,
                        buttonNearbyCurrentLocation.isChecked());
                addressManagerRequest.execute();
            }
        }

        @Override public void addressRequestFinished(Context context, int returnCode, PointWrapper addressPoint) {
            if (returnCode == Constants.RC.OK
                    && addressPoint != null) {
                // add to search history
                settingsManagerInstance.getSearchTermHistory().addSearchTerm(
                        editAddress.getText().toString().trim());
                // put into
                PointUtility.putNewPoint(context, addressPoint, pointPutInto);
                // reload ui
                if (childDialogCloseListener != null) {
                    childDialogCloseListener.childDialogClosed();
                }
                dismiss();
            } else {
                if (isAdded()) {
                    SimpleMessageDialog.newInstance(
                            ServerUtility.getErrorMessageForReturnCode(context, returnCode))
                        .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
            if (addressManagerRequest != null
                    && addressManagerRequest.getStatus() != AsyncTask.Status.FINISHED) {
                addressManagerRequest.cancel();
            }
        }
    }


    public static class EnterCoordinatesDialog extends DialogFragment {

        // Store instance variables
        private AccessDatabase accessDatabaseInstance;
        private ChildDialogCloseListener childDialogCloseListener;
        private int pointPutInto;
        private EditText editLatitude, editLongitude, editName;

        public static EnterCoordinatesDialog newInstance(int pointPutInto) {
            EnterCoordinatesDialog enterCoordinatesDialogInstance = new EnterCoordinatesDialog();
            Bundle args = new Bundle();
            args.putInt("pointPutInto", pointPutInto);
            enterCoordinatesDialogInstance.setArguments(args);
            return enterCoordinatesDialogInstance;
        }

        @Override public void onAttach(Context context){
            super.onAttach(context);
            if (getTargetFragment() != null
                    && getTargetFragment() instanceof ChildDialogCloseListener) {
                childDialogCloseListener = (ChildDialogCloseListener) getTargetFragment();
            }
            accessDatabaseInstance = AccessDatabase.getInstance(context);
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            pointPutInto = getArguments().getInt("pointPutInto");

            // custom view
            final ViewGroup nullParent = null;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_enter_coordinates, nullParent);

            editLatitude = (EditText) view.findViewById(R.id.editLatitude);
            editLongitude = (EditText) view.findViewById(R.id.editLongitude);
            editName = (EditText) view.findViewById(R.id.editName);
            editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        tryToGetAddressForCoordinates();
                        return true;
                    }
                    return false;
                }
            });

            // create dialog
            return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.enterCoordinatesDialogName))
                .setView(view)
                .setPositiveButton(
                        getResources().getString(R.string.dialogOK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.dialogCancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        }

        @Override public void onStart() {
            super.onStart();
            final AlertDialog dialog = (AlertDialog)getDialog();
            if(dialog != null) {
                // positive button
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        tryToGetAddressForCoordinates();
                    }
                });
                // negative button
                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }

        private void tryToGetAddressForCoordinates() {
            // latitude
            double latitude = 1000000.0;
            try {
                latitude = Double.valueOf(editLatitude.getText().toString());
            } catch (NumberFormatException e) {
            } finally {
                if (latitude <= -180.0 || latitude > 180.0) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageLatitudeMissing),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // longitude
            double longitude = 1000000.0;
            try {
                longitude = Double.valueOf(editLongitude.getText().toString());
            } catch (NumberFormatException e) {
            } finally {
                if (longitude <= -180.0 || longitude > 180.0) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(R.string.messageLongitudeMissing),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // optional point name
            String name = editName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                name = String.format("%1$f, %2$f", latitude, longitude);
            }

            // create point
            PointWrapper createdPoint = null;
            try {
                JSONObject jsonCreatedPoint = new JSONObject();
                jsonCreatedPoint.put("name", name);
                jsonCreatedPoint.put("type", Constants.POINT.GPS);
                jsonCreatedPoint.put("sub_type", getResources().getString(R.string.currentLocationName));
                jsonCreatedPoint.put("lat", latitude);
                jsonCreatedPoint.put("lon", longitude);
                jsonCreatedPoint.put("time", System.currentTimeMillis());
                createdPoint = new PointWrapper(getActivity(), jsonCreatedPoint);
            } catch (JSONException e) {
                createdPoint = null;
            } finally {
                if (createdPoint != null) {
                    // put into
                    PointUtility.putNewPoint(getActivity(), createdPoint, pointPutInto);
                    // add to user created point history
                    accessDatabaseInstance.addFavoritePointToProfile(
                            createdPoint, HistoryPointProfile.ID_USER_CREATED_POINTS);
                    // reload ui
                    if (childDialogCloseListener != null) {
                        childDialogCloseListener.childDialogClosed();
                    }
                    dismiss();
                } else {
                    SimpleMessageDialog.newInstance(
                            getResources().getString(R.string.messageCantCreatePointFromCoordinates))
                    .show(getActivity().getSupportFragmentManager(), "SimpleMessageDialog");
                }
            }
        }

        @Override public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            childDialogCloseListener = null;
        }
    }

}
