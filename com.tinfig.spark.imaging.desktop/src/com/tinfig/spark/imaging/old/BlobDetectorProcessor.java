package com.tinfig.spark.imaging.old;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_legacy;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlob;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobDetector;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobSeq;
import com.googlecode.javacv.cpp.opencv_video;
import com.googlecode.javacv.cpp.opencv_video.CvBGStatModel;
import com.googlecode.javacv.cpp.opencv_video.CvGaussBGStatModelParams;
import com.tinfig.spark.imaging.desktop.Scene;
import com.tinfig.spark.imaging.desktop.Processor;

public class BlobDetectorProcessor extends Processor
{
    private final static int BLOB_MINW = 5;
    private final static int BLOB_MINH = 5;

    private final CvGaussBGStatModelParams params;

    private double learningRate = 1;
    private int nextBlobID = 1;
    private CvBGStatModel bgModel;
    private CvBlobDetector blobDetector;
    CvBlobSeq newBlobs = new CvBlobSeq();
    CvBlobSeq oldBlobs = new CvBlobSeq();

    public BlobDetectorProcessor()
    {
        this.params = new CvGaussBGStatModelParams();
        this.params.win_size(2);
        this.params.n_gauss(5);
        this.params.bg_threshold(0.7);
        this.params.std_threshold(3.5);
        this.params.minArea(15);
        this.params.weight_init(0.05);
        this.params.variance_init(30);
    }

    @Override
    public void start()
    {
        blobDetector = opencv_legacy.cvCreateBlobDetectorCC();
        // or cvCreateBlobDetectorSimple();
    }

    @Override
    public void stop()
    {
        bgModel.release();
    }

    @Override
    public void process(Scene frame)
    {

        IplImage tmp = frame.getGrabbedImage();
        // IplImage tmp = IplImage.create(frame.getImage().width(),
        // frame.getImage().height(), 8, 1);
        // opencv_imgproc.cvCvtColor(frame.getImage(), tmp,
        // opencv_imgproc.CV_BGR2GRAY);
        // opencv_imgproc.cvThreshold(tmp, tmp, 128, 255,
        // opencv_imgproc.CV_THRESH_BINARY);
        // frame.getAdditionalOutputImages().add(tmp);

        if (this.bgModel == null)
        {
            bgModel = opencv_video.cvCreateGaussianBGModel(tmp, params);
        }
        opencv_video.cvUpdateBGStatModel(tmp, bgModel, 100);

        frame.getDebugImages().add(bgModel.background());
        frame.getDebugImages().add(bgModel.foreground());

        blobDetector.DetectNewBlob(tmp, bgModel.foreground(), newBlobs, oldBlobs);

        final int blobCount = newBlobs.GetBlobNum();
        for (int i = 0; i < blobCount; i++)
        {
            CvBlob blob = newBlobs.GetBlob(i);

            blob.ID(nextBlobID);

            if (blob.w() >= BLOB_MINW && blob.h() >= BLOB_MINH)
            {
                System.out.println("Add blob " + nextBlobID);
                oldBlobs.AddBlob(blob);
                nextBlobID++;
            }
        }
    }
}
