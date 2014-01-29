package com.tinfig.spark.imaging.desktop;

import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.tinfig.util.Check;

/**
 * Represents an input file (image, video) <b>or</b> camera.
 * 
 * @author sterwill
 */
public class InputConfig
{
    private final String file;
    private final int[] cameras;
    private final boolean fileIsImage;

    public InputConfig(final String file)
    {
        Check.notNullOrEmpty(file, "file");

        this.file = file;
        cameras = null;

        CvCapture capture = opencv_highgui.cvCreateFileCapture(file);
        fileIsImage = opencv_highgui.cvGetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_COUNT) == 1;
        opencv_highgui.cvReleaseCapture(capture);
    }

    public InputConfig(final int[] cameras)
    {
        Check.notNull(cameras, "cameras");

        this.file = null;
        this.cameras = cameras;
        this.fileIsImage = false;
    }

    public String getFile()
    {
        return file;
    }

    public int[] getCameras()
    {
        return cameras;
    }

    public boolean isFile()
    {
        return file != null;
    }

    public boolean isCamera()
    {
        return file == null;
    }

    /**
     * @return true if the input is a file and it's a single frame image, false otherwise (file is multi frame video or
     *         camera)
     */
    public boolean isFileImage()
    {
        return fileIsImage;
    }
}
