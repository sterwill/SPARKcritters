package com.tinfig.spark.imaging.desktop.processors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_legacy;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlob;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobDetector;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobSeq;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobTrackPostProc;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobTracker;
import com.googlecode.javacv.cpp.opencv_legacy.CvFGDetector;
import com.tinfig.spark.imaging.desktop.Main;
import com.tinfig.spark.imaging.desktop.Processor;
import com.tinfig.spark.imaging.desktop.Scene;
import com.tinfig.spark.imaging.desktop.processors.fg.DifferenceFGDetector;
import com.tinfig.spark.imaging.desktop.processors.fg.HSVFGDetector;
import com.tinfig.util.ConfigurationUtils;

public class SterwillProcessor extends Processor
{
    public static final String CONFIG_FILE = "sterwillProcessor.properties";
    public static final String TRAIN_FRAMES = "trainFrames";
    public static final String MIN_BLOB_WIDTH = "minBlobWidth";
    public static final String MIN_BLOB_HEIGHT = "minBlobHeight";
    public static final String IMAGE_SCALE_DIVISOR = "imageScaleDivisor";
    public PropertiesConfiguration config;

    private final Main main;
    private boolean showUI;

    private CanvasFrame canvasFrame;
    private IplImage scaledImage;

    private CvFGDetector fgDetector;
    private CvBlobDetector blobDetector;
    private CvBlobTracker blobTracker;
    private CvBlobTrackPostProc trackerPostProcessor;

    private int trainFrames;
    private CvBlobSeq blobs = new CvBlobSeq();
    private int nextBlobID = 1;
    private long trackerFrameCount;

    /**
     * Key is blob ID, value is num bad frames.
     */
    private Map<Integer, Integer> badFramesForBlob = new HashMap<Integer, Integer>();

    public SterwillProcessor(Main main, boolean showUI) throws ConfigurationException
    {
        this.main = main;
        this.showUI = showUI;

        final BaseConfiguration defaults = new BaseConfiguration();
        defaults.setProperty(TRAIN_FRAMES, 60);
        defaults.setProperty(MIN_BLOB_WIDTH, 20);
        defaults.setProperty(MIN_BLOB_HEIGHT, 20);
        defaults.setProperty(IMAGE_SCALE_DIVISOR, 2);

        config = ConfigurationUtils.createFromPropertiesWithDefaults(CONFIG_FILE, defaults);
    }

    @Override
    public void start() throws ConfigurationException
    {
        createTrackingPipeline();

        if (showUI)
        {
            canvasFrame = new CanvasFrame("SterwillProcessor");
            canvasFrame.setLocation(800, 0);
            canvasFrame.getContentPane().setLayout(new BoxLayout(canvasFrame.getContentPane(), BoxLayout.Y_AXIS));

            createRadioGroup(canvasFrame, new String[] { "1", "2", "4" }, new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    config.setProperty(IMAGE_SCALE_DIVISOR, Integer.parseInt(((JRadioButton) e.getSource()).getText()));
                }
            }, Integer.toString(config.getInt(IMAGE_SCALE_DIVISOR)), "Processing scale divisor");

