package org.ferbisek.goran.FaceRecognition;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;


public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final String	   FILENAME			   = "face.jpg";

    private Mat                    mRgba;
    private Mat                    mGray;
    private CascadeClassifier      mJavaDetector, mJavaDetectorEye;
    private Point irisL, irisR;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraView   mOpenCvCameraView;
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
                    ClassifierHolder cHolder = ClassifierHolder.getInstance(getApplicationContext());
                	mJavaDetector = cHolder.getFaceDetector();
                	mJavaDetectorEye = cHolder.getEyeSplitsDetector();

                    //mOpenCvCameraView.setCameraIndex(1);
    				mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main_surface_view);

        mOpenCvCameraView = (CameraView) findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    	mGray = new Mat();
    	mRgba = new Mat();
    }

    public void onCameraViewStopped() {
    	mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null) {
        	mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
					Objdetect.CASCADE_SCALE_IMAGE, 
					new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
					new Size());
        }

        
        Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),FACE_RECT_COLOR, 3);

			Rect r = facesArray[i];
			// compute the eye area
			/*Rect eyearea = new Rect(r.x + r.width / 8,
					(int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
					(int) (r.height / 3.0)); */
			// split it
			Rect eyearea_right = new Rect(r.x + r.width / 16,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			Rect eyearea_left = new Rect(r.x + r.width / 16
					+ (r.width - 2 * r.width / 16) / 2,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			
			irisR = getIris(mJavaDetectorEye, eyearea_right, 24);
			irisL = getIris(mJavaDetectorEye, eyearea_left, 24);
		}
		
		if (facesArray.length > 0) {
        	mOpenCvCameraView.setFaceMat(mRgba);
        	mOpenCvCameraView.setIrisLeft(irisL);
        	mOpenCvCameraView.setIrisRight(irisR);
        }

        return mRgba;
    }

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		Log.i(TAG,"onTouch event");
		arg0.performClick();  //test
		mOpenCvCameraView.takePicture(FILENAME);                
        return false;
	}
	
	private Point getIris(CascadeClassifier clasificator, Rect area, int size) {
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
				| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);

			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

			Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
					- size / 2, size, size);
			Imgproc.rectangle(mRgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			return iris;
		}
		return iris;
	}
}