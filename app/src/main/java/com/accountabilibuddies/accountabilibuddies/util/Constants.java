package com.accountabilibuddies.accountabilibuddies.util;

import java.util.HashMap;

abstract public class Constants {

    public static final int UNSELECTED = -1;
    public static final int TYPE_ONE_ON_ONE = 1;
    public static final int TYPE_MULTI_USER = 2;

    // Post Type
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_TEXT = 3;
    public static final int TYPE_LOCATION = 4;

    public static final String FIRST = "FIRST";

    public static final int ANIM_DURATION_TOOLBAR = 300;

    public static final int MIN_FREQ = 1;
    public static final int BASE_FREQ = 2;

    public static HashMap<Integer,String> frequencyMap = new HashMap<>();
    static {
        frequencyMap.put(1,"Unlimited");
        frequencyMap.put(2,"Daily");
        frequencyMap.put(3,"Weekly");
        frequencyMap.put(4,"Biweekly");
        frequencyMap.put(5,"Monthly");

    }
}
