package com.tinfig.spark.imaging.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.googlecode.javacv.CanvasFrame;
import com.tinfig.spark.imaging.desktop.Processor;
import com.tinfig.spark.imaging.desktop.Scene;

public class HoughLinesProcessor extends Processor
{
    private static final String FILE_NAME = "houghLines.properties";

    private CanvasFrame canvasFrame;

    private int threshold;
    private int minLength;
    private int maxLineGap;
    private int minHue;
    private int maxHue;
    private int minSat;
    private int maxSat;
    private int minVal;
    private int maxVal;

    public HoughLinesProcessor()
    {
    }

    @Override
    public void start()
    {
        Properties p = new Properties();
        try
        {
            if (new File(FILE_NAME).exists())
            {
                p.load(new FileInputStream(FILE_NAME));

                threshold = Integer.parseInt(p.getProperty("threshold", "50"));
                minLength = Integer.parseInt(p.getProperty("minLength", "10"));
                maxLineGap = Integer.parseInt(p.getProperty("maxLineGap", "10"));

                minHue = Integer.parseInt(p.getProperty("minHue", "60"));
                maxHue = Integer.parseInt(p.getProperty("maxHue", "120"));
                minSat = Integer.parseInt(p.getProperty("minSat", "0"));
                maxSat = Integer.parseInt(p.getProperty("maxSat", "255"));
                minVal = Integer.parseInt(p.getProperty("minVal", "0"));
                maxVal = Integer.parseInt(p.getProperty("maxVal", "255"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        canvasFrame = new CanvasFrame("Controls");
        canvasFrame.setLocation(800, 0);

        canvasFrame.getContentPane().setLayout(new BoxLayout(canvasFrame.getContentPane(), BoxLayout.Y_AXIS));

        createSlider(canvasFrame, "threshold", 1, 100, threshold, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                threshold = ((JSlider) e.getSource()).getValue();
            };
        });
        createSlider(canvasFrame, "minLength", 1, 1000, minLength, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                minLength = ((JSlider) e.getSource()).getValue();
            };
        });
        createSlider(canvasFrame, "maxLineGap", 1, 500, maxLineGap, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                maxLineGap = ((JSlider) e.getSource()).getValue();
            };
        });

        createSlider(canvasFrame, "minHue", 0, 255, minHue, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                minHue = ((JSlider) e.getSource()).getValue();
            };
        });
        createSlider(canvasFrame, "maxHue", 0, 255, maxHue, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                maxHue = ((JSlider) e.getSource()).getValue();
            };
        });

        createSlider(canvasFrame, "minSat", 0, 255, minSat, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                minSat = ((JSlider) e.getSource()).getValue();
            };
        });
        createSlider(canvasFrame, "maxSat", 0, 255, maxSat, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                maxSat = ((JSlider) e.getSource()).getValue();
            };
        });

        createSlider(canvasFrame, "minVal", 0, 255, minVal, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                minVal = ((JSlider) e.getSource()).getValue();
            };
        });
        createSlider(canvasFrame, "maxVal", 0, 255, maxVal, new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                maxVal = ((JSlider) e.getSource()).getValue();
            };
        });

        canvasFrame.pack();
    }

    @Override
    public void stop()
    {
        Properties p = new Properties();
        p.setProperty("threshold", Integer.toString(threshold));
        p.setProperty("minLength", Integer.toString(minLength));
        p.setProperty("maxLineGap", Integer.toString(maxLineGap));

        p.setProperty("minHue", Integer.toString(minHue));
        p.setProperty("maxHue", Integer.toString(maxHue));
        p.setProperty("minSat", Integer.toString(minSat));
        p.setProperty("maxSat", Integer.toString(maxSat));
        p.setProperty("minVal", Integer.toString(minVal));
        p.setProperty("maxVal", Integer.toString(maxVal));

        try
        {
            p.store(new FileOutputStream(FILE_NAME), null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        canvasFrame.dispose();
    }

    @Override
    public void process(final Scene frame)
    {

        // IplImage image = IplImage.create(frame.getImageHSV().width(), frame.getImageHSV().height(),
        // opencv_core.IPL_DEPTH_8U, 1);
        // // IplImage thresholded2 = IplImage.create(frame.getImageHSV().width(),
        // // frame.getImageHSV().height(),
        // // opencv_core.IPL_DEPTH_8U, 1);
        // CvMemStorage storage = CvMemStorage.create();
        //
        // // Hue 40-70 yellow
        //
        // // Actually Value,Saturation,Hue
        // CvScalar hsv_min = opencv_core.cvScalar(minHue, minSat, minVal, 0);
        // CvScalar hsv_max = opencv_core.cvScalar(maxHue, maxSat, maxVal, 0);
        // // CvScalar hsv_min2 = opencv_core.CV_RGB(40, 50, 170);
        // // CvScalar hsv_max2 = opencv_core.CV_RGB(70, 180, 256);
        //
        // opencv_core.cvInRangeS(frame.getImageHSV(), hsv_min, hsv_max, image);
        // // opencv_core.cvInRangeS(frame.getImageHSV(), hsv_min2, hsv_max2,
        // // thresholded2);
        // // opencv_core.cvOr(thresholded, thresholded2, thresholded, null);
        //
        // opencv_imgproc.cvSmooth(image, image, opencv_imgproc.CV_GAUSSIAN, 9);
        //
        // opencv_imgproc.cvCanny(image, image, 50, 255, 3);
        //
        // CvSeq lines = opencv_imgproc.cvHoughLines2(image, storage, opencv_imgproc.CV_HOUGH_PROBABILISTIC, 1,
        // Math.PI / 180, threshold, minLength, maxLineGap);
        //
        // int i = 0;
        // while (i < lines.total())
        // {
        // CvPoint p1 = new CvPoint(opencv_core.cvGetSeqElem(lines, i++));
        // CvPoint p2 = new CvPoint(opencv_core.cvGetSeqElem(lines, i++));
        //
        // opencv_core.cvLine(frame.getGrabbedImage(), p1, p2, CvScalar.BLUE, 3, opencv_core.CV_AA, 0);
        // }
        //
        // storage.release();
        //
        // frame.getDebugImages().add(image);
    }
}
