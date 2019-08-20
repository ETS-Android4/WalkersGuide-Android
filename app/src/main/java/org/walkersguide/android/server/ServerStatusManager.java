package org.walkersguide.android.server;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;

import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import org.walkersguide.android.data.server.ServerInstance;
import org.walkersguide.android.exception.ServerCommunicationException;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.util.Constants;
import org.walkersguide.android.util.SettingsManager;
import org.walkersguide.android.util.SettingsManager.ServerSettings;

import timber.log.Timber;


public class ServerStatusManager {

    private Context context;
    private static ServerStatusManager serverStatusManagerInstance;
    private SettingsManager settingsManagerInstance;

    public static ServerStatusManager getInstance(Context context) {
        if(serverStatusManagerInstance == null){
            serverStatusManagerInstance = new ServerStatusManager(context.getApplicationContext());
        }
        return serverStatusManagerInstance;
    }

    private ServerStatusManager(Context context) {
        this.context = context;
        this.settingsManagerInstance = SettingsManager.getInstance(context);
    }


    /**
     * request server status object
     */
    public interface ServerStatusListener {
        public void serverStatusRequestFinished(Context context, int returnCode, ServerInstance serverInstance);
    }

    private RequestServerStatus requestServerStatus = null;
    private ServerInstance cachedServerInstance = null;


    public void requestServerStatus(ServerStatusListener profileListener, String serverURL) {
        if (serverStatusRequestInProgress()) {
            if (profileListener == null) {
                return;
            } else if (this.requestServerStatus.getServerURL().equals(serverURL)) {
                this.requestServerStatus.addListener(profileListener);
                return;
            } else {
                cancelServerStatusRequest();
            }
        }
        this.requestServerStatus = new RequestServerStatus(profileListener, serverURL);
        this.requestServerStatus.execute();
    }

    public void invalidateServerStatusRequest(ServerStatusListener profileListener) {
        if (serverStatusRequestInProgress()) {
            this.requestServerStatus.removeListener(profileListener);
        }
    }

    public boolean serverStatusRequestInProgress() {
        if (this.requestServerStatus != null
                && this.requestServerStatus.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelServerStatusRequest() {
        if (serverStatusRequestInProgress()) {
            this.requestServerStatus.cancel();
        }
        this.requestServerStatus = null;
    }

    public ServerInstance getCachedServerInstance() {
        return this.cachedServerInstance;
    }

    public void setCachedServerInstance(ServerInstance newServerInstance) {
        this.cachedServerInstance = newServerInstance;
    }


    public class RequestServerStatus extends AsyncTask<Void, Void, ServerInstance> {

        private ArrayList<ServerStatusListener> serverStatusListenerList;
        private String serverURL;
        private int returnCode;

        public RequestServerStatus(ServerStatusListener serverStatusListener, String serverURL) {
            this.serverStatusListenerList = new ArrayList<ServerStatusListener>();
            if (serverStatusListener != null) {
                this.serverStatusListenerList.add(serverStatusListener);
            }
            this.serverURL = serverURL;
            this.returnCode = Constants.RC.OK;
        }

        @Override protected ServerInstance doInBackground(Void... params) {
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerUtility.getServerInstance(context, serverURL);
            } catch (ServerCommunicationException e) {
                this.returnCode = e.getReturnCode();
            } finally {
                if (returnCode != Constants.RC.OK) {
                    return null;
                }
            }
            return serverInstance;
        }

        @Override protected void onPostExecute(ServerInstance serverInstance) {
            Timber.d("ServerStatus: %1$d", this.returnCode);
            for (ServerStatusListener serverStatusListener : this.serverStatusListenerList) {
                serverStatusListener.serverStatusRequestFinished(context, returnCode, serverInstance);
            }
        }

        @Override protected void onCancelled(ServerInstance serverInstance) {
            for (ServerStatusListener serverStatusListener : this.serverStatusListenerList) {
                serverStatusListener.serverStatusRequestFinished(context, Constants.RC.CANCELLED, null);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public String getServerURL() {
            return this.serverURL;
        }

        public void addListener(ServerStatusListener newServerStatusListener) {
            if (newServerStatusListener != null
                    && ! this.serverStatusListenerList.contains(newServerStatusListener)) {
                this.serverStatusListenerList.add(newServerStatusListener);
            }
        }

        public void removeListener(ServerStatusListener newServerStatusListener) {
            if (newServerStatusListener != null
                    && this.serverStatusListenerList.contains(newServerStatusListener)) {
                this.serverStatusListenerList.remove(newServerStatusListener);
            }
        }
    }


    /**
     * cancel running request
     */
    private CancelRequest cancelRequest = null;

    public void cancelRunningRequestOnServer() {
        if (this.cancelRequest != null
                && this.cancelRequest.getStatus() != AsyncTask.Status.FINISHED) {
            this.cancelRequest.cancel();
        }
        this.cancelRequest = new CancelRequest();
        this.cancelRequest.execute();
    }


    public class CancelRequest extends AsyncTask<Void, Void, Void> {

        private HttpsURLConnection connection;
        private Handler cancelConnectionHandler;
        private CancelConnection cancelConnection;

        public CancelRequest() {
            this.connection = null;
            this.cancelConnectionHandler = new Handler();
            this.cancelConnection = new CancelConnection();
        }

        @Override protected Void doInBackground(Void... params) {
            ServerSettings serverSettings = SettingsManager.getInstance(context).getServerSettings();
            // server instance
            ServerInstance serverInstance = null;
            try {
                serverInstance = ServerUtility.getServerInstance(
                        context, serverSettings.getServerURL());
            } catch (ServerCommunicationException e) {
                serverInstance = null;
            } finally {
                if (serverInstance == null) {
                    return null;
                }
            }
            // create server param list
            JSONObject jsonServerParams = null;
            try {
                jsonServerParams = ServerUtility.createServerParamList(context);
            } catch (JSONException e) {
                jsonServerParams = new JSONObject();
            }
            // start request
            try {
                connection = ServerUtility.getHttpsURLConnectionObject(
                        context,
                        String.format(
                            "%1$s/%2$s", serverInstance.getServerURL(), Constants.SERVER_COMMAND.CANCEL_REQUEST),
                        jsonServerParams);
                cancelConnectionHandler.postDelayed(cancelConnection, 100);
                connection.connect();
                int responseCode = connection.getResponseCode();
                cancelConnectionHandler.removeCallbacks(cancelConnection);
            } catch (IOException e) {
            } catch (ServerCommunicationException e) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        public void cancel() {
            this.cancel(true);
        }

        private class CancelConnection implements Runnable {
            public void run() {
                if (isCancelled()) {
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception e) {}
                    }
                    return;
                }
                cancelConnectionHandler.postDelayed(this, 100);
            }
        }
    }

}
