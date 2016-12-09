package com.example.pieter_jan.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pieter-jan on 12/6/2016.
 */

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = BuildConfig.MY_API_KEY;

//    private static final String API_KEY = "6db62f99ca8fa4ee9db72dcb4bdad2d3" ; // The API key from flickr
    private List<GalleryItem> items = new ArrayList<>();

    /**
     * Fetch raw data from a URL and return it as an array of bytes.
     */
    public byte[] getUrlbytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // open a connection to an URL

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream(); // get the inputstream from the open connection

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " +
                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead); // write the inputstream into bytes
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }

    }


    /**
     * Converts the byte result from getUrlBytes to a String
     */
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlbytes(urlSpec));
    }

    // Set up the url String to fetch photos from flickr
    public List<GalleryItem> fetchItems() {
        try {
            String url = Uri.parse("https://api.flickr.com/services/rest")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s") // tell Flickr to include the URL for the small version of the picture if it is available
                    .build().toString();
            String jsonString = getUrlString(url); // connect to this url
            JSONObject jsonBody = new JSONObject(jsonString); // parse the json string into an object hierarchy that maps tho the original JSON text
            parseItems(items, jsonBody);
            Log.i(TAG, "Received JSON: " + jsonString);
        } catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        } catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }

    /**
     * Get the photo objects from the JSON, see the JSON structure on p 420
     * @param items
     * @param jsonBody
     * @throws IOException
     */

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException{

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i <photoJsonArray.length(); i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")){ // url for lower size image, not always available
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }

    }

}

