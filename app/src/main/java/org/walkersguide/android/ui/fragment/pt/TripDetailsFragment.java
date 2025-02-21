package org.walkersguide.android.ui.fragment.pt;

import org.walkersguide.android.ui.activity.toolbar.FragmentContainerActivity;
import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Stop;
import java.util.ListIterator;
import android.os.Looper;
import androidx.core.view.ViewCompat;
import android.content.Context;

import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.walkersguide.android.R;
import org.walkersguide.android.server.pt.PtException;
import org.walkersguide.android.server.pt.PtUtility;
import java.util.Date;
import android.widget.AdapterView;
import androidx.fragment.app.Fragment;
import org.walkersguide.android.util.SettingsManager;
import android.os.Handler;
import android.os.Looper;
import org.walkersguide.android.server.ServerTaskExecutor;
import org.walkersguide.android.server.pt.TripDetailsTask;
import org.walkersguide.android.server.pt.PtException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.Intent;
import org.walkersguide.android.util.GlobalInstance;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import timber.log.Timber;
import org.walkersguide.android.util.Helper;


public class TripDetailsFragment extends Fragment implements OnRefreshListener, Runnable {


    // instance constructors

	public static TripDetailsFragment newInstance(Location station, Departure departure) {
		TripDetailsFragment fragment = new TripDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_STATION, station);
        args.putSerializable(KEY_DEPARTURE, departure);
        fragment.setArguments(args);
		return fragment;
	}


    // fragment
    private static final String KEY_CACHED_STOP_LIST = "cachedStopList";
    private static final String KEY_TASK_ID = "taskId";
    private static final String KEY_STATION = "station";
    private static final String KEY_DEPARTURE = "departure";
    private static final String KEY_LIST_POSITION = "listPosition";

    private ServerTaskExecutor serverTaskExecutorInstance;
    private long taskId;
	private Handler nextDeparturesHandler = new Handler(Looper.getMainLooper());

    private ArrayList<Stop> cachedStopList;
    private Location station;
    private Departure departure;
    private int listPosition;

	// ui components
    private SwipeRefreshLayout swipeRefreshListView, swipeRefreshEmptyTextView;
    private ListView listViewTrip;
    private TextView labelHeading, labelEmptyListView;

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverTaskExecutorInstance = ServerTaskExecutor.getInstance();
    }


    /**
     * menu
     */

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_trip_details_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItemRefresh = menu.findItem(R.id.menuItemRefresh);
        if (serverTaskExecutorInstance.taskInProgress(taskId)) {
            menuItemRefresh.setTitle(
                    getResources().getString(R.string.menuItemCancel));
        } else {
            menuItemRefresh.setTitle(
                    getResources().getString(R.string.menuItemRefresh));
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemRefresh) {
            if (serverTaskExecutorInstance.taskInProgress(taskId)) {
                serverTaskExecutorInstance.cancelTask(taskId);
            } else if (station != null && departure != null) {
                cachedStopList = null;
                listPosition = 0;
                prepareRequest();
            }

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
		return inflater.inflate(R.layout.layout_heading_and_list_view, container, false);
	}

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        station = (Location) getArguments().getSerializable(KEY_STATION);
        departure = (Departure) getArguments().getSerializable(KEY_DEPARTURE);
        if (savedInstanceState != null) {
            cachedStopList = (ArrayList<Stop>) savedInstanceState.getSerializable(KEY_CACHED_STOP_LIST);
            taskId = savedInstanceState.getLong(KEY_TASK_ID);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        } else {
            cachedStopList = null;
            taskId = ServerTaskExecutor.NO_TASK_ID;
            listPosition = 0;
        }

        labelHeading = (TextView) view.findViewById(R.id.labelHeading);
        labelHeading.setVisibility(View.GONE);

        swipeRefreshListView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshListView);
        swipeRefreshListView.setOnRefreshListener(this);

        listViewTrip = (ListView) view.findViewById(R.id.listView);
        listViewTrip.setVisibility(View.GONE);
        listViewTrip.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Stop selectedStop = (Stop) parent.getItemAtPosition(position);
                if (selectedStop != null) {
                    FragmentContainerActivity.showDepartures(
                            TripDetailsFragment.this.getContext(),
                            selectedStop.location,
                            PtUtility.getDepartureTime(selectedStop));
                }
            }
        });

        swipeRefreshEmptyTextView = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshEmptyTextView);
        swipeRefreshEmptyTextView.setOnRefreshListener(this);
        listViewTrip.setEmptyView(swipeRefreshEmptyTextView);
        labelEmptyListView = (TextView) view.findViewById(R.id.labelEmptyListView);
        labelEmptyListView.setVisibility(View.GONE);

        if (station != null && departure != null) {
            labelHeading.setVisibility(View.VISIBLE);
            listViewTrip.setVisibility(View.VISIBLE);
            labelEmptyListView.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onRefresh() {
        if (! serverTaskExecutorInstance.taskInProgress(taskId)
                && (station != null && departure != null)) {
            Helper.vibrateOnce(
                    Helper.VIBRATION_DURATION_SHORT, Helper.VIBRATION_INTENSITY_WEAK);
            cachedStopList = null;
            listPosition = 0;
            prepareRequest();
        }
    }

    @Override public void onResume() {
        super.onResume();
        // prepare request
        if (cachedStopList != null) {
            tripTaskWasSuccessful(cachedStopList);
        } else if (station != null && departure != null) {
            prepareRequest();
        }

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED);
        localIntentFilter.addAction(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(localIntentReceiver, localIntentFilter);
    }

    @Override public void onPause() {
        super.onPause();
        if (station != null && departure != null) {
            nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(localIntentReceiver);
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(KEY_CACHED_STOP_LIST, cachedStopList);
        savedInstanceState.putLong(KEY_TASK_ID, taskId);
        savedInstanceState.putInt(KEY_LIST_POSITION, listPosition);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (! getActivity().isChangingConfigurations()) {
            serverTaskExecutorInstance.cancelTask(taskId);
        }
    }


    private void prepareRequest() {
        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelHeading.setText(
                getResources().getQuantityString(R.plurals.station, 0, 0));

        // list view
        listViewTrip.setAdapter(null);
        listViewTrip.setOnScrollListener(null);
        ViewCompat.setAccessibilityLiveRegion(
                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
        labelEmptyListView.setText(
                getResources().getString(R.string.messagePleaseWait));

        nextDeparturesHandler.removeCallbacks(TripDetailsFragment.this);
        if (! serverTaskExecutorInstance.taskInProgress(taskId)) {
            swipeRefreshListView.setRefreshing(true);
            swipeRefreshEmptyTextView.setRefreshing(true);

            taskId = serverTaskExecutorInstance.executeTask(
                    new TripDetailsTask(
                        SettingsManager.getInstance().getSelectedNetworkId(),
                        station, departure));
        }
    }

    // background task results

    private BroadcastReceiver localIntentReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)
                    || intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                if (taskId != intent.getLongExtra(ServerTaskExecutor.EXTRA_TASK_ID, ServerTaskExecutor.INVALID_TASK_ID)) {
                    return;
                }

                if (intent.getAction().equals(ServerTaskExecutor.ACTION_TRIP_DETAILS_TASK_SUCCESSFUL)) {
                    tripTaskWasSuccessful(
                            (ArrayList<Stop>) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_STOP_LIST));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_CANCELLED)) {
                    Timber.d("cancelled");
                    labelEmptyListView.setText(
                            GlobalInstance.getStringResource(R.string.errorReqRequestCancelled));

                } else if (intent.getAction().equals(ServerTaskExecutor.ACTION_SERVER_TASK_FAILED)) {
                    PtException ptException = (PtException) intent.getSerializableExtra(ServerTaskExecutor.EXTRA_EXCEPTION);
                    if (ptException != null) {
                        ViewCompat.setAccessibilityLiveRegion(
                                labelEmptyListView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
                        labelEmptyListView.setText(ptException.getMessage());
                    }
                }

                swipeRefreshListView.setRefreshing(false);
                swipeRefreshEmptyTextView.setRefreshing(false);
            }
        }
    };


    /**
     * trip details
     */

    private void tripTaskWasSuccessful(ArrayList<Stop> stopList) {
        this.cachedStopList = stopList;

        // listview
        listViewTrip.setAdapter(
                new TripAdapter(TripDetailsFragment.this.getContext(), stopList));
        labelEmptyListView.setText(
                getResources().getString(R.string.labelNoMoreStops));

        // heading
        ViewCompat.setAccessibilityLiveRegion(
                labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        labelHeading.setText(
                getResources().getQuantityString(
                    R.plurals.station, stopList.size(), stopList.size()));

        // list position
        listViewTrip.setSelection(listPosition);
        listViewTrip.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listPosition != firstVisibleItem) {
                    listPosition = firstVisibleItem;
                }
            }
        });

        // start relative departure time updates
        nextDeparturesHandler.postDelayed(TripDetailsFragment.this, 60000);
    }

    // update trip every 60 seconds

    @Override public void run() {
        TripAdapter tripAdapter = (TripAdapter) listViewTrip.getAdapter();
        if (tripAdapter != null) {
            tripAdapter.notifyDataSetChanged();

            // update heading
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
            labelHeading.setText(
                    getResources().getQuantityString(
                        R.plurals.station, tripAdapter.getCount(), tripAdapter.getCount()));
            ViewCompat.setAccessibilityLiveRegion(
                    labelHeading, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

            // plan next update
            if (tripAdapter.getCount() > 0) {
                nextDeparturesHandler.postDelayed(TripDetailsFragment.this, 60000);
            }
        }
    }


    private static class TripAdapter extends ArrayAdapter<Stop> {

        private Context context;
        private ArrayList<Stop> stopList;

        public TripAdapter(Context context, ArrayList<Stop> stopList) {
            super(context, R.layout.layout_single_text_view);
            this.context = context;
            this.stopList = stopList;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Stop stop = getItem(position);

            // load item layout
            EntryHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.layout_single_text_view, parent, false);
                holder = new EntryHolder();
                holder.label = (TextView) convertView.findViewById(R.id.label);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }

            holder.label.setText(
                    String.format(
                        context.getResources().getString(R.string.labelTripAdapter),
                        PtUtility.getLocationName(stop.location),
                        PtUtility.formatRelativeDepartureTime(
                            context, PtUtility.getDepartureTime(stop), false),
                        PtUtility.formatAbsoluteDepartureTime(
                            context, PtUtility.getDepartureTime(stop)))
                    );

            holder.label.setContentDescription(
                    String.format(
                        context.getResources().getString(R.string.labelTripAdapterCD),
                        PtUtility.getLocationName(stop.location),
                        PtUtility.formatRelativeDepartureTime(
                            context, PtUtility.getDepartureTime(stop), true),
                        PtUtility.formatAbsoluteDepartureTime(
                            context, PtUtility.getDepartureTime(stop)))
                    );

            return convertView;
        }

        @Override public int getCount() {
            if (stopList != null) {
                return stopList.size();
            }
            return 0;
        }

        @Override public Stop getItem(int position) {
            return stopList.get(position);
        }

        @Override public void notifyDataSetChanged() {
            ListIterator<Stop> stopListIterator = this.stopList.listIterator();
            while(stopListIterator.hasNext()){
                Stop stop = stopListIterator.next();
                if (PtUtility.getDepartureTime(stop).before(new Date())) {
                    stopListIterator.remove();
                }
            }
            super.notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView label;
        }
    }

}
