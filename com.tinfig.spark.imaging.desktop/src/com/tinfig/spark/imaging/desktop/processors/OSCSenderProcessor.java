package com.tinfig.spark.imaging.desktop.processors;

import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.googlecode.javacv.CanvasFrame;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.tinfig.spark.imaging.desktop.Processor;
import com.tinfig.spark.imaging.desktop.RawBlobInfo;
import com.tinfig.spark.imaging.desktop.Scene;
import com.tinfig.util.ConfigurationUtils;

public class OSCSenderProcessor extends Processor
{
    public static final String CONFIG_FILE = "oscSenderProcessor.properties";
    public static final String FULL_TRANSMIT_PERIOD_MS = "fullTransmitPeriod";
    public PropertiesConfiguration config;

    private static final int OSC_WIDTH = 1024;
    private static final int OSC_HEIGHT = 768;

    private long nextFullSend;

    private OSCPortOut[] oscOutputs;
    private boolean showUI;
    private CanvasFrame canvasFrame;

    public OSCSenderProcessor(final OSCPortOut[] oscOutputs, boolean showUI) throws ConfigurationException
    {
        this.oscOutputs = oscOutputs;
        this.showUI = showUI;

        final BaseConfiguration defaults = new BaseConfiguration();
        defaults.setProperty(FULL_TRANSMIT_PERIOD_MS, 500);

        config = ConfigurationUtils.createFromPropertiesWithDefaults(CONFIG_FILE, defaults);
    }

    @Override
    public void start() throws ConfigurationException
    {
        if (showUI)
        {
            canvasFrame = new CanvasFrame("OSCSenderProcessor");
            canvasFrame.setLocation(1000, 0);
            canvasFrame.getContentPane().setLayout(new BoxLayout(canvasFrame.getContentPane(), BoxLayout.Y_AXIS));

            createSlider(canvasFrame, "full transmitPeriod (mss)", 100, 5000, config.getInt(FULL_TRANSMIT_PERIOD_MS),
                    new ChangeListener()
                    {
                        public void stateChanged(ChangeEvent e)
                        {
                            config.setProperty(FULL_TRANSMIT_PERIOD_MS, ((JSlider) e.getSource()).getValue());
                        };
                    });

            canvasFrame.pack();
        }

        calculateFullSendTime();
    }

    private void calculateFullSendTime()
    {
        nextFullSend = System.currentTimeMillis() + config.getInt(FULL_TRANSMIT_PERIOD_MS);
    }

    @Override
    public void stop() throws ConfigurationException
    {
        if (showUI)
        {
            canvasFrame.dispose();
        }
    }

    @Override
    public void process(Scene scene) throws ConfigurationException
    {
        // Calculate new blobs
        for (RawBlobInfo b : scene.getBlobs())
        {
            if (!scene.getOldBlobs().contains(b))
            {
                System.out.println("newblob " + b.id);
                send(new OSCMessage("/newblob", new Integer[] { b.id }));
            }
        }

        // Calculate deleted blobs
        for (RawBlobInfo b : scene.getOldBlobs())
        {
            if (!scene.getBlobs().contains(b))
            {
                System.out.println("deleteblob " + b.id);
                send(new OSCMessage("/deleteblob", new Integer[] { b.id }));
            }
        }

        if (System.currentTimeMillis() > nextFullSend)
        {
            calculateFullSendTime();
            sendFull(scene);
        }
    }

    private void sendFull(Scene scene)
    {
        final double xScale = (double) OSC_WIDTH / (double) scene.getGrabbedImage().width();
        final double yScale = (double) OSC_HEIGHT / (double) scene.getGrabbedImage().height();

        System.out.println("updating " + scene.getBlobs().size() + " blobs ");

        for (RawBlobInfo b : scene.getBlobs())
        {
            // center x, center y, width, height in 1024x768 space
            int id = b.id;
            int x = (int) Math.round(b.x * xScale);
            int y = (int) Math.round(b.y * yScale);
            int w = (int) Math.round(b.width * xScale);
            int h = (int) Math.round(b.height * yScale);

            int newX = OSC_WIDTH - (int) Math.round((((double) y / (double) OSC_HEIGHT) * (double) OSC_WIDTH));
            int newY = OSC_HEIGHT - (int) Math.round((((double) x / (double) OSC_WIDTH) * (double) OSC_HEIGHT));

            System.out.println(id + "=" + newX + "," + newY + " (" + h + "," + w + ")");

            OSCMessage msg = new OSCMessage("/blob", new Object[] { id, newX, newY, h, w });
            send(msg);
        }
    }

    private void send(OSCMessage message)
    {
        for (OSCPortOut out : oscOutputs)
        {
            try
            {
                out.send(message);
            }
            catch (IOException e)
            {
                System.out.println("Error sending OSC packet:");
                e.printStackTrace();
            }
        }
    }

}