            createSlider(canvasFrame, "minBlobWidth", 10, 200, config.getInt(MIN_BLOB_WIDTH), new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    config.setProperty(MIN_BLOB_WIDTH, ((JSlider) e.getSource()).getValue());
                };
            });
            createSlider(canvasFrame, "minBlobHeight", 10, 200, config.getInt(MIN_BLOB_HEIGHT), new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    config.setProperty(MIN_BLOB_HEIGHT, ((JSlider) e.getSource()).getValue());
                };
            });

            if (fgDetector instanceof DifferenceFGDetector)
            {
                createSlider(canvasFrame, "threshold", 1, 255,
                        ((DifferenceFGDetector) fgDetector).config.getInt(DifferenceFGDetector.THRESHOLD),
                        new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((DifferenceFGDetector) fgDetector).config.setProperty(DifferenceFGDetector.THRESHOLD,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });

                createButton(canvasFrame, "Reset First Image", new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        ((DifferenceFGDetector) fgDetector).resetFirstImage();
                    }
                });
            }

            if (fgDetector instanceof HSVFGDetector)
            {
                createSlider(canvasFrame, "minHue", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MIN_HUE), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MIN_HUE,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });
                createSlider(canvasFrame, "maxHue", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MAX_HUE), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MAX_HUE,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });

                createSlider(canvasFrame, "minSat", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MIN_SAT), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MIN_SAT,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });
                createSlider(canvasFrame, "maxSat", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MAX_SAT), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MAX_SAT,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });

                createSlider(canvasFrame, "minVal", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MIN_VAL), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MIN_VAL,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });
                createSlider(canvasFrame, "maxVal", 0, 255,
                        ((HSVFGDetector) fgDetector).config.getInt(HSVFGDetector.MAX_VAL), new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ((HSVFGDetector) fgDetector).config.setProperty(HSVFGDetector.MAX_VAL,
                                        ((JSlider) e.getSource()).getValue());
                            };
                        });
            }

            createButton(canvasFrame, "Quit", new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    main.stopProcessing();
                }
            });

            canvasFrame.pack();
        }
    }

    private void createTrackingPipeline() throws ConfigurationException
    {
        // fgDetector = opencv_legacy.cvCreateFGDetectorBase(opencv_video.CV_BG_MODEL_FGD, null);
        fgDetector = new HSVFGDetector();
        // fgDetector = new DifferenceFGDetector();

        blobDetector = opencv_legacy.cvCreateBlobDetectorSimple();
        // blobTracker = opencv_legacy.cvCreateBlobTrackerCCMSPF();
        blobTracker = opencv_legacy.cvCreateBlobTrackerCC();
        trackerPostProcessor = opencv_legacy.cvCreateModuleBlobTrackPostProcKalman();
        trackerFrameCount = 0;
        blobs.Clear();
        badFramesForBlob.clear();
    }

    @Override
    public void stop() throws ConfigurationException
    {
        config.save();

        if (fgDetector instanceof HSVFGDetector)
        {
            ((HSVFGDetector) fgDetector).config.save();
        }
        else if (fgDetector instanceof DifferenceFGDetector)
        {
            ((DifferenceFGDetector) fgDetector).config.save();
        }

        if (showUI)
        {
            canvasFrame.dispose();
        }
    }

    @Override
    public void process(final Scene scene) throws ConfigurationException
    {
        final int scaledWidth = scene.getGrabbedImage().width() / config.getInt(IMAGE_SCALE_DIVISOR);
        final int scaledHeight = scene.getGrabbedImage().height() / config.getInt(IMAGE_SCALE_DIVISOR);

        // If first time or scaling has changed
        if (scaledImage == null || scaledImage.width() != scaledWidth || scaledImage.height() != scaledHeight)
        {
            scaledImage = opencv_core.cvCreateImage(opencv_core.cvSize(scaledWidth, scaledHeight), scene
                    .getGrabbedImage().depth(), scene.getGrabbedImage().nChannels());

            // Some of the pipeline caches old images/sizes
            createTrackingPipeline();
        }

        // Scale for analysis
        opencv_imgproc.cvResize(scene.getGrabbedImage(), scaledImage);

        // Track blobs
        trackBlobs(scaledImage, scene);

        // this.blobs is now up-to-date
        scene.setBlobs(blobs, config.getInt(IMAGE_SCALE_DIVISOR));

        trackerFrameCount++;
    }

    private void trackBlobs(final IplImage scaledImage, final Scene scene)
    {
        // Min blob size scaled to temp image
        final int scaledMinBlobWidth = config.getInt(MIN_BLOB_WIDTH) / config.getInt(IMAGE_SCALE_DIVISOR);
        final int scaledMinBlobHeight = config.getInt(MIN_BLOB_HEIGHT) / config.getInt(IMAGE_SCALE_DIVISOR);

        final IplImage fgImage;
        final IplImage fgImageClone;

        // Separate foreground from background
        {
            fgDetector.Process(scaledImage);
            fgImage = fgDetector.GetMask();

            // Add a copy for debugging because the pipeline modifies it
            fgImageClone = opencv_core.cvCloneImage(fgImage);

            if (fgDetector instanceof DifferenceFGDetector)
            {
                scene.getDebugImages().add(((DifferenceFGDetector) fgDetector).getDiffImage());
            }
        }

        int blobCount;

        // Track existing blobs in this frame
        {
            blobTracker.Process(scaledImage, fgImage);

            // Persist IDs from last run as we process each blob
            blobCount = blobs.GetBlobNum();
            for (int i = 0; i < blobCount; i++)
            {
                final CvBlob blob = blobs.GetBlob(i);
                final int blobID = opencv_legacy.CV_BLOB_ID(blob);
                final int blobIndex = blobTracker.GetBlobIndexByID(blobID);
                blobTracker.ProcessBlob(blobIndex, blob, scaledImage, fgImage);
                blob.ID(blobID);
            }
        }

        // Post-process the blobs
        {
            blobCount = blobs.GetBlobNum();
            for (int i = 0; i < blobCount; i++)
            {
                CvBlob blob = blobs.GetBlob(i);
                trackerPostProcessor.AddBlob(blob);
            }

            trackerPostProcessor.Process();

            // Build a new blob list with the processed blobs
            final CvBlobSeq ppBlobs = new CvBlobSeq();
            blobCount = blobs.GetBlobNum();
            for (int i = 0; i < blobCount; i++)
            {
                CvBlob blob = blobs.GetBlob(i);
                int blobID = opencv_legacy.CV_BLOB_ID(blob);
                CvBlob ppBlob = trackerPostProcessor.GetBlobByID((blobID));

                // Update the tracker
                if (ppBlob != null && ppBlob.w() >= scaledMinBlobWidth && ppBlob.h() >= scaledMinBlobHeight)
                {
                    blobTracker.SetBlobByID(blobID, ppBlob);
                }

                // Update the local list
                if (ppBlob != null)
                {
                    ppBlobs.AddBlob(ppBlob);
                }
                else
                {
                    ppBlobs.AddBlob(blob);
                }
            }

            // Swap the new list for the old
            blobs = ppBlobs;
        }

        // Blob deleter
        {
            blobCount = blobs.GetBlobNum();
            for (int i = 0; i < blobCount; i++)
            {
                CvBlob blob = blobs.GetBlob(i);
                int blobID = opencv_legacy.CV_BLOB_ID(blob);

                boolean good = false;
                int w = fgImage.width();
                int h = fgImage.height();
                CvRect r = opencv_legacy.CV_BLOB_RECT(blob);
                CvMat mat = new CvMat();
                double aver = 0;
                double area = opencv_legacy.CV_BLOB_WX(blob) * opencv_legacy.CV_BLOB_WY(blob);
                if (r.x() < 0)
                {
                    r.width(r.width() + r.x());
                    r.x(0);
                }
                if (r.y() < 0)
                {
                    r.height(r.height() + r.y());
                    r.y(0);
                }
                if (r.x() + r.width() >= w)
                {
                    r.width(w - r.x() - 1);
                }
                if (r.y() + r.height() >= h)
                {
                    r.height(h - r.y() - 1);
                }

                if (r.width() > 4 && r.height() > 4 && r.x() < w && r.y() < h && r.x() >= 0 && r.y() >= 0
                        && r.x() + r.width() < w && r.y() + r.height() < h && area > 0)
                {
                    aver = opencv_core.cvSum(opencv_core.cvGetSubRect(fgImage, mat, r)).val(0) / area;
                    /* if mask in blob area exists then its blob OK */
                    if (aver > 0.1 * 255)
                    {
                        good = true;
                    }
                }
                else
                {
                    int val = badFramesForBlob.containsKey(blobID) ? badFramesForBlob.get(blobID) : 0;
                    badFramesForBlob.put(blobID, val + 2);
                }

                if (good)
                {
                    badFramesForBlob.remove(blobID);
                }
                else
                {
                    int val = badFramesForBlob.containsKey(blobID) ? badFramesForBlob.get(blobID) : 0;
                    badFramesForBlob.put(blobID, val + 1);
                }
            }

            // Check error count
            blobCount = blobs.GetBlobNum();
            for (int i = blobCount; i > 0; i--)
            {
                CvBlob blob = blobs.GetBlob(i - 1);
                int blobID = opencv_legacy.CV_BLOB_ID(blob);

                int val = badFramesForBlob.containsKey(blobID) ? badFramesForBlob.get(blobID) : 0;

                if (val > 3)
                {

                    // Delete from tracker
                    blobTracker.DelBlobByID(blobID);

                    // And from local
                    blobs.DelBlob(i - 1);
                }
            }
        }

        // Update the tracker for detecting new blobs
        blobTracker.Update(scaledImage, fgImage);

        // Detect new blobs
        if (trackerFrameCount > trainFrames)
        {
            CvBlobSeq newBlobs = new CvBlobSeq();

            if (blobDetector.DetectNewBlob(scaledImage, fgImage, newBlobs, blobs) > 0)
            {

                blobCount = newBlobs.GetBlobNum();
                for (int i = 0; i < blobCount; i++)
                {
                    CvBlob newBlob = newBlobs.GetBlob(i);
                    newBlob.ID(nextBlobID);

                    if (newBlob.w() > scaledMinBlobWidth && newBlob.h() > scaledMinBlobHeight)
                    {
                        final CvBlob trackedBlob = blobTracker.AddBlob(newBlob, scaledImage, fgImage);
                        if (trackedBlob != null)
                        {
                            blobs.AddBlob(trackedBlob);
                            nextBlobID++;
                        }
                    }
                }
            }
        }

        // Draw them
        blobCount = blobs.GetBlobNum();
        for (int i = 0; i < blobCount; i++)
        {
            CvBlob blob = blobs.GetBlob(i);
            int blobID = opencv_legacy.CV_BLOB_ID(blob);

            drawBlob(fgImageClone, blob, blobID);
        }
        scene.getDebugImages().add(fgImageClone);
    }

    private void drawBlob(final IplImage image, CvBlob blob, int blobID)
    {
        CvPoint displayBlobCenter = opencv_core.cvPoint((int) Math.round(blob.x()), (int) Math.round(blob.y()));
        CvPoint displayTopLeft = opencv_core.cvPoint((int) Math.round(blob.x() - blob.w() / 2),
                (int) Math.round(blob.y() - blob.h() / 2));
        CvPoint displayBottomRight = opencv_core.cvPoint((int) Math.round(blob.x() + blob.w() / 2),
                (int) Math.round(blob.y() + blob.h() / 2));

        // Some debug drawing
        opencv_core.cvDrawCircle(image, displayBlobCenter, 3, CvScalar.WHITE, 3, 0, 0);
        opencv_core.cvDrawRect(image, displayTopLeft, displayBottomRight, CvScalar.WHITE, 1, opencv_core.CV_AA, 0);

        CvFont font = new CvFont();
        opencv_core.cvInitFont(font, opencv_core.CV_FONT_HERSHEY_PLAIN, 0.7, 0.7, 0, 1, opencv_core.CV_AA);

        final String idString = String.format("%03d", blobID);
        final CvSize textSize = new CvSize();
        opencv_core.cvGetTextSize(idString, font, textSize, null);

        CvPoint textPosition = new CvPoint();
        textPosition.x(displayTopLeft.x());
        textPosition.y(displayTopLeft.y() - textSize.height());
        opencv_core.cvPutText(image, idString, textPosition, font, CvScalar.WHITE);
    }
}
