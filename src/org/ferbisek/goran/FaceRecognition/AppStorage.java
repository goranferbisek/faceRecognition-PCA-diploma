package org.ferbisek.goran.FaceRecognition;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

public class AppStorage {
	public static final String TAG = AppStorage.class.getSimpleName();
	
	public static boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	private static void createDirecory(File dir) {
    	boolean success = true;
    	if (!dir.exists()) {
    	    success = dir.mkdir();
    	}
    	if (success) {
    	    Log.i(TAG, "Created DIR: "+ dir.getAbsolutePath());
    	} else {
    		Log.e(TAG, "Failed to create DIR: "+ dir.getAbsolutePath()); 
    	}
	}
	
	public static String getOriginalsPath(Context ctx) {
		String appName = ctx.getString(R.string.app_name);          	
    	String galleryPath =
    			Environment.getExternalStoragePublicDirectory(
    					Environment.DIRECTORY_PICTURES).toString();
    	String albumPath = galleryPath + File.separator +
    			appName + File.separator + ctx.getString(R.string.originals_face_folder);
    	AppStorage.createDirecory(new File(albumPath));
		return albumPath;
	}
	
	public static String getCroppedPath(Context ctx) {
		String appName = ctx.getString(R.string.app_name);          	
    	String galleryPath =
    			Environment.getExternalStoragePublicDirectory(
    					Environment.DIRECTORY_PICTURES).toString();
    	String albumPath = galleryPath + File.separator +
    			appName + File.separator + ctx.getString(R.string.cropped_face_folder);
    	AppStorage.createDirecory(new File(albumPath));
		return albumPath;
	}
	
	public static String getAppAlbumPath(Context ctx) {        	
    	String albumPath = Environment.getExternalStoragePublicDirectory(
								Environment.DIRECTORY_PICTURES).toString()
				+ File.separator +
    			ctx.getString(R.string.app_name);
    	AppStorage.createDirecory(new File(albumPath));
		return albumPath;
	}
	
	public static boolean addToMediaStore(Context ctx, String path) {
		final ContentValues values = new ContentValues();
    	values.put(MediaStore.MediaColumns.DATA, path);
    	values.put(Images.Media.MIME_TYPE, "img/jpg");
    	values.put(Images.Media.TITLE, ctx.getString(R.string.app_name));
    	values.put(Images.Media.DESCRIPTION, ctx.getString(R.string.app_name));
    	values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());	        	
    	
    	Uri uri;
    	try {
    		uri = ctx.getContentResolver().insert(
    				Images.Media.EXTERNAL_CONTENT_URI, values);
    	} catch (final Exception e) {
    		Log.e(TAG, "Failed to insert photo into MediaStore");
    		e.printStackTrace();
    		return false;
    	}	
		return true;
	}
	
}
