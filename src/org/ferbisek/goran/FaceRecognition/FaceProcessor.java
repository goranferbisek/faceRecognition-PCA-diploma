package org.ferbisek.goran.FaceRecognition;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;


public class FaceProcessor {
	public static final String TAG = FaceProcessor.class.getSimpleName();
    
    private CascadeClassifier      mEyePairDetector, mEyeDetector;
    //private static final Scalar    EYE_RECT_COLOR     = new Scalar(0, 255, 0, 255);

    private Mat image, eyeROI;
    private MatOfRect eyeArea = new MatOfRect();
    private MatOfRect eyes = new MatOfRect();
    private Rect leftEye, rightEye;

    private double				   angle = 0;
    private int 				   d0 = 48; //razdalja med oèmi koncna dimenzija je 96*130px

	public FaceProcessor(Context ctx, Mat image) {
		this.image = image;	
		this.eyeROI = new Mat();

		 ClassifierHolder cHolder = ClassifierHolder.getInstance(ctx);
         mEyePairDetector = cHolder.getEyePairDetector();
         mEyeDetector = cHolder.getEyeDetector();

	}
	
	public void prepareFace() {
		
		mEyePairDetector.detectMultiScale(image, eyeArea);
		
		//risanje kvadrata okoli oci
		Point globalLeftEye = new Point(0, 0);
		Point globalRightEye = new Point(0, 0);
		Rect[] eyeRectArray = eyeArea.toArray();
        if (eyeRectArray.length == 1) {
        	double horizontalFactor = eyeRectArray[0].width*0.3;
        	double verticalFactor = eyeRectArray[0].height*0.7;
        	Point tl = new Point(eyeRectArray[0].tl().x- horizontalFactor, eyeRectArray[0].tl().y- verticalFactor);
        	Point br = new Point(eyeRectArray[0].br().x+ horizontalFactor, eyeRectArray[0].br().y+ verticalFactor);
        	
        	double halfWidth = (br.x-tl.x)/2;
        	//left eye
        	Point eyeTl = new Point(tl.x, tl.y);
        	Point eyeBr = new Point(br.x- halfWidth, br.y);
        	leftEye = getEyeRect(eyeTl, eyeBr);
        	globalLeftEye.x += eyeTl.x + leftEye.x;
        	globalLeftEye.y += eyeTl.y + leftEye.y;
        	//right eye
        	eyeTl = new Point(br.x- halfWidth, tl.y);
        	eyeBr = new Point(br.x, br.y);
        	rightEye = getEyeRect(eyeTl, eyeBr);
        	globalRightEye.x += eyeTl.x + rightEye.x;
        	globalRightEye.y += eyeTl.y + rightEye.y;
       	
        } else if(eyeRectArray.length == 0){
        	System.err.println("Eye area NOT detected!");
        } else {
			System.err.println("To many eye areas!");
		}
        
        
        //ugotavljanje centra oci
        Point leftEyeCenter = new Point(globalLeftEye.x + leftEye.width/2, globalLeftEye.y + leftEye.height/2);
        Point rightEyeCenter = new Point(globalRightEye.x + rightEye.width/2, globalRightEye.y + rightEye.height/2);
        
        //rotacija obraza
        angle = eyeAngle(leftEyeCenter, rightEyeCenter);
        this.image = rotateFace(image, angle);

        
        //raèunanje nove pozicije oèi
        Point newLeftEyeCenter = translateEyePosition(leftEyeCenter);
        Point newRightEyeCenter = translateEyePosition(rightEyeCenter);

        //resize obraza
        double LX = newLeftEyeCenter.x;
        double RX = newRightEyeCenter.x;
        double LY = newLeftEyeCenter.y;
        double RY = newRightEyeCenter.y;        	

        double eyeDistance = Math.sqrt( Math.pow(LX-RX,2)+Math.pow((LY-RY),2));
        double ratio = d0/eyeDistance;
        Imgproc.resize(image, image, new Size(), ratio, ratio, Imgproc.INTER_AREA);
        
        //rezanje obraza
        LY=LY*ratio;
        RY=RY*ratio;    
        LX=LX*ratio;
        RX=RX*ratio;

        int heightNorm = 130;
        int widthNorm = 96;
        double xRez = LX - 24;
        double yRez = LY - 67;
        this.image = this.image.submat((int)yRez,(int)yRez+heightNorm, (int)xRez,(int)xRez+widthNorm);

        //convert to grayscale if not already
        if(image.channels() != 1) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY,1);  //dela
            //Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY,1);   	
        }

        //svetlobna normalizacija
        Imgproc.equalizeHist(image, image);
	}
	
	private Rect getEyeRect(Point tl, Point br) {
        eyeROI = new Mat(image, new Rect(tl, br));
    	mEyeDetector.detectMultiScale(eyeROI, eyes);
    	Rect[] eyeArray = eyes.toArray();
    	
    	//TEMP
    	for(int i=0; i< eyeArray.length; i++) {
    		//Core.rectangle(eyeROI, eyeArray[i].br(), eyeArray[i].tl(), EYE_RECT_COLOR);
    	}
    	//END TEMP
    		
    	if(eyeArray.length == 1) {
    		//risanje kvadrata okrog oèi
    		//Core.circle(eyeROI, eyeCenter(new Rect(eyeArray[0].tl(), eyeArray[0].br())), 8, EYE_RECT_COLOR);
    	} else {
    		System.err.println("More than one eye or none! NUM  = "+eyeArray.length);
    	}
		return new Rect(eyeArray[0].tl(), eyeArray[0].br());
	}
	
	private Mat rotateFace(Mat src, double angle) {
		Mat dst = new Mat();
		Point center = new Point(image.width()/2, image.height()/2);
		Mat r = Imgproc.getRotationMatrix2D(center, Math.toDegrees(angle), 1);
		Imgproc.warpAffine(src, dst, r, src.size());
		return dst;
	}
	
	private double eyeAngle(Point left, Point right) {
		double heightDiff = left.y - right.y;
		double widthDiff = left.x - right.x;
		if(widthDiff == 0) {
			throw new ArithmeticException("Eyes have same X coordinate");
		}
		return Math.atan(heightDiff/widthDiff);
	}
	
	private Point translateEyePosition(Point eye) {
		//premaknem koordinatni sistem
		double x = eye.x - image.cols()/2;
		double y = image.rows()/2 - eye.y;
		
		//poenostavljeno množenje matrike
		double rx = Math.cos(-angle) * x + Math.sin(-angle)*y;
		double ry = -Math.sin(-angle) * x + Math.cos(-angle)*y;
		
		//popravimo koordinatni sistem nazaj
		double nx = rx + image.cols()/2;
		double ny = image.rows()/2 - ry;
		
		return new Point(nx,ny);
	}
		
	public Mat processedimage() {
		return this.image;		
	}
}