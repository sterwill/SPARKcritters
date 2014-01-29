package com.tinfig.spark.imaging.desktop;

import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.configuration.ConfigurationException;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.illposed.osc.OSCPortOut;
import com.tinfig.spark.imaging.desktop.processors.DisplayProcessor;
import com.tinfig.spark.imaging.desktop.processors.OSCSenderProcessor;
import com.tinfig.spark.imaging.desktop.processors.SterwillProcessor;

public class Main
{
    public static final int BORDER_WIDTH_PIXELS = 20;
    public static final int PRINT_FPS_PERIOD = 5;

    public static void main(final String[] args)
    {
        try
        {
            new Main(args).run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Original command line options
    private final String[] args;

    private Map<FrameGrabber, Integer> grabbersToCameras = new HashMap<FrameGrabber, Integer>();

    private final ExecutorService grabberExecutorService = Executors.newCachedThreadPool();

    // Parsed command line options
    private int numBins = 10;
    private double normalizeFactor = 0;
    private InputConfig inputConfig;
    private float captureRate;
    private int captureWidth;
    private int captureHeight;
    private boolean noVideo;
    private boolean noUI;

    private OSCPortOut[] oscOutputs;
    private boolean keepProcessing = true;

    private CvFont debugFont = new CvFont();
    private CvPoint cameraTextPosition = opencv_core.cvPoint(20, 40);

    public Main(final String[] args)
    {
        this.args = args;
    }

    public void stopProcessing()
    {
        keepProcessing = false;
    }

    private Processor[] createProcessors() throws ConfigurationException
    {
        final List<Processor> processors = new ArrayList<Processor>();

        processors.add(new SterwillProcessor(this, !noUI));
        // processors.add(new HistogramProcessor(numBins, normalizeFactor));

        // processors.add(new SimpleBlobDetectorProcessor());
        // processors.add(new HoughLinesProcessor());
        // processors.add(new BlobDetectorProcessor());
        // processors.add(new TrackerPipelineProcessor());

        processors.add(new OSCSenderProcessor(oscOutputs, !noUI));

        if (!noUI)
        {
            processors.add(new DisplayProcessor(this, !noVideo));
        }

        return processors.toArray(new Processor[processors.size()]);
    }

    public void run() throws Exception
    {
        parseArguments(args);

        opencv_core.cvInitFont(debugFont, opencv_core.CV_FONT_HERSHEY_PLAIN, 2, 2, 0, 3, opencv_core.CV_AA);

        final FrameGrabber[] grabbers = createGrabbers();
        for (FrameGrabber grabber : grabbers)
        {
            grabber.start();
        }

        // Initialize the filters
        final Processor[] processors = createProcessors();

        for (Processor p : processors)
        {
            p.start();
        }

        Scene frame = new Scene();
        IplImage image = null;

        long fpsLastPrinted = 0;
        long fpsFrameCount = 0;

        while (keepProcessing)
        {
            // Allocates one if image is null
            final long grabStart = System.currentTimeMillis();
            image = grabAll(grabbers, image);
            if ((frame.getGrabberFrameCount() % 60) == 0)
            {
                System.out.println("Grab: " + (System.currentTimeMillis() - grabStart) + " ms");
            }

            if (image == null)
            {
                break;
            }

            frame.setGrabbedImage(image);

            final long processingStart = System.currentTimeMillis();
            for (Processor p : processors)
            {
                p.process(frame);

                if ((frame.getGrabberFrameCount() % 60) == 0)
                {
                    System.out.println("Processing: " + (System.currentTimeMillis() - processingStart) + " ms");
                }
            }

            if (inputConfig.isFileImage())
            {
                break;
            }

            final long now = System.currentTimeMillis();
            if (now > fpsLastPrinted + (PRINT_FPS_PERIOD * 1000))
            {
                fpsLastPrinted = now;
                final double fps = (double) fpsFrameCount / (double) (PRINT_FPS_PERIOD);
                System.out.println(MessageFormat.format("{0,number,#.##} fps", fps));
                fpsFrameCount = 0;
            }
            else
            {
                fpsFrameCount++;
            }
        }

        frame.dispose();

        for (Processor p : processors)
        {
            p.stop();
        }

        for (FrameGrabber grabber : grabbers)
        {
            grabber.stop();
        }

        grabberExecutorService.shutdownNow();
    }

    /**
     * Grabs an image from all the grabbers and composes a vertical image.
     */
    private IplImage grabAll(final FrameGrabber[] grabbers, IplImage image) throws Exception
    {
        // If first time through, grab frames to calculate composite size
        if (image == null)
        {
            int width = 0;
            int height = 0;
            for (FrameGrabber grabber : grabbers)
            {
                final IplImage i = grabber.grab();
                if (i == null)
                {
                    return null;
                }

                height += i.height();
                width = Math.max(width, i.width());
            }

            image = IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 3);
        }

        int topOffset = 0;

        @SuppressWarnings("unchecked")
        final Future<IplImage>[] futures = new Future[grabbers.length];

        for (int f = 0; f < futures.length; f++)
        {
            final FrameGrabber thisGrabber = grabbers[f];
            futures[f] = grabberExecutorService.submit(new Callable<IplImage>()
            {
                @Override
                public IplImage call() throws Exception
                {
                    return thisGrabber.grab();
                }
            });
        }

        for (int f = 0; f < futures.length; f++)
        {
            final IplImage i = futures[f].get();
            // IplImage i = grabbers[f].grab();
            if (i == null)
            {
                return null;
            }

            // opencv_core.cvFlip(i, i, 1);
            opencv_core.cvSetImageROI(image, opencv_core.cvRect(0, topOffset, i.width(), i.height()));
            opencv_core.cvCopy(i, image);

            opencv_core.cvPutText(image, Integer.toString(grabbersToCameras.get(grabbers[f])), cameraTextPosition,
                    debugFont, CvScalar.GRAY);

            topOffset += i.height();
        }

        opencv_core.cvSetImageROI(image, opencv_core.cvRect(0, 0, image.width(), image.height()));

        return image;
    }

    private FrameGrabber[] createGrabbers()
    {
        final FrameGrabber[] grabbers;
        if (inputConfig.isFile())
        {
            // ffmpeg seems more robust for files
            grabbers = new FrameGrabber[] { new FFmpegFrameGrabber(inputConfig.getFile()) };
        }
        else
        {
            grabbers = new FrameGrabber[inputConfig.getCameras().length];
            for (int i = 0; i < grabbers.length; i++)
            {
                grabbers[i] = new OpenCVFrameGrabber(inputConfig.getCameras()[i]);
                grabbers[i].setTimeout(1000);
                if (captureRate > 0)
                {
                    grabbers[i].setFrameRate(captureRate);
                }
                if (captureWidth > 0)
                {
                    grabbers[i].setImageWidth(captureWidth);
                }
                if (captureHeight > 0)
                {
                    grabbers[i].setImageHeight(captureHeight);
                }

                grabbersToCameras.put(grabbers[i], inputConfig.getCameras()[i]);
            }
        }
        return grabbers;
    }

    private void parseArguments(String[] args) throws ParseException, SocketException, UnknownHostException
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("b", "bins", true, "number of histogram bins (integer; default " + numBins + ")");
        options.addOption("c", "camera", true, "camera number (can be specified multiple times) (default -1)");
        options.addOption("f", "file", true, "input image or video (path; camera used by default)");
        options.addOption("h", "help", false, "Shows help");
        options.addOption("n", "normalize", true, "histogram normalization factor (double; default none)");
        options.addOption("o", "osc", true,
                "host:port (can be specified multiple times) to send OSC messages to (default none)");
        options.addOption("r", "rate", true, "Configure cameras to capture at this rate (fps)");
        options.addOption("s", "size", true,
                "Configure cameras to capture at this size (<width>x<height>; default is hardware's choice)");
        options.addOption("x", "no-video", false, "don't show the video");
        options.addOption("y", "no-ui", false, "don't show any UI");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption("bins"))
        {
            numBins = Integer.parseInt(line.getOptionValue("bins"));
            System.out.println("Using " + numBins + " histogram bins");
        }

