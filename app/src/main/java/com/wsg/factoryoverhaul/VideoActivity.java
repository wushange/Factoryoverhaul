package com.wsg.factoryoverhaul;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.blankj.utilcode.util.FileUtils;
import com.wsg.factoryoverhaul.util.Contants;

import java.io.File;

import cn.jzvd.JZDataSource;
import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

public class VideoActivity extends AppCompatActivity  {
    String filePth = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                filePth = bundle.getString("PATH");
                JzvdStd jzvdStd = (JzvdStd) findViewById(R.id.videoplayer);
                JZDataSource jzDataSource = new JZDataSource(filePth, FileUtils.getFileName(filePth));
                jzvdStd.setUp(jzDataSource, JzvdStd.SCREEN_FULLSCREEN);
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (Jzvd.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Jzvd.resetAllVideos();
    }
}
