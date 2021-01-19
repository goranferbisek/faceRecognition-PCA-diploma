package org.ferbisek.goran.FaceRecognition;

import java.io.File;
import java.util.List;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class CameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Goran::CameraView";
    public static final String 	EXTRA_FILENAME = "org.opencv.samples.tutorial1.FILENAME";
    
    private String mFilename;
    private Mat cropedFace;
    private Context ctx;
    
    private Point irisL, irisR;
    
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
    } 

    
    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    
    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(String filename) {
        Log.i(TAG, "Taking picture");
        this.mFilename = filename;
        
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        //PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }
    
    public void setFaceMat(Mat face) {
    	this.cropedFace = face;
    }
    
    public void setIrisLeft(Point iris) {
    	this.irisL = iris;
    }
    
    public void setIrisRight(Point iris) {
    	this.irisR = iris;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
    	Log.i(TAG, "Saving a bitmap to file");
        Mat mIntermediateMat = new Mat();
        Imgproc.cvtColor(cropedFace, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);
        //Imgproc.cvtColor(cropedFace, mIntermediateMat, Imgproc.COLOR_RGBA2RGB, 3);
               
        //CROP SLIKE
        FaceProcessor fp = new  FaceProcessor(ctx, mIntermediateMat);
        fp.prepareFace();
        mIntermediateMat = fp.processedimage();

  	  	File file = new File(AppStorage.getAppAlbumPath(ctx), mFilename);
  	  	Boolean bool = null;
  	  	String filename = file.toString();
  	  	bool = Imgcodecs.imwrite(filename, mIntermediateMat);

  	  	if (bool == true) {
  	  		AppStorage.addToMediaStore(ctx, filename);
  	  		
  	  		Log.i(TAG, "JPEG saved at " + mFilename);
  	  		Toast.makeText(ctx, mFilename + " saved", Toast.LENGTH_SHORT).show();
  	  		Intent intent = new Intent(ctx,ResultActivity.class);
  	  		intent.putExtra(EXTRA_FILENAME,mFilename);
  	  		ctx.startActivity(intent);            
  	  	} else {
  	  		Log.d(TAG, "Fail writing image to external storage");
  	  	}
    }
}