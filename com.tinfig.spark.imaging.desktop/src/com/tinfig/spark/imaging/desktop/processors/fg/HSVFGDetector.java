package com.tinfig.spark.imaging.desktop.processors.fg;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_legacy.CvFGDetector;
import com.tinfig.util.ConfigurationUtils;

public class HSVFGDetector extends CvFGDetector
{
    public static final String CONFIG_FILE = "hsvFGDetector.properties";
    public static final String MIN_HUE = "minHue";
    public static final String MAX_HUE = "maxHue";
    public static final String MIN_SAT = "minSat";
    public static final String MAX_SAT = "maxSat";
    public static final String MIN_VAL = "minVal";
    public static final String MAX_VAL = "maxVal";
    public PropertiesConfiguration config;

    private IplImage colorImage;
    private IplImage fgImage;

    public HSVFGDetector() throws ConfigurationException
    {
        final BaseConfiguration defaults = new BaseConfiguration();
        defaults.setProperty(MIN_HUE, 60);
        defaults.setProperty(MAX_HUE, 120);
        defaults.setProperty(MIN_SAT, 128);
        defaults.setProperty(MAX_SAT, 255);
        defaults.setProperty(MIN_VAL, 128);
        defaults.setProperty(MAX_VAL, 255);

        config = ConfigurationUtils.createFromPropertiesWithDefaults(CONFIG_FILE, defaults);
    }

    @Override
    public void Process(IplImage image)
    {
        if (fgImage == null || fgImage.width() != image.width() || fgImage.height() != image.height())
        {
            colorImage = opencv_core.cvCloneImage(image);
            fgImage = IplImage.create(image.width(), image.height(), opencv_core.IPL_DEPTH_8U, 1);
        }

        // Smooth to reduce noise
        opencv_imgproc.cvSmooth(image, colorImage, opencv_imgproc.CV_GAUSSIAN, 9);

        // Filter by HSV params
        CvScalar hsv_min = opencv_core.cvScalar(config.getInt(MIN_HUE), config.getInt(MIN_SAT), config.getInt(MIN_VAL),
                0);
        CvScalar hsv_max = opencv_core.cvScalar(config.getInt(MAX_HUE), config.getInt(MAX_SAT), config.getInt(MAX_VAL),
                0);
        opencv_core.cvInRangeS(colorImage, hsv_min, hsv_max, fgImage);

        // Draw a border for edge blob detection
        opencv_core.cvDrawRect(fgImage, opencv_core.cvPoint(0, 0),
                opencv_core.cvPoint(fgImage.width(), fgImage.height()), CvScalar.BLACK, 5, 0, 0);

    }

    @Override
    public IplImage GetMask()
    {
        return fgImage;
    }
}