package com.shrifd.ls149.detect;

import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class InferenceResult {

    OutputBuffer mOutputBuffer;
    ArrayList<Recognition> recognitions = null;
    private boolean mIsVaild = false;   //是否需要重新计算
    PostProcess mPostProcess = new PostProcess();
    //private ObjectTracker mObjectTracker;

    public void init() throws IOException {
        mOutputBuffer = new OutputBuffer();
        mPostProcess.init();
    }

    public void reset() {
        if (recognitions != null) {
            recognitions.clear();
            mIsVaild = true;
        }
       // mObjectTracker = new ObjectTracker(640, 480, 30);
    }

    public synchronized void setResult(OutputBuffer outputs) {

//        if (mOutputBuffer.mLocations == null) {
//            mOutputBuffer.mLocations = outputs.mLocations.clone();
//            mOutputBuffer.mClasses = outputs.mClasses.clone();
//        } else {
//            arraycopy(outputs.mLocations, 0, mOutputBuffer.mLocations, 0, outputs.mLocations.length);
//            arraycopy(outputs.mClasses, 0, mOutputBuffer.mClasses, 0, outputs.mClasses.length);
//        }
        if (outputs.objs != null) {
            //Log.i("ssssssssssss","outputs.objs != null");
            mOutputBuffer.objs = new Obj[outputs.objs.length];
            for (int i = 0; i < outputs.objs.length; i++) {
                mOutputBuffer.objs[i] = new Obj();
                mOutputBuffer.objs[i].trackId = outputs.objs[i].trackId;
                mOutputBuffer.objs[i].name = outputs.objs[i].name;
                mOutputBuffer.objs[i].probs = outputs.objs[i].probs;
                mOutputBuffer.objs[i].left = outputs.objs[i].left;
                mOutputBuffer.objs[i].top = outputs.objs[i].top;
                mOutputBuffer.objs[i].right = outputs.objs[i].right;
                mOutputBuffer.objs[i].bottom = outputs.objs[i].bottom;
            }
        }
        else {
            mOutputBuffer.objs = null;
        }
        mIsVaild = false;
    }

    public synchronized ArrayList<Recognition> getResult() {
        if (!mIsVaild) {
            mIsVaild = true;
            recognitions = mPostProcess.postProcess(mOutputBuffer);
            //recognitions = mObjectTracker.tracker(recognitions);
        }

        return recognitions;
    }

    public static class OutputBuffer {
        public Obj[] objs;
    }

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    public static class Recognition {

        private int trackId = 0;
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final int id;

        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        /**
         * Optional location within the source image for the location of the recognized object.
         */
        private RectF location;

        public Recognition(
                final int id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public int getTrackId() {
            return trackId;
        }

        public void setTrackId(int trackId) {
            this.trackId = trackId;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";

            resultString += "[" + id + "] ";

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}
