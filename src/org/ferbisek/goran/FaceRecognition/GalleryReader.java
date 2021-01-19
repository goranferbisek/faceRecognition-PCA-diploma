package org.ferbisek.goran.FaceRecognition;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;


public class GalleryReader {
	private File galleryDir;
	private File[] images;
	private double[][] facesArray;
	
	public GalleryReader(String path, int pixelsNum) {
		this.galleryDir = new File(path);
		this.images = galleryDir.listFiles();
		
		double[][] facesArray = new double[pixelsNum][images.length];
		Mat faceMat = new Mat();
				
		for(int imgCount = 0; imgCount < images.length; imgCount++) {
			faceMat = Imgcodecs.imread(images[imgCount].getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
			faceMat = faceMat.reshape(0,pixelsNum); //(channels - 0 ostane enako, rows - pixel number)
			
			//prepis v tabelo		
			for(int row = 0; row<faceMat.rows(); row++) { 
				facesArray[row][imgCount] = faceMat.get(row, 0)[0];
			}
		}
		this.facesArray = facesArray;
	}
	
	public double[][] getFacesArray() {
		return this.facesArray;
	}
	
	public int gallerySize() {
		return this.images.length;
	}
}
