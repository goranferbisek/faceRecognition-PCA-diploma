package org.ferbisek.goran.FaceRecognition;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import android.content.Context;
import android.util.Log;


public class PCA {
	
	public static final String TAG = ResultActivity.class.getSimpleName();
	
	public final static int NUM_OF_EIGENFACES = 50; //stevilo lastnih vektorjev = lastnih obrazov = velikost vektorja znaèilk
	public final static int IMG_HEIGHT = 130;
	public final static int IMG_WIDTH = 96;
	
	
	public static int runPCA(Context ctx, String testImagePath,String facePath) {
		GalleryReader gallery = null;
		
		if(AppStorage.isExternalStorageReadable()) {
			gallery = new GalleryReader(facePath, IMG_HEIGHT * IMG_WIDTH); 
		} else {
			Log.e(TAG, "External storage not readable");
		}
		
		/* read images to array */
		double[][] facesArray = gallery.getFacesArray();
		
		//centriramo podatke mean
		double[] meanFace = rowMean(facesArray);

		//Highgui.imwrite("avface-testrecog.jpg", getImage(meanFace));  //povprecen obraz
		
		/* subtract the mean face from each face vector */
		double[][] subtractedFaces = subtractFaces(facesArray, meanFace);		
		RealMatrix subtractedFaceMat = MatrixUtils.createRealMatrix(subtractedFaces);
		
				
		/* find the covariance matrix A^T*T size is MxM (M - number of images) */		
		RealMatrix covarianceMatrix = subtractedFaceMat.transpose().multiply(subtractedFaceMat);	
		
		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		// u - lastni vektorji  s - lastni vrednosti
		RealMatrix u = svd.getU();  //System.out.println("u dims "+u.getRowDimension()+"x"+u.getColumnDimension()+"\n");
		//RealMatrix s = svd.getS();  //System.out.println("s dims "+s.getRowDimension()+"x"+s.getColumnDimension()+"\n");
		
		RealVector v = new ArrayRealVector(svd.getSingularValues()); /* mogoce raje spremenim v matriko*/
		int eigenfacesCount = Math.min(NUM_OF_EIGENFACES, v.getDimension());		
		v = v.getSubVector(0, eigenfacesCount);
		
		RealMatrix eigenMat = u.getSubMatrix(0, gallery.gallerySize()-1, 0, eigenfacesCount-1);
		RealMatrix w = subtractedFaceMat.multiply(eigenMat); //dims 3000x50
		
		w = normColums(w);  // normalized faces				
		
		//projekcija
		RealMatrix pGallery = w.transpose().multiply(subtractedFaceMat);
		
		
		/* TESTNI OBRAZ */
		Mat testImage = Imgcodecs.imread(testImagePath, Imgcodecs.IMREAD_GRAYSCALE);
		testImage = testImage.reshape(0,1);
		
		double[] testFace = new double[IMG_HEIGHT * IMG_WIDTH];
		for(int i = 0; i<testImage.cols(); i++) {
			testFace[i] = testImage.get(0, i)[0] - meanFace[i];  //odstejemo povp. obraz
		}
		
		
		RealMatrix testFaceMat = MatrixUtils.createColumnRealMatrix(testFace);
		
		RealMatrix y2 = testFaceMat.transpose().multiply(w);
			
		
		// raèunanje razdalj 
		double[] razdalje = new double[gallery.gallerySize()];
		EuclideanDistance d = new EuclideanDistance();
		
		for (int i = 0; i < razdalje.length; i++) {
			razdalje[i] = d.compute(y2.getRow(0), pGallery.getColumn(i));
		}
		
		int minIndex = 0;
		double minelem = razdalje[0]; 
		for (int i = 1; i < razdalje.length; i++) {
			if (minelem > razdalje[i]) {
				minelem = razdalje[i];
				minIndex = i;
			}
		}
		
		System.out.println("minelement: "+minelem);
		System.out.println("rezult: "+minIndex);
		
		return minIndex;		
	} /* MAIN END */
	
	
	
	
	private static RealMatrix normColums(RealMatrix w) {
		RealVector v;
		for (int i = 0; i < w.getColumnDimension(); i++) {
			v = w.getColumnVector(i);
			v.unitize();
			w.setColumnVector(i, v);
		}
		return w;
	}


	public static Mat getImage(double[] face) {
		Mat image = new Mat(IMG_HEIGHT, IMG_WIDTH, CvType.CV_8UC1);
		image.put(0, 0, face);		
		return image;
	}


	public static double[][] subtractFaces(double[][] facesArray, double[] mean) {
		if(facesArray.length != mean.length) {
			System.err.println("NEUJEMANJE DIMENZIJ");
		}
		for(int row = 0; row < facesArray.length; row++) {
			for(int col=0; col < facesArray[row].length; col++) {
				facesArray[row][col] = facesArray[row][col] - mean[row];
			}
		}
		return facesArray;
	}

	public static double[] rowMean(double[][] facesArray) {
		/* sum */
		double[] mean = new double[facesArray.length];
		for(int row = 0; row < facesArray.length; row++) {
			for(int col=0; col < facesArray[row].length; col++) {
				mean[row] = mean[row] + facesArray[row][col];
			}
			/* divide */
			mean[row] = mean[row]/facesArray[row].length;
		}
		return mean;
	}
	
}
