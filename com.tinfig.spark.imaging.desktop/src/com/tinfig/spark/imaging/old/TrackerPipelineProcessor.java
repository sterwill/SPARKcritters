package com.tinfig.spark.imaging.old;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_legacy;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlob;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobTrackerAuto;
import com.googlecode.javacv.cpp.opencv_legacy.CvBlobTrackerAutoParam1;
import com.googlecode.javacv.cpp.opencv_video;
import com.googlecode.javacv.cpp.opencv_video.CvFGDStatModelParams;
import com.tinfig.spark.imaging.desktop.Scene;
import com.tinfig.spark.imaging.desktop.Processor;

public class TrackerPipelineProcessor extends Processor
{

    private CvBlobTrackerAuto tracker;

    public TrackerPipelineProcessor()
    {
    }

    @Override
    public void start()
    {

        CvBlobTrackerAutoParam1 param = new CvBlobTrackerAutoParam1();

        CvFGDStatModelParams fgdParams = new CvFGDStatModelParams();
        fgdParams.minArea(400);

        param.pFG(opencv_legacy.cvCreateFGDetectorBase(opencv_video.CV_BG_MODEL_FGD_SIMPLE, null));
        param.pBD(opencv_legacy.cvCreateBlobDetectorCC());
        param.pBT(opencv_legacy.cvCreateBlobTrackerCCMSPF());

        // param.pBTGen(opencv_legacy.cvCreateModuleBlobTrackGen1());
        // param.pBTPP(opencv_legacy.cvCreateModuleBlobTrackPostProcKalman());
        // param.UsePPData = (bt_corr && MY_STRICMP(bt_corr,"PostProcRes")==0);
        // param.pBTA(opencv_legacy.cvCreateModuleBlobTrackAnalysisHistPVS());

        tracker = opencv_legacy.cvCreateBlobTrackerAuto1(param);
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void process(Scene frame)
    {

        tracker.Process(frame.getGrabbedImage(), null);

        frame.getDebugImages().add(tracker.GetFGMask());

        final int blobCount = tracker.GetBlobNum();
        for (int i = 0; i < blobCount; i++)
        {
            final CvBlob blob = tracker.GetBlob(i);

            final CvPoint blobPosition = opencv_core.cvPointFrom32f(opencv_legacy.CV_BLOB_CENTER(blob));
            final CvSize blobSize = opencv_core.cvSize(Math.max(1, Math.round(opencv_legacy.CV_BLOB_RX(blob))),
                    Math.max(1, Math.round(opencv_legacy.CV_BLOB_RY(blob))));

            final int ellipseColor = Math.round(255 * tracker.GetState(opencv_legacy.CV_BLOB_ID(blob)));
            opencv_core.cvEllipse(frame.getGrabbedImage(), blobPosition, blobSize, 0, 0, 360,
                    opencv_core.CV_RGB(ellipseColor, 255 - ellipseColor, 0), Math.round(1 + (3 * ellipseColor) / 255),
                    8, 0);

            CvFont font = new CvFont();
            opencv_core.cvInitFont(font, opencv_core.CV_FONT_HERSHEY_PLAIN, 0.7, 0.7, 0, 1, opencv_core.CV_AA);

            final String idString = String.format("%03d", opencv_legacy.CV_BLOB_ID(blob));
            final CvSize textSize = new CvSize();
            opencv_core.cvGetTextSize(idString, font, textSize, null);

            // Write the text at the top of the ellipse
            CvPoint textPosition = new CvPoint();
            textPosition.x(blobPosition.x());
            textPosition.y(blobPosition.y() - blobSize.height());
            opencv_core.cvPutText(frame.getGrabbedImage(), idString, textPosition, font, opencv_core.CV_RGB(0, 255, 255));

            // final String stateDesc =
            // tracker.GetStateDesc(opencv_legacy.CV_BLOB_ID(blob));
            // if (stateDesc != null) {
            // final String[] lines = stateDesc.split(Pattern.quote("\n"));
            //
            // for (String line : lines) {
            // textPosition.y(textPosition.y() + textSize.height() + 1);
            // opencv_core.cvPutText(frame.getImage(), line, textPosition, font,
            // opencv_core.CV_RGB(0, 255, 255));
            // }
            // }
        }
    }
}
