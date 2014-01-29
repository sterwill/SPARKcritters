package com.tinfig.spark.imaging.old;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class Imaging
{

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        CvMemStorage storage = CvMemStorage.create();

        CvCapture capture = opencv_highgui.cvCreateCameraCapture(0);
        IplImage originalImage = opencv_highgui.cvLoadImage("/home/sterwill/weedy-sea-dragon.jpg");

        int width = originalImage.width();
        int height = originalImage.height();

        IplImage grayImage = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 1);

        opencv_imgproc.cvCvtColor(originalImage, grayImage, opencv_imgproc.CV_BGR2GRAY);

        // CvHistogram hist = new CvHistogram();
        // opencv_imgproc.cvCalcHist(new IplImage[] { originalImage, grayImage
        // },
        // hist, 0, null);

        // IplImage smoothImage = opencv_core.cvCloneImage(originalImage);
        // opencv_imgproc.cvSmooth(originalImage, smoothImage,
        // opencv_imgproc.CV_GAUSSIAN, 9, 9, 2, 2);

        showImage(originalImage);

        opencv_core.cvClearMemStorage(storage);
    }

    private static void showImage(IplImage image)
    {
        CanvasFrame frame = new CanvasFrame(image.toString());
        frame.showImage(image);

        while (true)
        {
            if (!frame.isShowing())
            {
                break;
            }

            KeyEvent e = frame.waitKey(50);
            if (e != null && e.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                break;
            }
        }

        frame.dispose();
    }
}
