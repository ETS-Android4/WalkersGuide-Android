package org.walkersguide.android.ui.dialog.edit;

import org.walkersguide.android.server.wg.p2p.WayClassWeightSettings;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassType;
import org.walkersguide.android.server.wg.p2p.wayclass.WayClassWeight;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.R;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.Spinner;
import java.util.Arrays;
import android.widget.ArrayAdapter;
import org.walkersguide.android.ui.view.builder.TextViewBuilder;
import android.widget.TableRow;
import android.widget.AdapterView;


public class ConfigureWayClassWeightsDialog extends DialogFragment
        implements AdapterView.OnItemSelectedListener {
    public static final String REQUEST_WAY_CLASS_WEIGHTS_CHANGED = "wayClassWeightsChanged";
    public static final String EXTRA_WAY_CLASS_SETTINGS = "wayClassWeightSettings";


    // instance constructors

    public static ConfigureWayClassWeightsDialog newInstance(WayClassWeightSettings wayClassWeightSettings) {
        ConfigureWayClassWeightsDialog dialog = new ConfigureWayClassWeightsDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_WAY_CLASS_SETTINGS, wayClassWeightSettings);
        dialog.setArguments(args);
        return dialog;
    }


    // dialog
    private static final String KEY_WAY_CLASS_SETTINGS = "wayClassWeightSettings";

    private WayClassWeightSettings wayClassWeightSettings;
    private TableLayout tableLayout;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            wayClassWeightSettings = (WayClassWeightSettings) savedInstanceState.getSerializable(KEY_WAY_CLASS_SETTINGS);
        } else {
            wayClassWeightSettings = (WayClassWeightSettings) getArguments().getSerializable(KEY_WAY_CLASS_SETTINGS);
        }

        tableLayout = new TableLayout(
                ConfigureWayClassWeightsDialog.this.getContext());
        tableLayout.setLayoutParams(
                new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        tableLayout.setColumnStretchable(1, true);

        ScrollView scrollView = new ScrollView(
                ConfigureWayClassWeightsDialog.this.getContext());
        scrollView.setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        scrollView.addView(tableLayout);

        // create dialog
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.configureWayClassWeightsDialogTitle))
            .setView(scrollView)
            .setPositiveButton(
                    getResources().getString(R.string.dialogOK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
            .setNeutralButton(
                    getResources().getString(R.string.dialogDefault),
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
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {

            // positive button
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Bundle result = new Bundle();
                    result.putSerializable(EXTRA_WAY_CLASS_SETTINGS, wayClassWeightSettings);
                    getParentFragmentManager().setFragmentResult(REQUEST_WAY_CLASS_WEIGHTS_CHANGED, result);
                    dialog.dismiss();
                }
            });

            // neutral button
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    wayClassWeightSettings = WayClassWeightSettings.getDefault();
                    populateTableLayout();
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

        populateTableLayout();
    }

    private void populateTableLayout() {
        final TableRow.LayoutParams lpTableRowChild = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        tableLayout.removeAllViews();

        for (WayClassType type : WayClassType.values()) {

            Spinner spinner = new Spinner(
                    ConfigureWayClassWeightsDialog.this.getContext(),
                    Spinner.MODE_DIALOG);
            spinner.setId(type.ordinal());
            spinner.setLayoutParams(lpTableRowChild);
            spinner.setTag(type);
            spinner.setPrompt(type.name);
            spinner.setOnItemSelectedListener(this);

            ArrayList<WayClassWeight> wayClassWeightList =
                new ArrayList<WayClassWeight>(
                        Arrays.asList(WayClassWeight.values()));
            // create weight adapter
            ArrayAdapter<WayClassWeight> wayClassWeightAdapter = new ArrayAdapter<WayClassWeight>(
                    ConfigureWayClassWeightsDialog.this.getContext(),
                    android.R.layout.select_dialog_singlechoice,
                    wayClassWeightList);
            wayClassWeightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // set adapter
            spinner.setAdapter(wayClassWeightAdapter);
            // set weight selection
            spinner.setSelection(
                    wayClassWeightList.indexOf(
                        wayClassWeightSettings.getWeightFor(type)));

            TextView label = new TextViewBuilder(
                    ConfigureWayClassWeightsDialog.this.getContext(),
                    type.name,
                    lpTableRowChild)
                .setId(WayClassType.values().length + spinner.getId() + 1)
                .isLabelFor(spinner.getId())
                .centerTextVertically()
                .create();

            TableRow tableRow = new TableRow(
                    ConfigureWayClassWeightsDialog.this.getContext());
            tableRow.setLayoutParams(
                    new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.addView(label);
            tableRow.addView(spinner);

            tableLayout.addView(tableRow);
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_WAY_CLASS_SETTINGS, wayClassWeightSettings);
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        wayClassWeightSettings.setWeightFor(
                (WayClassType) parent.getTag(),
                (WayClassWeight) parent.getItemAtPosition(position));
    }

    @Override public void onNothingSelected(AdapterView parent) {
    }
}
