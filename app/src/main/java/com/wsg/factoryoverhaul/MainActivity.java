package com.wsg.factoryoverhaul;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;

import com.aliyun.recorder.AliyunRecorderCreator;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.aliyun.svideo.sdk.external.struct.encoder.VideoCodecs;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraType;
import com.aliyun.svideo.sdk.external.struct.recorder.MediaInfo;

public class MainActivity extends AppCompatActivity {
    AliyunIRecorder mRecorder;
    private SurfaceView glSurfaceview;
    private VideoCodecs mVideoCodec = VideoCodecs.H264_HARDWARE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceview = findViewById(R.id.aliyun_preview);
//        AliyunSnapVideoParam recordParam = new AliyunSnapVideoParam.Builder()
//                .setResolutionMode(resolutionMode)
//                .setRatioMode(ratioMode)
//                .setRecordMode(AliyunSnapVideoParam.RECORD_MODE_AUTO)
//                .setFilterList(effectDirs)
//                .setBeautyLevel(80)
//                .setBeautyStatus(true)
//                .setCameraType(CameraType.FRONT)
//                .setFlashType(FlashType.ON)
//                .setNeedClip(true)
//                .setMaxDuration(max)
//                .setMinDuration(min)
//                .setVideoQuality(videoQuality)
//                .setGop(gop)
//                .setVideoCodec(mVideoCodec)
//                .setCropMode(VideoDisplayMode.FILL)
//                .setSortMode(AliyunSnapVideoParam.SORT_MODE_VIDEO)
//                .build();

        mRecorder = AliyunRecorderCreator.getRecorderInstance(this);//参数context为当前页面的上下文
        final MediaInfo info = new MediaInfo();
        info.setVideoWidth(720);
        info.setVideoHeight(720);
        info.setVideoCodec(mVideoCodec);
        info.setCrf(25);
        mRecorder.setMediaInfo(info);
        mRecorder.setDisplayView(glSurfaceview);
        mRecorder.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorder.destroy();
    }
}
