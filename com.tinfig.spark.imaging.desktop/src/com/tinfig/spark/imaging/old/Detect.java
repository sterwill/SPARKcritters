package com.tinfig.spark.imaging.old;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_features2d;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_features2d.AdjusterAdapter;
import com.googlecode.javacv.cpp.opencv_features2d.AdjusterAdapterPtr;
import com.googlecode.javacv.cpp.opencv_features2d.DynamicAdaptedFeatureDetector;
import com.googlecode.javacv.cpp.opencv_features2d.FeatureDetector;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPointVectorVector;
import com.googlecode.javacv.cpp.opencv_features2d.SimpleBlobDetector;
import com.googlecode.javacv.cpp.opencv_features2d.SimpleBlobDetector.Params;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.googlecode.javacv.cpp.opencv_ml.CvVectors;

public class Detect
{

    public static void main(String[] args) throws Exception
    {

        new Detect().run();
    }

    private final Params detectorParameters;

    private JTextArea statusText;
    private FeatureDetector detector;

    public Detect()
    {
        this.detectorParameters = new Params();
        createDetector();
    }

    private void createDetector()
    {
        this.detector = new SimpleBlobDetector(this.detectorParameters);
    }

    public void run() throws Exception
    {

        final String file = "../com.tinfig.spark.imaging.media/overhead-walking-videos/first.flv";

        CvCapture capture = opencv_highgui.cvCreateFileCapture(file);
        final double fps = opencv_highgui.cvGetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FPS);
        opencv_highgui.cvReleaseCapture(capture);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        grabber.start();

        final CanvasFrame frame = createGUI();

        IplImage img;

        final long frameTimeMillis = (long) (1000.0 / fps);

        while (true)
        {
            long start = System.currentTimeMillis();
            img = grabber.grab();

            if (!frame.isShowing())
            {
                break;
            }

            if (img == null)
            {
                System.out.println("restarting");
                grabber.stop();
                grabber = new FFmpegFrameGrabber(file);
                grabber.start();
                continue;
            }

            processFrame(img, frame);

            long sleep = frameTimeMillis - (System.currentTimeMillis() - start);
            if (sleep > 0)
            {
                Thread.sleep(sleep);
            }
        }
        frame.dispose();

        grabber.stop();
    }

    private void setStatus(final String status)
    {
        this.statusText.setText(status);
    }

    private CanvasFrame createGUI()
    {
        final CanvasFrame frame = new CanvasFrame("foo");

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        createSlider(frame, "minDistBetweenBlobs", 0, 300, (int) detectorParameters.minDistBetweenBlobs(),
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        detectorParameters.minDistBetweenBlobs(((JSlider) e.getSource()).getValue());
                        createDetector();
                    };
                });

        createSlider(frame, "minArea", 1, 10000, (int) detectorParameters.minArea(), new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                detectorParameters.minArea(((JSlider) e.getSource()).getValue());
                createDetector();
            };
        });

        createSlider(frame, "maxArea", 1, 10000, (int) detectorParameters.minArea(), new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                detectorParameters.maxArea(((JSlider) e.getSource()).getValue());
                createDetector();
            };
        });

        createCheckBox(frame, "filterByColor", detectorParameters.filterByColor(), new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                detectorParameters.filterByColor(((JCheckBox) e.getSource()).isSelected());
                createDetector();
            };
        });

        createSlider(frame, "blobColor", 0, 255, (int) detectorParameters.minArea(), new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                detectorParameters.blobColor((byte) ((JSlider) e.getSource()).getValue());
                createDetector();
            };
        });

        this.statusText = createText(frame);

        frame.pack();
        return frame;
    }

    private JTextArea createText(CanvasFrame frame)
    {

        final JTextArea text = new JTextArea();

        frame.getContentPane().add(text);

        return text;
    }

    private JCheckBox createCheckBox(final CanvasFrame frame, final String label, final boolean defaultValue,
            final ItemListener listener)
    {

        final JCheckBox button = new JCheckBox(label, defaultValue);
        button.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                System.out.println(label + ": " + button.isSelected());

            }
        });
        button.addItemListener(listener);
        frame.getContentPane().add(button);

        return button;
    }

    private JSlider createSlider(final CanvasFrame frame, final String label, final int min, final int max,
            final int defaultValue, final ChangeListener listener)
    {

        final JSlider slider = new JSlider(min, max, defaultValue);
        slider.setBorder(BorderFactory.createTitledBorder(label));
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setMinorTickSpacing(slider.getMajorTickSpacing() / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        slider.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                System.out.println(label + ": " + Integer.toString(slider.getValue()));
            }
        });
        slider.addChangeListener(listener);

        frame.getContentPane().add(slider);

        return slider;
    }

    private void processFrame(IplImage colorImage, CanvasFrame frame)
    {

        KeyPoint keypoints = new KeyPoint();
        detector.detect(colorImage, keypoints, null);
        setStatus(Float.toString(keypoints.size()));
        opencv_features2d.drawKeypoints(null, keypoints, colorImage, CvScalar.RED,
                opencv_features2d.DrawMatchesFlags.DRAW_RICH_KEYPOINTS
                        | opencv_features2d.DrawMatchesFlags.DRAW_OVER_OUTIMG);

        frame.showImage(colorImage);
    }
}
