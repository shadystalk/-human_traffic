package com.shrifd.ls149.detect;

import java.util.ArrayList;

public class ObjectFlow {

    private ArrayList<String> tops = new ArrayList<>();
    private ArrayList<String> bottoms = new ArrayList<>();


    private int MAX_CACHE = 1000;

    private int MAX_LIFE_TIME = 30;

    private int current_life_time = 0;


    private int topThresh;
    private int bottomThresh;

    public ObjectFlow(int topThresh, int bottomThresh) {
        this.topThresh = topThresh;
        this.bottomThresh = bottomThresh;
    }

    public synchronized int[] cameraCount(ArrayList<InferenceResult.Recognition> recognitions) {
        //防止数据集过大
        if (tops.size() > MAX_CACHE) {
            tops.clear();
        }
        if (bottoms.size() > MAX_CACHE) {
            bottoms.clear();
        }
        int[] flows = new int[2];
        if (recognitions.size() == 0) {
            current_life_time++;
            //30帧之类，就判断丢失
            if (current_life_time == MAX_LIFE_TIME) {
                current_life_time = 0;
                tops.clear();
                bottoms.clear();
            }
        } else {
            for (int i = 0; i < recognitions.size(); i++) {
                String trackId = String.valueOf(recognitions.get(i).getTrackId());
                if (trackId.equals("0")) {
                    continue;
                }
                if (recognitions.get(i).getLocation().bottom < topThresh) {
                    if (!tops.contains(trackId)) {
                        tops.add(trackId);
                    }
                    else if (bottoms.contains(trackId)) {
                        flows[0]++;
                        bottoms.remove(trackId);
                    }
                } else if (recognitions.get(i).getLocation().bottom > bottomThresh) {
                    if (tops.contains(trackId)) {
                        flows[1]++;
                        tops.remove(trackId);
                    }
                    else if (!bottoms.contains(trackId)) {
                        bottoms.add(trackId);
                    }
                }
            }
        }
        return flows;
    }
}
