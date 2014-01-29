package com.tinfig.spark.imaging.desktop.processors;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvArr;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_imgproc.CvHistogram;
import com.tinfig.spark.imaging.desktop.Processor;
import com.tinfig.spark.imaging.desktop.Scene;

public class HistogramProcessor extends Processor
{
    private static final float[] RANGE = new float[] { 0, 255 };

    private final int numBins;
    private final double normalizeFactor;

    /**
     * When non-<code>null</code> the planes and HSV images are allocated at this size, when <code>null</code> the
     * planes and HSV image are <code>null</code>.
     */
    private opencv_core.CvSize inputImageSize;

    private IplImage inputImageHSV;
    private IplImage bluePlane;
    private IplImage greenPlane;
    private IplImage redPlane;

    private CvHistogram blueHistogram;
    private CvHistogram greenHistogram;
    private CvHistogram redHistogram;

    public HistogramProcessor(final int numBins, final double normalizeFactor)
    {
        this.numBins = numBins;
        this.normalizeFactor = normalizeFactor;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
        if (inputImageSize != null)
        {
            inputImageHSV.release();
            inputImageHSV = null;

            blueHistogram.release();
            blueHistogram = null;

            greenHistogram.release();
            greenHistogram = null;

            redHistogram.release();
            redHistogram = null;

            bluePlane.release();
            bluePlane = null;

            greenPlane.release();
            greenPlane = null;

            redPlane.release();
            redPlane = null;

            inputImageSize = null;
        }
    }

    @Override
    public void process(final Scene scene)
    {
        final int width = scene.getGrabbedImage().width();
        final int height = scene.getGrabbedImage().height();

        // Reallocate derived storage if new or size changed
        if (inputImageSize == null || inputImageSize.width() != width || inputImageSize.height() != height)
        {
            stop();

            inputImageSize = new opencv_core.CvSize();
            inputImageSize.width(width);
            inputImageSize.height(height);

            inputImageHSV = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 3);

            bluePlane = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 1);
            greenPlane = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 1);
            redPlane = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 1);

            blueHistogram = CvHistogram.create(1, new int[] { numBins }, opencv_imgproc.CV_HIST_ARRAY,
                    new float[][] { RANGE }, 1);
            greenHistogram = CvHistogram.create(1, new int[] { numBins }, opencv_imgproc.CV_HIST_ARRAY,
                    new float[][] { RANGE }, 1);
            redHistogram = CvHistogram.create(1, new int[] { numBins }, opencv_imgproc.CV_HIST_ARRAY,
                    new float[][] { RANGE }, 1);
        }

        // Split the color planes
        opencv_core.cvSplit(scene.getGrabbedImage(), bluePlane, greenPlane, redPlane, null);

        // Convert to HSV
        opencv_imgproc.cvCvtColor(scene.getGrabbedImage(), inputImageHSV, opencv_imgproc.CV_BGR2HSV);

        opencv_imgproc.cvCalcHist(new IplImage[] { bluePlane }, blueHistogram, 0, null);
        opencv_imgproc.cvCalcHist(new IplImage[] { greenPlane }, greenHistogram, 0, null);
        opencv_imgproc.cvCalcHist(new IplImage[] { redPlane }, redHistogram, 0, null);

        if (normalizeFactor != 0)
        {
            opencv_imgproc.cvNormalizeHist(blueHistogram, normalizeFactor);
            opencv_imgproc.cvNormalizeHist(greenHistogram, normalizeFactor);
            opencv_imgproc.cvNormalizeHist(redHistogram, normalizeFactor);
        }

        final double[] blueBins = getBins(blueHistogram);
        final double[] greenBins = getBins(greenHistogram);
        final double[] redBins = getBins(redHistogram);

        // printBins(blueBins, "b");
        // printBins(greenBins, "g");
        // printBins(redBins, "r");
        // System.out.println();
    }

    private double[] getBins(CvHistogram histogram)
    {
        final CvArr bins = histogram.bins();
        final double[] ret = new double[numBins];

        for (int i = 0; i < numBins; i++)
        {
            ret[i] = opencv_core.cvGetReal1D(bins, i);
        }

        return ret;
    }

    private void printBins(final double[] bins, String name)
    {
        System.out.print(name + "=[");
        for (int i = 0; i < bins.length; i++)
        {
            System.out.printf("%05d", Math.round(bins[i]));
            if (i < bins.length - 1)
            {
                System.out.print(",");
            }
        }
        System.out.print("] ");
    }
}
