package com.example.pieter_jan.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by pieter-jan on 12/10/2016.
 */

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>(); // ConcurrentHashMap is a thread safe version of HashMap
// using a download request’s identifying object of type T as a key, you can store
// and retrieve the URL associated with a particular request

    // Handler to pass the image back to the UI thread
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail); //will eventually be called when an image has been fully downloaded and is ready to be added to the UI
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }


    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    /**
     * HandlerThread.onLooperPrepared() is called before the Looper checks the queue for the first time.
       This makes it a good place to create your Handler implementation.
     */
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    /**
     * The handleRequest() method is a helper method where the downloading happens. Here you check for
       the existence of a URL. Then you pass the URL to a new instance of your old friend FlickrFetchr
     */
    private void handleRequest(final T target) {
        try{
            final String url = mRequestMap.get(target);

            if(url == null){
                return;
            }

            byte[] bitmapBytes = new FlickrFetchr().getUrlbytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            // requesting to add the image to the UI
            // Because mResponseHandler is associated with the main thread's looper, all of the code inside of run() will
            // be executed on the main thread.
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url){
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });


        } catch (IOException ioe){
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG,"Got a URL: " + url);

        if(url == null){
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();

            /*
            mRequestHandler will be in charge of
            processing the message when it is pulled off the message queue. The message’s what field is set to
            MESSAGE_DOWNLOAD. Its obj field is set to the T target value (a PhotoHolder in this case) that is
            passed to queueThumbnail(…).
             */
        }

    }


    // Clear the requests when the screen rotates, otherwise the wrong images could be bound to the wrong imageviews
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }


}
