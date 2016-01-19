package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

/**
 * Created by nick on 12/10/15.
 */
public class LBP
{
    private static final String TAG = "PPFA::LBP";
    private Mat image;
    private int grid_x;
    private int grid_y;
    private int radius;
    private int neighbors;
    private double threshold;
    private Mat histogram;

    Mat labels;

    public LBP(long imageLoc, int radius, int neighbors, int grid_x, int grid_y, double threshold)
    {
        this.image = new Mat(imageLoc);
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.radius = radius;
        this.neighbors = neighbors;
        this.threshold = threshold;
        this.histogram = new Mat();

        if (!checkInputs())
        {
            Log.e(TAG, "Inputs are bad");
        } else
        {
            Mat lbpImage = elbp(image.clone());
            histogram = spatialHistogram(lbpImage, (int) Math.pow(2, neighbors));

//            Log.d(TAG, "Distance: " + Imgproc.compareHist(histogram, histogram, Imgproc.CV_COMP_CHISQR));
        }
    }

    public Mat getHistogram()
    {
        return histogram;
    }

    private void printMat(Mat hist)
    {
        Log.d(TAG, "size: " + hist.size());
        for(int i = 0; i < hist.rows(); i++)
        {
            for(int k = 0; k < hist.cols(); k++)
            {
                double[] data = hist.get(i, k);
                for(int l = 0; l < data.length; l++)
                {
                    Log.d(TAG, "row: " + i + "\tcol: " + k + "\tData: " + data[l]);
                }
            }
        }
    }

    private void printHist(Mat hist)
    {
        Log.d(TAG, "size: " + hist.size());
        for(int i = 0; i < hist.rows(); i++)
        {
            double[] data = hist.get(i, 0);
            for(int k = 0; k < data.length; k++)
            {
                Log.d(TAG, "i: " + i + "\tdata: " + data[k]);
            }
        }
    }

    private Mat spatialHistogram(Mat src, int numPatterns)
    {
        Log.d(TAG, "spatialHistogram started");
        int width = src.cols() / grid_x;
        int height = src.rows() / grid_y;

        Mat result = Mat.zeros(grid_x * grid_y, numPatterns, CvType.CV_32FC1);

        src.convertTo(src, CvType.CV_32FC1);

        if (src.empty())
        {
            Log.e(TAG, "Image is empty.");
        } else
        {
            int resultRowIdx = 0;
            for (int i = 0; i < grid_y; i++)
            {
                for (int j = 0; j < grid_x; j++)
                {
                    Mat src_cell = new Mat(src, new Range(i * height, (i + 1) * height), new Range(j * width, (j + 1) * width));
                    Mat cell_hist = histc(src_cell.clone(), 0, (numPatterns - 1));

                    Mat result_row = result.row(resultRowIdx);
                    (cell_hist.reshape(1, 1)).convertTo(result_row, CvType.CV_32FC1);

                    resultRowIdx++;
                }
            }
        }

        return result.reshape(1, 1).clone();
    }

    private Mat histc(Mat src, int minVal, int maxVal)
    {
        Mat hist = new Mat();

        MatOfInt histSize = new MatOfInt(maxVal - minVal + 1);

        MatOfFloat histRange = new MatOfFloat(minVal, maxVal + 1);
        MatOfInt channels = new MatOfInt(0);
        Imgproc.calcHist(Arrays.asList(src), channels, new Mat(), hist, histSize, histRange);

        return hist.reshape(1,1);
    }

    private Mat elbp(Mat src)
    {
        Log.d(TAG, "elpb started");
        Mat dst = Mat.zeros(src.rows() - 2 * radius, src.cols() - 2 * radius, CvType.CV_32SC1);
        dst.setTo(new Scalar(0));

        for (int n = 0; n < neighbors; n++)
        {
            float x = (float) (radius * Math.cos((2.0 * Math.PI * n) / neighbors));
            float y = (float) (-radius * Math.cos((2.0 * Math.PI * n) / neighbors));

            int fx = (int) Math.floor(x);
            int fy = (int) Math.floor(y);
            int cx = (int) Math.ceil(x);
            int cy = (int) Math.ceil(y);

            float ty = y - fy;
            float tx = x - fx;

            float w1 = (1 - tx) * (1 - ty);
            float w2 = tx * (1 - ty);
            float w3 = (1 - tx) * ty;
            float w4 = tx * ty;

            for (int i = 0; i < (src.rows() - radius); i++)
            {
                for (int j = 0; j < (src.cols() - radius); j++)
                {
                    float t = (float) (w1 * src.get(i + fy, j + fx)[0] +
                            w2 * src.get(i + fy, j + cx)[0] +
                            w3 * src.get(i + cy, j + fx)[0] +
                            w4 * src.get(i + cy, j + cx)[0]);

                    boolean epsilonCheck = ((t > src.get(i, j)[0]) || (Math.abs(t - src.get(i, j)[0]) < Math.ulp(1.0f)));
                    if (epsilonCheck)
                    {
                        double[] addition = new double[]{dst.get(i - radius, j - radius)[0] + Math.pow(1, n)};
                        dst.put(i - radius, j - radius, addition);
                    }
                }
            }
        }
        Log.d(TAG, "elpb ended");
        return dst;
    }

    private boolean checkInputs()
    {
        boolean goodInputs = true;

        if (image.total() == 0)
        {
            Log.e(TAG, "Empty training data was given.");
            goodInputs = false;
        } else if (image.type() != CvType.CV_32SC1)
        {
            Log.d(TAG, "Label must be given as (CV_32SC1) instead of " + CvType.typeToString(image.type()));
            image.convertTo(image, CvType.CV_32SC1);
            if (image.type() == CvType.CV_32SC1)
            {
                Log.d(TAG, "Fixed! Label is now: " + CvType.typeToString(image.type()));
                goodInputs = true;
            } else
            {
                goodInputs = false;
            }
        }

        return goodInputs;
    }
}
