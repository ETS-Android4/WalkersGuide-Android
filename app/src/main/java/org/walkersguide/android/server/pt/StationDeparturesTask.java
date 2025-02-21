package org.walkersguide.android.server.pt;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.NetworkId;
import de.schildbach.pte.AbstractNetworkProvider;


import org.walkersguide.android.server.ServerTask;
import org.walkersguide.android.server.ServerTaskExecutor;
import timber.log.Timber;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import org.walkersguide.android.server.ServerUtility;
import org.walkersguide.android.server.ServerException;


public class StationDeparturesTask extends ServerTask {

    private NetworkId networkId;
    private Location station;
    private Date initialDepartureDate;

    public StationDeparturesTask(NetworkId networkId, Location station, Date initialDepartureDate) {
        this.networkId = networkId;
        this.station = station;
        this.initialDepartureDate = initialDepartureDate;
    }

    @Override public void execute() throws PtException {
        ArrayList<Departure> departureList = new ArrayList<Departure>();

        AbstractNetworkProvider provider = PtUtility.findNetworkProvider(this.networkId);
        if (provider == null) {
            throw new PtException(PtException.RC_NO_NETWORK_PROVIDER);
        } else if (this.station == null) {
            throw new PtException(PtException.RC_NO_STATION);
        } else if (! ServerUtility.isInternetAvailable()) {
            throw new PtException( ServerException.RC_NO_INTERNET_CONNECTION);
        }

        int numberOfRequests = 0, maxNumberOfRequests = 5, maxNumberOfDepartures = 50;
        Date nextDepartureDate = this.initialDepartureDate;
        Date maxDepartureDate = new Date(
                this.initialDepartureDate.getTime() + 3*60*60*1000);
        QueryDeparturesResult departuresResult = null;
        while (numberOfRequests < maxNumberOfRequests
                && departureList.size() < maxNumberOfDepartures
                && nextDepartureDate.before(maxDepartureDate)) {

            // query departures
            try {
                Timber.d("request: stationId=%1$s, date=%2$s", this.station.id, nextDepartureDate.toString());
                departuresResult = provider.queryDepartures(
                        this.station.id, nextDepartureDate, 100, false);
            } catch (IOException e) {
                Timber.e("DepartureManager query error: %1$s", e.getMessage());
                departuresResult = null;
            } finally {
                if (departuresResult == null) {
                    break;
                }
                Timber.d("result: %1$s, numberOfRequests: %2$d", departuresResult.status, numberOfRequests);
            }

            // parse departures
            for (StationDepartures stationDepartures  : departuresResult.stationDepartures) {
                for (Departure departure : stationDepartures.departures) {
                    if (! departureList.contains(departure)) {
                        departureList.add(departure);
                    }
                }
            }

            // update next departure date
            if (! departureList.isEmpty()) {
                nextDepartureDate = PtUtility.getDepartureTime(
                        departureList.get(departureList.size()-1));
            }
            // increment request counter
            numberOfRequests += 1;
        }

        // post-processing
        if (! isCancelled()) {
            if (departureList.isEmpty()) {
                if (departuresResult == null) {
                    throw new PtException(PtException.RC_REQUEST_FAILED);
                } else if (departuresResult.status == QueryDeparturesResult.Status.SERVICE_DOWN) {
                    throw new PtException(PtException.RC_SERVICE_DOWN);
                } else if (departuresResult.status == QueryDeparturesResult.Status.INVALID_STATION) {
                    throw new PtException(PtException.RC_INVALID_STATION);
                } else if (departuresResult.status == QueryDeparturesResult.Status.OK) {
                    throw new PtException(PtException.RC_NO_DEPARTURES);
                } else {
                    throw new PtException(PtException.RC_UNKNOWN_SERVER_RESPONSE);
                }
            }

            ServerTaskExecutor.sendStationDeparturesTaskSuccessfulBroadcast(getId(), departureList);
        }
    }

}
