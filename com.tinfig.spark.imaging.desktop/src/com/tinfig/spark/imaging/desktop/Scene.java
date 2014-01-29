package com.tinfig.spark.imaging.desktop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlob;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobSeq;

/**
 * A mutable image representing one frame from the capture source, video, or image file and output images that should be
 * displayed.
 * 
 * @author sterwill
 */
public class Scene
{
    /**
     * The count of images grabbed already (including {@link #grabbedImage}).
     */
    private int grabberFrameCount;

    /**
     * The most recent grabbed image.
     */
    private IplImage grabbedImage;

    /**
     * Any additional images to display this cycle. Cleared each cycle.
     */
    private final List<IplImage> debugImages = new ArrayList<IplImage>();

    /**
     * Blobs in the last scene.
     */
    private final Set<RawBlobInfo> oldBlobs = new HashSet<RawBlobInfo>();

    /**
     * Blobs in the scene.
     */
    private final Set<RawBlobInfo> blobs = new HashSet<RawBlobInfo>();

    public Scene()
    {
    }

    public int getGrabberFrameCount()
    {
        return grabberFrameCount;
    }

    public IplImage getGrabbedImage()
    {
        return grabbedImage;
    }

    public void setGrabbedImage(final IplImage image)
    {
        this.grabbedImage = image;
        debugImages.clear();
        grabberFrameCount++;
    }

    public List<IplImage> getDebugImages()
    {
        return debugImages;
    }

    public void dispose()
    {
        debugImages.clear();
    }

    public void setBlobs(CvBlobSeq newBlobs, int scaleDivisor)
    {
        oldBlobs.clear();
        oldBlobs.addAll(blobs);

        blobs.clear();

        final int blobCount = newBlobs.GetBlobNum();
        for (int i = 0; i < blobCount; i++)
        {
            CvBlob newBlob = newBlobs.GetBlob(i);
            blobs.add(new RawBlobInfo(newBlob.ID(), newBlob.x() * scaleDivisor, newBlob.y() * scaleDivisor, newBlob.w()
                    * scaleDivisor, newBlob.h() * scaleDivisor));
        }
    }

    public Set<RawBlobInfo> getOldBlobs()
    {
        return oldBlobs;
    }

    public Set<RawBlobInfo> getBlobs()
    {
        return blobs;
    }
}
