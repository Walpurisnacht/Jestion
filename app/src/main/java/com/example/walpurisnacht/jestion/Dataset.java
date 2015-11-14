package com.example.walpurisnacht.jestion;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by walpurisnacht on 14/11/2015.
 */
public class Dataset {
    private Mat sample;
    private Mat response;

    protected Dataset(int row, int col) {
        sample = new Mat(row,col,CvType.CV_32F);
        response = new Mat(row,1,CvType.CV_32S);
    }

    public void putSample(int row, int col, float[] data) {
        sample.put(row,col,data);
    }

    public void putResponse(int row, int col, int[] data) {
        response.put(row,col,data);
    }

    public Mat getSample() {
        return sample;
    }

    public Mat getResponse() {
        return response;
    }
}
