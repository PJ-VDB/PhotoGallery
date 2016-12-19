package com.example.pieter_jan.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
* Class with the SharedPreferences of PhotoGallery (used for the search field)
 */

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery"; // the key for the search query
    private static final String PREF_LAST_RESULT_ID  = "lastResultId";
    /*
    Read the query from shared preferences
    (does not have a context of its own, that's why a context needs to be passed in)
     */
    public static String getStoredQuery(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    /*
    Write the query to shared preferences
     */
    public static void setStoredQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    // Poll Flickr to check the latest id (of the results)
    public static String getPrefLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }

    public static void setLastResultId(Context context, String lastResultId){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, lastResultId)
                .apply();
    }

}
