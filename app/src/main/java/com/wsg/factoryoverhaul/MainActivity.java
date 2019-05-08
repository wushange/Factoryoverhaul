package com.wsg.factoryoverhaul;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.aliyun.recorder.AliyunRecorderCreator;
import com.aliyun.recorder.supply.AliyunIRecorder;

import static com.blankj.utilcode.util.FlashlightUtils.destroy;

public class MainActivity extends AppCompatActivity {
    AliyunIRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recorder = AliyunRecorderCreator.getRecorderInstance(this);//参数context为当前页面的上下文

        recorder.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.destroy();
    }
}