        if (line.hasOption("normalize"))
        {
            normalizeFactor = Double.parseDouble(line.getOptionValue("normalize"));
        }

        if (line.hasOption("file"))
        {
            inputConfig = new InputConfig(line.getOptionValue("file"));
            System.out.println("Using input file " + inputConfig.getFile());
            System.out.println("Input is single frame: " + inputConfig.isFileImage());

        }
        else if (line.hasOption("camera"))
        {
            String[] cameraStrings = line.getOptionValues("camera");
            int[] cameraNumbers = new int[cameraStrings.length];
            for (int i = 0; i < cameraStrings.length; i++)
            {
                cameraNumbers[i] = Integer.parseInt(cameraStrings[i]);
            }
            inputConfig = new InputConfig(cameraNumbers);

            System.out.println("Using cameras " + Arrays.toString(inputConfig.getCameras()));
            if (captureWidth > 0)
            {
                System.out.println("Configuring camera width " + captureWidth);
            }
            if (captureHeight > 0)
            {
                System.out.println("Configuring camera height " + captureHeight);
            }
        }
        else
        {
            inputConfig = new InputConfig(new int[] { 0 });
        }

        if (line.hasOption("osc"))
        {
            String[] outputStrings = line.getOptionValues("osc");
            oscOutputs = new OSCPortOut[outputStrings.length];

            for (int i = 0; i < outputStrings.length; i++)
            {
                final String outputString = outputStrings[i];
                String[] hostAndPort = outputString.split(":");

                if (hostAndPort.length != 2)
                {
                    throw new UnrecognizedOptionException("OSC output must be host:port");
                }

                oscOutputs[i] = new OSCPortOut(Inet4Address.getByName(hostAndPort[0]), Integer.parseInt(hostAndPort[1]));

                System.out.println("Sending OSC to " + hostAndPort[0] + ":" + hostAndPort[1]);
            }
        }
        else
        {
            // Sends datagrams to localhost on SuperCollider default port
            oscOutputs = new OSCPortOut[] { new OSCPortOut() };
        }

        if (line.hasOption("help"))
        {
            new HelpFormatter().printHelp(getClass().getName(), options);
            System.exit(0);
        }

        if (line.hasOption("rate"))
        {
            captureRate = Float.parseFloat(line.getOptionValue("rate"));
        }

        if (line.hasOption("size"))
        {
            String[] dimensions = line.getOptionValue("size").split("x");
            if (dimensions.length != 2)
            {
                throw new UnrecognizedOptionException("The size must be in the format integer,integer (like 1024,768)");
            }

            captureWidth = Integer.parseInt(dimensions[0]);
            captureHeight = Integer.parseInt(dimensions[1]);
        }

        if (line.hasOption("no-video"))
        {
            noVideo = true;
        }

        if (line.hasOption("no-ui"))
        {
            noUI = true;
        }
    }
}
