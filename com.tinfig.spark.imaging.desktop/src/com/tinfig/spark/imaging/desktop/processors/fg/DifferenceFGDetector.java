package com.tinfig.spark.imaging.desktop.processors.fg;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_legacy.CvFGDetector;
import com.tinfig.util.ConfigurationUtils;

public class DifferenceFGDetector extends CvFGDetector
{
    public static final String CONFIG_FILE = "differenceFGDetector.properties";
    public static final String THRESHOLD = "threshold";
    public PropertiesConfiguration config;

    private IplImage firstImage;
    private IplImage diffImage;
    private IplImage fgImage;

    public DifferenceFGDetector() throws ConfigurationException
    {
        final BaseConfiguration defaults = new BaseConfiguration();
        defaults.setProperty(THRESHOLD, 50);

        config = ConfigurationUtils.createFromPropertiesWithDefaults(CONFIG_FILE, defaults);
    }

    public void resetFirstImage()
    {
        firstImage = null;
    }

    @Override
    public void Process(IplImage image)
    {
        if (firstImage == null || firstImage.width() != image.width() || firstImage.height() != image.height())
        {
            boolean isFirst = firstImage == null;

            firstImage = opencv_core.cvCloneImage(image);
            diffImage = IplImage.create(image.width(), image.height(), image.depth(), image.nChannels());
            fgImage = IplImage.create(image.width(), image.height(), opencv_core.IPL_DEPTH_8U, 1);

            // fgImage is a big empty image, just right for the first
            if (isFirst)
            {
                return;
            }
        }

        opencv_core.cvAbsDiff(firstImage, image, diffImage);

        opencv_imgproc.cvCvtColor(diffImage, fgImage, opencv_imgproc.CV_BGR2GRAY);

        // Smooth to reduce noise
        opencv_imgproc.cvSmooth(fgImage, fgImage, opencv_imgproc.CV_GAUSSIAN, 9);

        opencv_imgproc.cvThreshold(fgImage, fgImage, config.getInt(THRESHOLD), 255, opencv_imgproc.CV_THRESH_BINARY);
    }

    public IplImage getDiffImage()
    {
        return diffImage;
    }

    @Override
    public IplImage GetMask()
    {
        return fgImage;
    }
}