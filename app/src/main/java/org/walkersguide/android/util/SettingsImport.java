package org.walkersguide.android.util;

import android.content.Context;

import android.database.SQLException;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.walkersguide.android.database.AccessDatabase;
import org.walkersguide.android.database.SQLiteHelper;
import org.walkersguide.android.helper.ServerUtility;
import org.walkersguide.android.R;
import org.walkersguide.android.util.Constants;


public class SettingsImport extends AsyncTask<Void, Void, Integer> {

    public interface SettingsImportListener {
	    public void settingsImportFinished(int returnCode, String returnMessage);
    }


    private Context context;
    private SettingsImportListener settingsImportListener;
    private File databaseFileToImport;

    public SettingsImport(Context context, SettingsImportListener settingsImportListener, File databaseFileToImport) {
        this.context = context;
        this.settingsImportListener = settingsImportListener;
        this.databaseFileToImport = databaseFileToImport;
    }

    @Override protected Integer doInBackground(Void... params) {
        // open database file
        InputStream in = null;
        try {
            in = new FileInputStream(this.databaseFileToImport);
        } catch(IOException e) {
            return Constants.RC.DATABASE_IMPORT_FAILED;
        }

        // create temp database file
        File oldDatabaseFile = context.getDatabasePath(SQLiteHelper.INTERNAL_DATABASE_NAME);
        File tempDatabaseFile = context.getDatabasePath(SQLiteHelper.INTERNAL_TEMP_DATABASE_NAME);
        if (tempDatabaseFile.exists()) {
            tempDatabaseFile.delete();
        } else {
            tempDatabaseFile.getParentFile().mkdirs();
        }

        // copy
        int returnCode = Constants.RC.OK;
        OutputStream out = null;
        byte[] buffer = new byte[1024];
        int length;
        try {
            out = new FileOutputStream(tempDatabaseFile);
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                if (isCancelled()) {
                    returnCode = Constants.RC.CANCELLED;
                    break;
                }
            }
        } catch(IOException e) {
            returnCode = Constants.RC.DATABASE_IMPORT_FAILED;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {}
            }
        }

        if (returnCode == Constants.RC.OK) {
            // replace database
            if (oldDatabaseFile.exists()) {
                oldDatabaseFile.delete();
            }
            tempDatabaseFile.renameTo(
                    context.getDatabasePath(SQLiteHelper.INTERNAL_DATABASE_NAME));
            // re-open database
            try {
                AccessDatabase.getInstance(this.context).reOpen();
            } catch (SQLException e) {
                returnCode = Constants.RC.DATABASE_IMPORT_FAILED;
            }
        } else {
            // remove temp file
            if (tempDatabaseFile.exists()) {
                tempDatabaseFile.delete();
            }
        }

        return returnCode;
    }

    @Override protected void onPostExecute(Integer returnCode) {
        String returnMessage = context.getResources().getString(R.string.labelImportDatabaseSuccessful);
        if (returnCode != Constants.RC.OK) {
            returnMessage = ServerUtility.getErrorMessageForReturnCode(this.context, returnCode);
        }
        if (settingsImportListener != null) {
        	settingsImportListener.settingsImportFinished(returnCode, returnMessage);
        }
    }

    @Override protected void onCancelled(Integer empty) {
        int returnCode = Constants.RC.CANCELLED;
        String returnMessage = ServerUtility.getErrorMessageForReturnCode(this.context, returnCode);
        if (settingsImportListener != null) {
        	settingsImportListener.settingsImportFinished(returnCode, returnMessage);
        }
    }

    public void cancel() {
        this.cancel(true);
    }

}
