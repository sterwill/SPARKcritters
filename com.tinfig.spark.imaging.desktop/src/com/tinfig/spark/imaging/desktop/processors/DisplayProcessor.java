package com.tinfig.spark.imaging.desktop.processors;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.tinfig.spark.imaging.desktop.Main;
import com.tinfig.spark.imaging.desktop.Processor;
import com.tinfig.spark.imaging.desktop.Scene;

/**
 * Displays a frame on screen using a {@link CanvasFrame}.
 * 
 * @author sterwill
 */
public class DisplayProcessor extends Processor
{
    private static final double GAMMA = 1.0;
    private Main main;
    private boolean showVideo;
    private CanvasFrame mainFrame;
    private List<CanvasFrame> additionalFrames = new ArrayList<CanvasFrame>();

    public DisplayProcessor(Main main, boolean showVideo)
    {
        this.main = main;
        this.showVideo = showVideo;
    }

    @Override
    public void start()
    {
        mainFrame = newFrame("main");
        mainFrame.setVisible(true);
    }

    @Override
    public void stop()
    {
        mainFrame.setVisible(false);
        mainFrame.dispose();

        for (CanvasFrame f : additionalFrames)
        {
            f.setVisible(false);
            f.dispose();
        }
    }

    @Override
    public void process(Scene scene)
    {
        if (showVideo)
        {
            mainFrame.showImage(scene.getGrabbedImage().getBufferedImage(GAMMA));
        }

        // Display the additional images
        for (int i = 0; i < scene.getDebugImages().size(); i++)
        {
            IplImage image = scene.getDebugImages().get(i);

            // Might need to create a new frame
            final CanvasFrame f;
            if (i > additionalFrames.size() - 1)
            {
                f = newFrame(Integer.toString(i));
                f.setLocation(0, (i + 1) * 400);
                f.setVisible(true);
                additionalFrames.add(f);
            }
            else
            {
                f = additionalFrames.get(i);
            }

            f.showImage(image.getBufferedImage(GAMMA));
        }

        // Close unused frames
        for (int i = scene.getDebugImages().size(); i < additionalFrames.size(); i++)
        {
            CanvasFrame f = additionalFrames.get(i);
            f.setVisible(false);
            f.dispose();
            additionalFrames.remove(i);
        }
    }

    private CanvasFrame newFrame(final String title)
    {
        final CanvasFrame frame = new CanvasFrame(title);

        frame.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                main.stopProcessing();
            }
        });

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                main.stopProcessing();
            }
        });

        return frame;
    }
}
