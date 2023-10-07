package com.shrifd.ls149.detect;

import android.graphics.RectF;


import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by randall on 18-4-28.
 */

public class PostProcess {

    ArrayList<InferenceResult.Recognition> recognitions = new ArrayList<InferenceResult.Recognition>();

    public static final int INPUT_SIZE = 640;


    public PostProcess() {

    }

    public void init() throws IOException {
        recognitions.clear();
    }

    public  ArrayList<InferenceResult.Recognition> postProcess(InferenceResult.OutputBuffer outputs) {

        recognitions.clear();
//
        if ((outputs == null) || (outputs.objs == null)) {
            return recognitions;
        }
//
        for (int i=0; i<outputs.objs.length; i++) {

            RectF detection = new RectF(
                            outputs.objs[i].left,
                            outputs.objs[i].top,
                            outputs.objs[i].right,
                            outputs.objs[i].bottom);

            InferenceResult.Recognition recog = new InferenceResult.Recognition(
                    0,
                    outputs.objs[i].name,
                    outputs.objs[i].probs,
                    detection);
            recog.setTrackId(outputs.objs[i].trackId);
            recognitions.add(recog);
        }
        //Log.i(TAG,"recognitions: "+recognitions.size());
        return recognitions;
    }


}
