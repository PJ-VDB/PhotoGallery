package com.example.pieter_jan.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by pieter-jan on 12/15/2016.
 *
 * A service is an application component that provides lifecycle callbacks, just like an activity. Those
 callbacks are even performed on the main UI thread for you, just like in an activity.
 A service does not run any code on a background thread out of the box.
 */

public class PollService extends IntentService {

    private static final String TAG = "PollService";

    private static long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    // Check if there already is a pendingintent
    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // This is good practice because your app cannot
//        download any data if it is not connected to the network.
        if( !isNetworkAvailableAndConnected()){ // the user can shut down services so we have to check if it is running
            return;
        }

        Log.i(TAG, "Received an intent: " + intent);

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getPrefLastResultId(this);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        }
        else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size() == 0){
            return;
        }

        String resultId = items.get(0).getId();
        if(resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        }
        else{
            Log.i(TAG, "Got a new result: " + resultId);

            // Notification when there are new results
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true) // notification will be deleted once it is pressed
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);

        }

        QueryPreferences.setLastResultId(this, resultId);

    }

    /*
    if you are doing networking in the background, you need to verify with
    the ConnectivityManager that the network is available.
     */
    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;

    }

}
