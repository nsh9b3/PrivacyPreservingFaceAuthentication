package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by nick on 1/6/16.
 */
public class MyLBP
{
    private static final String TAG = "PPFA::MyLBP";
    // The image used to generate the LBP histogram
    private Mat mat;

    // The generated histogram
    HashMap<Integer, Integer> histogram;

    // These are default values for simple LBP
    private final int radius = 1;
    private final int neighbors = 8;
    private final int grid_size = 16;
    private final int pixel_width_per_grid = TakePicture.imageWidth / (grid_size / (int) Math.sqrt(grid_size));
    private final int pixel_height_per_grid = TakePicture.imageHeight / (grid_size / (int) Math.sqrt(grid_size));

    public MyLBP(long croppedLong)
    {
        this.mat = new Mat(croppedLong);
        this.histogram = new HashMap<Integer, Integer>();

        // Get the row
        for (int i = 0; i < grid_size / (int) Math.sqrt(grid_size); i++)
        {
            boolean isTop = false;
            if (i == 0)
                isTop = true;

            // Get the column
            for (int k = 0; k < grid_size / (int) Math.sqrt(grid_size); k++)
            {
                boolean isLeft = false;
                if (k == 0)
                    isLeft = true;

                generateLocalHistogram(i, k, isTop, isLeft);
            }
        }
    }

    private void generateLocalHistogram(int row, int column, boolean atTop, boolean atLeft)
    {
        // Get grid dimensions
        int startRow = row * pixel_width_per_grid;
        int startCol = column * pixel_height_per_grid;
        int endRow = (row + 1) * pixel_width_per_grid - 1;
        int endCol = (column + 1) * pixel_height_per_grid - 1;

        // LBP needs to be 1 row and col smaller to avoid seg faults
        int startLBPRow;
        if (atTop)
            startLBPRow = startRow + 1;
        else
            startLBPRow = startRow;

        int startLBPCol;
        if (atLeft)
            startLBPCol = startCol + 1;
        else
            startLBPCol = startCol;

        for (int i = startLBPCol; i < endCol; i++)
        {
            for (int k = startLBPRow; k < endRow; k++)
            {
                int center = (int) mat.get(k, i)[0];

                // LBP values
                int topLeft = (int) mat.get(k - 1, i - 1)[0];
                int top = (int) mat.get(k - 1, i)[0];
                int topRight = (int) mat.get(k - 1, i + 1)[0];

                int midRight = (int) mat.get(k, i + 1)[0];

                int botRight = (int) mat.get(k + 1, i + 1)[0];
                int bot = (int) mat.get(k + 1, i)[0];
                int botLeft = (int) mat.get(k + 1, i - 1)[0];

                int midLeft = (int) mat.get(k, i - 1)[0];

                byte value = 0x00;
                if (topLeft >= center)
                    value |= 1 << 7;
                if (top >= center)
                    value |= 1 << 6;
                if (topRight >= center)
                    value |= 1 << 5;
                if (midRight >= center)
                    value |= 1 << 4;
                if (botRight >= center)
                    value |= 1 << 3;
                if (bot >= center)
                    value |= 1 << 2;
                if (botLeft >= center)
                    value |= 1 << 1;
                if (midLeft >= center)
                    value |= 1 << 0;

                if (histogram.get((value & 0xFF)) != null)
                    histogram.put((value & 0xFF), histogram.get((value & 0xFF)) + 1);
                else
                    histogram.put((value & 0xFF), 1);
            }
        }
    }

    public HashMap<Integer, Integer> getHistogram()
    {
        return histogram;
    }
}
