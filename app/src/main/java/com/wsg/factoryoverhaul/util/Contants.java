package com.wsg.factoryoverhaul.util;

import android.os.Environment;

import com.blankj.utilcode.util.StringUtils;

import java.io.File;

public class Contants {

    public static String VIDEOPATH = Environment.getExternalStorageDirectory() + File.separator + "FACTORYOVERHAUL";
    public static String VIDEO_FILE_PATH = VIDEOPATH + File.separator + System.currentTimeMillis() + ".mp4";
}
