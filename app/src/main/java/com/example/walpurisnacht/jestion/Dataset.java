package com.example.walpurisnacht.jestion;

import org.opencv.core.Core;
import org.opencv.core.Core.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

/**
 * Created by walpurisnacht on 14/11/2015.
 */
public class Dataset {
    private Mat
            sample,
            response;

    private Feature[] sensor = new Feature[9];

    public static final boolean
            SAMPLE = true,
            RESPONSE = false;

    protected Dataset(int row, int col) {
        sample = new Mat(row,col,CvType.CV_32F);
        response = new Mat(row,1,CvType.CV_32S);
    }

    public void putSample(int row, int col, float[] data) {
        sample.put(row, col, data);
    }

    public Mat getSample() {
        return sample;
    }

    public void putResponse(int row, int col, int[] data) {
        response.put(row,col,data);
    }

    public Mat getResponse() {
        return response;
    }

    public StringBuilder print(boolean dataset) {
        float[] data = new float[9];
        StringBuilder text = new StringBuilder();

        Mat output;

        if (dataset) {
            output = sample;
        }
        else {
            output = response;
        }

        for (int i = 0; i < 64; i++) {
            output.get(i,0,data);
            for (int k = 0; k < 9; k++) {
                text.append(data[k]);
                text.append(" ");
            }
            text.append("[LINE] " + i + "\n");
        }

        return text;
    }

    public void normalize() {
        for (int i = 0; i < 9; i++) {
            Core.normalize(sample.col(i), sample.col(i), -1, 1, Core.NORM_MINMAX);
        }
    }

    private Feature calc(int col) {
        MatOfDouble Mean = new MatOfDouble();
        MatOfDouble StdDev = new MatOfDouble();
        MinMaxLocResult minmax = new MinMaxLocResult();
        Feature temp = new Feature();

        Core.meanStdDev(sample.col(col),Mean,StdDev);
        minmax = Core.minMaxLoc(sample.col(col));

        temp.Mean = (float)Mean.get(0,0)[0];
        temp.StdDev = (float)StdDev.get(0,0)[0];
        temp.Min = (float)minmax.minVal;
        temp.Max = (float)minmax.maxVal;

        return temp;
    }

    public void sensorCalc() {
        for (int i = 0; i < 9; i++) {
            sensor[i] = calc(i);
        }
    }

    //region Get sensor feature
    public Feature getAccX() {
        return sensor[0];
    }

    public Feature getAccY() {
        return sensor[1];
    }

    public Feature getAccZ() {
        return sensor[2];
    }

    public Feature getMagX() {
        return sensor[3];
    }

    public Feature getMagY() {
        return sensor[4];
    }

    public Feature getMagZ() {
        return sensor[5];
    }

    public Feature getGyrX() {
        return sensor[6];
    }

    public Feature getGyrY() {
        return sensor[7];
    }

    public Feature getGyrZ() {
        return sensor[8];
    }
    //endregion

}
