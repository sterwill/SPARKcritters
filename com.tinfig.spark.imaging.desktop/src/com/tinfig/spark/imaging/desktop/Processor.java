package com.tinfig.spark.imaging.desktop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.configuration.ConfigurationException;

import com.googlecode.javacv.CanvasFrame;

public abstract class Processor
{
    public abstract void start() throws ConfigurationException;

    public abstract void stop() throws ConfigurationException;

    /**
     * Process one frame of the video stream or image file. The image may be modified.
     * 
     * @param frame
     *            the frame (not <code>null</code>)
     * @throws ConfigurationException
     */
    public abstract void process(Scene frame) throws ConfigurationException;

    protected JTextArea createText(CanvasFrame frame)
    {
        final JTextArea text = new JTextArea();

        frame.getContentPane().add(text);

        return text;
    }

    protected JCheckBox createCheckBox(final CanvasFrame frame, final String label, final boolean defaultValue,
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

    protected JPanel createRadioGroup(final CanvasFrame frame, final String[] labels, final ActionListener listener,
            final String selectedLabel, final String groupLabel)
    {
        final JPanel panel = new JPanel();
        if (groupLabel != null)
        {
            final Border border = BorderFactory.createTitledBorder(groupLabel);
            panel.setBorder(border);
        }

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final ButtonGroup group = new ButtonGroup();

        final JRadioButton[] buttons = new JRadioButton[labels.length];
        for (int i = 0; i < buttons.length; i++)
        {
            buttons[i] = new JRadioButton(labels[i]);

            final JRadioButton thisButton = buttons[i];
            final String thisLabel = labels[i];

            thisButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    System.out.println(thisLabel + ": " + thisButton.isSelected());
                }
            });
            thisButton.addActionListener(listener);

            if (thisLabel.equals(selectedLabel))
            {
                thisButton.setSelected(true);
            }

            panel.add(thisButton);
            group.add(thisButton);
        }

        frame.getContentPane().add(panel);
        return panel;
    }

    protected JSlider createSlider(final CanvasFrame frame, final String label, final int min, final int max,
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

    protected JButton createButton(final CanvasFrame frame, final String label, final ActionListener listener)
    {
        final JButton button = new JButton(label);
        button.addActionListener(listener);
        frame.getContentPane().add(button);
        return button;
    }
}
