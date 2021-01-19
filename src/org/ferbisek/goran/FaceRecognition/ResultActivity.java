package org.ferbisek.goran.FaceRecognition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ResultActivity extends Activity {
	
	public static final String TAG = ResultActivity.class.getSimpleName();
	
	String filename;
	private ImageView mFaceView, mMatchView;
	private TextView mTextView;
	private ProgressBar mProgressBar;
	private Bitmap bImage;
	private String matchName;
	private File fileDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);

		filename = getIntent().getStringExtra(CameraView.EXTRA_FILENAME);
		
		mMatchView = (ImageView) findViewById(R.id.match_imageView);
		mMatchView.setImageResource(R.drawable.noface);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		mTextView = (TextView) findViewById(R.id.rezultText);
        
		//read result face
		fileDir = new File(AppStorage.getAppAlbumPath(getApplicationContext()));
		Log.d(TAG, "Filename: "+fileDir+"/"+filename);
		
		Mat face = Imgcodecs.imread(fileDir + File.separator + filename);					
		Bitmap bm = Bitmap.createBitmap(face.cols(), face.rows(),Bitmap.Config.ARGB_8888);
		try {
			Utils.matToBitmap(face, bm);
		} catch (Exception e) {
			Log.e(TAG, "Napaka pri konverziji matrike");
		}

		mFaceView = (ImageView) findViewById(R.id.result_imageView);
        mFaceView.setImageBitmap(bm);
        
		AsyncPCA asyncPCA = new AsyncPCA();
		asyncPCA.execute();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//don't run out of memmory before garbage collection
		BitmapDrawable bitmap = (BitmapDrawable) mFaceView.getDrawable();
		bitmap.getBitmap().recycle();
		mFaceView.setImageDrawable(null);
	};
	
	private void copyOriginalsFromAssets() {
		AssetManager am = getAssets();	
		String[] assetFiles = null;
		try {
			assetFiles = am.list(getString(R.string.assets_face_folder));
		} catch (IOException e) {
	        Log.w(TAG, "Error reading from assets: ", e);
	    }	     	
		
		String albumPath = AppStorage.getOriginalsPath(getApplicationContext());
		File outfile = new File(albumPath);
		
		if(assetFiles.length != outfile.list().length) {
			for (int i = 0; i < assetFiles.length; i++) {
		    	try {
		        	InputStream ins =  am.open(getString(R.string.assets_face_folder) +
		        						File.separator + assetFiles[i]); 	            	
		        	Log.i(TAG, "Succes reading from "+assetFiles[i]);
		
		        	Bitmap bm = BitmapFactory.decodeStream(ins);
		        	
		        	
		        	final String photoPath = albumPath + File.separator + assetFiles[i];
		        		        	
		        	File photo = new File(photoPath);
		        	FileOutputStream out = null;
		        	try {
		        	    out = new FileOutputStream(photo);
		        	    bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
		        	} catch (Exception e) {
		        	    e.printStackTrace();
		        	}	        	
		        	AppStorage.addToMediaStore(getApplicationContext(), photoPath);
		        } catch (IOException e) {
		            Log.w(TAG, "Error reading from assets: " + assetFiles[i], e);
		        }					
			}
		} else {
			Log.i(TAG, "assets allready copied");
		}
	}
	
    class AsyncPCA extends AsyncTask<Void, Integer, String> {
        @Override
        protected String doInBackground(Void... params) {   
        	if(AppStorage.isExternalStorageReadable()) {  
		        copyOriginalsFromAssets();
		        
		        String sourceFolder = null;
		        File outFolder;
		        String[] outFileList = null;
		        //process faces
		        if(AppStorage.isExternalStorageReadable()) {
		        	sourceFolder = AppStorage.getOriginalsPath(getApplicationContext());
		        	String outputFolder = AppStorage.getCroppedPath(getApplicationContext());
		        	outFolder = new File(outputFolder);
		        	outFileList = outFolder.list();
		        	
		    		File galleryDir = new File(sourceFolder);
		    		File[] images = galleryDir.listFiles();
		    		
		    		if(images.length != outFileList.length) {
		    			Mat faceMat = new Mat();
			    		String writePath;
			    		for(int imgCount = 0; imgCount < images.length; imgCount++) {
			    			Log.i(TAG, "read " + images[imgCount].toString());
			    			faceMat = Imgcodecs.imread(images[imgCount].getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			    			FaceProcessor fp = new FaceProcessor(getApplicationContext(), faceMat);
			    			fp.prepareFace();
			    			writePath = outputFolder + File.separator +images[imgCount].getName();
			    			Imgcodecs.imwrite(writePath, fp.processedimage());
			    			AppStorage.addToMediaStore(getApplicationContext(), writePath);
			    			Log.d(TAG, "write " + writePath);
			    		} 
		    		} else {
						Log.i(TAG, "originals allready processed");
					}
		        }

		        //Run PCA
		        int rezultPoz = PCA.runPCA(getApplicationContext(),
		        		fileDir + File.separator + filename,
		        		AppStorage.getCroppedPath(getApplicationContext()));
		        
		        matchName = outFileList[rezultPoz];
		        bImage = BitmapFactory.decodeFile(sourceFolder+
		        				File.separator + matchName);   
			} else {
				Log.e(TAG, "External storage not readable");
			}
        	
            return matchName;
        }
        @Override
        protected void onPostExecute(String result) {
        	mProgressBar.setVisibility(View.INVISIBLE);
        	mMatchView.setImageBitmap(bImage);
    		mTextView.setText("You look like: " + result.split("\\.")[0]);
        }
        @Override
        protected void onPreExecute() {
        	mProgressBar.setVisibility(View.VISIBLE);
        }
    }

}
