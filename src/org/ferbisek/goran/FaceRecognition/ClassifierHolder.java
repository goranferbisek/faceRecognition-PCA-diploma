package org.ferbisek.goran.FaceRecognition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.util.Log;

public class ClassifierHolder {
	private static final String TAG = ClassifierHolder.class.getSimpleName();
	private static ClassifierHolder sClassifierHolder;
	private Context mContext;
	private CascadeClassifier      mFaceDetector, mEyeSplitsDetector, mEyePairDetector, mEyeDetector;
	
	private ClassifierHolder(Context appContext) {
		this.mContext = appContext;
		this.mFaceDetector = getCascadeClassifier(R.raw.lbpcascade_frontalface, "lbpcascade_frontalface.xml");
		this.mEyeSplitsDetector = getCascadeClassifier(R.raw.haarcascade_lefteye_2splits, "haarcascade_lefteye_2splits.xml");
		this.mEyeDetector = getCascadeClassifier(R.raw.haarcascade_eye, "haarcascade_eye.xml");
		this.mEyePairDetector = getCascadeClassifier(R.raw.haarcascade_mcs_eyepair_big, "haarcascade_mcs_eyepair_big.xml");
	}
	
	public static ClassifierHolder getInstance(Context c) {
		if(sClassifierHolder == null) {
			sClassifierHolder = new ClassifierHolder(c.getApplicationContext());
		}
		return sClassifierHolder;
	}
	
	private CascadeClassifier getCascadeClassifier(int resourceId, String fileName) {
		CascadeClassifier mJavaDetector = null;
		try {
			InputStream is = mContext.getResources().openRawResource(resourceId);
			File cascadeDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir, fileName);
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
			if (mJavaDetector.empty()) {
				Log.e(TAG, "Failed to load cascade classifier"+ fileName);
				mJavaDetector = null;
			} else
				Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

			cascadeDir.delete();

		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
		}
		return mJavaDetector;
	}
	
	public CascadeClassifier getFaceDetector() {
		return this.mFaceDetector;
	}
	
	public CascadeClassifier getEyeSplitsDetector() {
		return this.mEyeSplitsDetector;
	}
	
	public CascadeClassifier getEyeDetector() {
		return this.mEyeDetector;
	}
	
	public CascadeClassifier getEyePairDetector() {
		return this.mEyePairDetector;
	}

}
