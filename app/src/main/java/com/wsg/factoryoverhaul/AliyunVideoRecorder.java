/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.wsg.factoryoverhaul;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.common.global.Version;
import com.aliyun.common.utils.CommonUtil;
import com.aliyun.recorder.AliyunRecorderCreator;
import com.aliyun.recorder.supply.AliyunIClipManager;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.aliyun.recorder.supply.RecordCallback;
import com.aliyun.svideo.sdk.external.struct.common.AliyunVideoParam;
import com.aliyun.svideo.sdk.external.struct.common.VideoDisplayMode;
import com.aliyun.svideo.sdk.external.struct.common.VideoQuality;
import com.aliyun.svideo.sdk.external.struct.effect.EffectFilter;
import com.aliyun.svideo.sdk.external.struct.encoder.VideoCodecs;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraParam;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraType;
import com.aliyun.svideo.sdk.external.struct.recorder.FlashType;
import com.aliyun.svideo.sdk.external.struct.recorder.MediaInfo;
import com.aliyun.svideo.sdk.external.struct.snap.AliyunSnapVideoParam;
import com.qu.preview.callback.OnFrameCallBack;
import com.qu.preview.callback.OnTextureIdCallBack;
import com.wsg.factoryoverhaul.bean.User;
import com.wsg.factoryoverhaul.manager.DataManager;
import com.wsg.factoryoverhaul.util.FastClickUtil;
import com.wsg.factoryoverhaul.util.PermissionUtils;
import com.wsg.factoryoverhaul.view.OrientationDetector;
import com.wsg.factoryoverhaul.view.RecordTimelineView;
import com.wsg.factoryoverhaul.view.SeekWrapperLayout;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static com.wsg.factoryoverhaul.util.Contants.VIDEOPATH;
import static com.wsg.factoryoverhaul.util.Contants.VIDEO_FILE_PATH;

/**
 * 视频拍摄界面
 * 该界面进行视频录制, 选择相册资源等
 */
public class AliyunVideoRecorder extends Activity implements View.OnClickListener, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {
    private final String TAG = "AliyunVideoRecorder";
    private static final int TIMELINE_HEIGHT = 20;

    private static final int MAX_SWITCH_VELOCITY = 2000;
    private static final float FADE_IN_START_ALPHA = 0.3f;
    private static final int FILTER_ANIMATION_DURATION = 1000;


    public static final String NEED_GALLERY = "need_gallery";


    public static final String OUTPUT_PATH = "output_path";

    private static final int REQUEST_CROP = 2001;

    public static final String RESULT_TYPE = "result_type";
    public static final int RESULT_TYPE_CROP = 4001;
    public static final int RESULT_TYPE_RECORD = 4002;


    private int mResolutionMode;
    private int mMinDuration;
    private int mMaxDuration;
    private int mGop;
    private int mBitrate;
    private int mBeautyLevel;
    private int mRecordMode;
    private VideoQuality mVideoQuality = VideoQuality.HD;
    private VideoCodecs mVideoCodec = VideoCodecs.H264_HARDWARE;
    private int mRatioMode = AliyunSnapVideoParam.RATIO_MODE_3_4;
    private int mSortMode = AliyunSnapVideoParam.SORT_MODE_VIDEO;
    private AliyunIRecorder mRecorder;
    private AliyunIClipManager mClipManager;
    private SurfaceView mSurfaceView;
    private boolean isBeautyOn = false;
    private boolean isSelected = false;
    private RecordTimelineView mRecordTimelineView;
    private ImageView mSwitchBeautyBtn, mSwitchCameraBtn, mSwitchLightBtn, mBackBtn, mRecordBtn, mDeleteBtn, mCompleteBtn, mGalleryBtn;
    private TextView mRecordTimeTxt;
    private FrameLayout mToolBar, mRecorderBar;
    private FlashType mFlashType = FlashType.OFF;
    private CameraType mCameraType = CameraType.BACK;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float mScaleFactor;
    private float mLastScaleFactor;
    /**
     * 曝光度
     */
    private float mExposureCompensationRatio = 0.5f;
    private boolean isOnMaxDuration;
    private boolean isOpenFailed;
    private boolean isRecording = false;
    private AliyunVideoParam mVideoParam;
    private OrientationDetector mOrientationDetector;
    private int mTintColor, mTimelineDelBgColor,
            mTimelineBgColor, mTimelinePosY, mLightDisableRes, mLightSwitchRes;
    private long mDownTime;
    private String[] mFilterList;
    private int mFilterIndex = 0;
    private TextView mFilterTxt;
    private boolean isNeedClip;
    private boolean isNeedGallery;
    private boolean isRecordError;

    private int mFrame = 25;
    private VideoDisplayMode mCropMode = VideoDisplayMode.SCALE;
    private int mMinCropDuration = 2000;
    private int mMaxVideoDuration = 10000;
    private int mMinVideoDuration = 2000;

    private int mGalleryVisibility;

    private boolean mIsToEditor;
    private RelativeLayout rootView;
    private SeekWrapperLayout mIvRecordFocusView;


    private static final String ACTIVITY_NAME_CROP = "com.aliyun.snap.crop.MediaActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.aliyun_apsaravideo_svideo_activity_recorder);
        getStyleParam();
        initOrientationDetector();
        getData();
        initView();
        initSDK();
        reSizePreview();
    }

    public static void startRecordForResult(Activity activity, int requestCode, AliyunSnapVideoParam param) {
        Intent intent = new Intent(activity, AliyunVideoRecorder.class);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RESOLUTION, param.getResolutionMode());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RATIO, param.getRatioMode());
        intent.putExtra(AliyunSnapVideoParam.RECORD_MODE, param.getRecordMode());
        intent.putExtra(AliyunSnapVideoParam.FILTER_LIST, param.getFilterList());
        intent.putExtra(AliyunSnapVideoParam.BEAUTY_LEVEL, param.getBeautyLevel());
        intent.putExtra(AliyunSnapVideoParam.BEAUTY_STATUS, param.getBeautyStatus());
        intent.putExtra(AliyunSnapVideoParam.CAMERA_TYPE, param.getCameraType());
        intent.putExtra(AliyunSnapVideoParam.FLASH_TYPE, param.getFlashType());
        intent.putExtra(AliyunSnapVideoParam.NEED_CLIP, param.isNeedClip());
        intent.putExtra(AliyunSnapVideoParam.MAX_DURATION, param.getMaxDuration());
        intent.putExtra(AliyunSnapVideoParam.MIN_DURATION, param.getMinDuration());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_QUALITY, param.getVideoQuality());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_GOP, param.getGop());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_BITRATE, param.getVideoBitrate());
        intent.putExtra(AliyunSnapVideoParam.SORT_MODE, param.getSortMode());


        intent.putExtra(AliyunSnapVideoParam.VIDEO_FRAMERATE, param.getFrameRate());
        intent.putExtra(AliyunSnapVideoParam.CROP_MODE, param.getScaleMode());
        intent.putExtra(AliyunSnapVideoParam.MIN_CROP_DURATION, param.getMinCropDuration());
        intent.putExtra(AliyunSnapVideoParam.MIN_VIDEO_DURATION, param.getMinVideoDuration());
        intent.putExtra(AliyunSnapVideoParam.MAX_VIDEO_DURATION, param.getMaxVideoDuration());
        intent.putExtra(AliyunSnapVideoParam.SORT_MODE, param.getSortMode());
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startRecord(Context context, AliyunSnapVideoParam param) {
        Intent intent = new Intent(context, AliyunVideoRecorder.class);
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RESOLUTION, param.getResolutionMode());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_RATIO, param.getRatioMode());
        intent.putExtra(AliyunSnapVideoParam.RECORD_MODE, param.getRecordMode());
        intent.putExtra(AliyunSnapVideoParam.FILTER_LIST, param.getFilterList());
        intent.putExtra(AliyunSnapVideoParam.BEAUTY_LEVEL, param.getBeautyLevel());
        intent.putExtra(AliyunSnapVideoParam.BEAUTY_STATUS, param.getBeautyStatus());
        intent.putExtra(AliyunSnapVideoParam.CAMERA_TYPE, param.getCameraType());
        intent.putExtra(AliyunSnapVideoParam.FLASH_TYPE, param.getFlashType());
        intent.putExtra(AliyunSnapVideoParam.NEED_CLIP, param.isNeedClip());
        intent.putExtra(AliyunSnapVideoParam.MAX_DURATION, param.getMaxDuration());
        intent.putExtra(AliyunSnapVideoParam.MIN_DURATION, param.getMinDuration());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_QUALITY, param.getVideoQuality());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_GOP, param.getGop());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_BITRATE, param.getVideoBitrate());
        intent.putExtra(AliyunSnapVideoParam.SORT_MODE, param.getSortMode());
        intent.putExtra(AliyunSnapVideoParam.VIDEO_CODEC, param.getVideoCodec());


        intent.putExtra(AliyunSnapVideoParam.VIDEO_FRAMERATE, param.getFrameRate());
        intent.putExtra(AliyunSnapVideoParam.CROP_MODE, param.getScaleMode());
        intent.putExtra(AliyunSnapVideoParam.MIN_CROP_DURATION, param.getMinCropDuration());
        intent.putExtra(AliyunSnapVideoParam.MIN_VIDEO_DURATION, param.getMinVideoDuration());
        intent.putExtra(AliyunSnapVideoParam.MAX_VIDEO_DURATION, param.getMaxVideoDuration());
        intent.putExtra(AliyunSnapVideoParam.SORT_MODE, param.getSortMode());

        context.startActivity(intent);
    }

    public static String getVersion() {
        return Version.VERSION;
    }

    private void getStyleParam() {
        TypedArray a = obtainStyledAttributes(new int[]{
                R.attr.qusnap_tint_color, R.attr.qusnap_timeline_del_backgound_color,
                R.attr.qusnap_timeline_backgound_color, R.attr.qusnap_time_line_pos_y,
                R.attr.qusnap_switch_light_icon_disable, R.attr.qusnap_switch_light_icon,
                R.attr.qusnap_gallery_icon_visibility
        });
        mTintColor = a.getResourceId(0, R.color.aliyun_record_fill_progress);
        mTimelineDelBgColor = a.getResourceId(1, android.R.color.holo_red_dark);
        mTimelineBgColor = a.getResourceId(2, R.color.aliyun_editor_overlay_line);
        mTimelinePosY = (int) a.getDimension(3, 100f);
        mLightDisableRes = a.getResourceId(4, R.mipmap.aliyun_svideo_icon_light_dis);
        mLightSwitchRes = a.getResourceId(5, R.drawable.aliyun_svideo_switch_light_selector);
        mGalleryVisibility = a.getInt(6, 0);
        a.recycle();
    }

    private void reSizePreview() {
        RelativeLayout.LayoutParams previewParams = null;
        RelativeLayout.LayoutParams timeLineParams = null;
        RelativeLayout.LayoutParams durationTxtParams = null;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        switch (mRatioMode) {
            case AliyunSnapVideoParam.RATIO_MODE_1_1:
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth);
                previewParams.addRule(RelativeLayout.BELOW, R.id.aliyun_tools_bar);
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.BELOW, R.id.aliyun_preview);
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.aliyun_record_timeline);
                timeLineParams.topMargin = -mTimelinePosY;
                mToolBar.setBackgroundColor(getResources().getColor(R.color.aliyun_transparent));
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.aliyun_transparent));
                mRecordTimelineView.setColor(mTintColor, mTimelineDelBgColor, R.color.qupai_black_opacity_70pct, mTimelineBgColor);
                break;
            case AliyunSnapVideoParam.RATIO_MODE_3_4:
                int barHeight = getVirtualBarHeight();
                float ratio = (float) screenHeight / screenWidth;
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth * 4 / 3);
                if (barHeight > 0 || ratio < (16f / 9.2f)) {
                    mToolBar.setBackgroundColor(getResources().getColor(R.color.aliyun_tools_bar_color));
                } else {
                    previewParams.addRule(RelativeLayout.BELOW, R.id.aliyun_tools_bar);
                    mToolBar.setBackgroundColor(getResources().getColor(R.color.aliyun_transparent));
                }
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.BELOW, R.id.aliyun_preview);
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.aliyun_record_timeline);
                timeLineParams.topMargin = -mTimelinePosY;
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.aliyun_transparent));
                mRecordTimelineView.setColor(mTintColor, mTimelineDelBgColor, R.color.qupai_black_opacity_70pct, mTimelineBgColor);
                break;
            case AliyunSnapVideoParam.RATIO_MODE_9_16:
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth * 16 / 9);
                if (previewParams.height > screenHeight) {
                    previewParams.height = screenHeight;
                }
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.ABOVE, R.id.aliyun_record_layout);
                timeLineParams.bottomMargin = mTimelinePosY;
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.aliyun_record_timeline);
                mToolBar.setBackgroundColor(getResources().getColor(R.color.aliyun_tools_bar_color));
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.aliyun_tools_bar_color));
                mRecordTimelineView.setColor(mTintColor, mTimelineDelBgColor, R.color.qupai_black_opacity_70pct, R.color.aliyun_qupai_transparent);
                break;
            default:
                break;
        }
        if (previewParams != null) {
            mSurfaceView.setLayoutParams(previewParams);
        }
        if (timeLineParams != null) {
            mRecordTimelineView.setLayoutParams(timeLineParams);
        }
        if (durationTxtParams != null) {
            mRecordTimeTxt.setLayoutParams(durationTxtParams);
        }
    }

    private void initOrientationDetector() {
        mOrientationDetector = new OrientationDetector(getApplicationContext());
    }

    public int getVirtualBarHeight() {
        int vh = 0;
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            vh = dm.heightPixels - windowManager.getDefaultDisplay().getHeight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vh;
    }

//    private void toSourceType(){
//        if(BuildConfig.source_type.equals(SAAS_CLOSE_SOURCE)){
//            toEditor();
//        }else if(BuildConfig.source_type.equals(CUSTOM_OPEN_SOURCE)){
//            toEditor();
//        }
//    }

    private void reOpenCamera(int width, int height) {
        mRecorder.stopPreview();
        MediaInfo info = new MediaInfo();
        info.setVideoWidth(width);
        info.setVideoHeight(height);
        mRecorder.setMediaInfo(info);
        mRecorder.startPreview();
    }

    private void initView() {
        rootView = findViewById(R.id.root_view);
        mIvRecordFocusView = findViewById(R.id.iv_record_focus);
        mSurfaceView = findViewById(R.id.aliyun_preview);
        mSurfaceView.setOnTouchListener(this);
        mSwitchBeautyBtn = (ImageView) findViewById(R.id.aliyun_switch_beauty);
        mSwitchBeautyBtn.setOnClickListener(this);
        mSwitchCameraBtn = (ImageView) findViewById(R.id.aliyun_switch_camera);
        mSwitchCameraBtn.setOnClickListener(this);
        mSwitchLightBtn = (ImageView) findViewById(R.id.aliyun_switch_light);
        mSwitchLightBtn.setImageResource(mLightDisableRes);
        mSwitchLightBtn.setOnClickListener(this);
        mBackBtn = (ImageView) findViewById(R.id.aliyun_back);
        mBackBtn.setOnClickListener(this);
        mRecordBtn = (ImageView) findViewById(R.id.aliyun_record_btn);
        mRecordBtn.setOnTouchListener(this);
        mDeleteBtn = (ImageView) findViewById(R.id.aliyun_delete_btn);
        mDeleteBtn.setOnClickListener(this);
        mCompleteBtn = (ImageView) findViewById(R.id.aliyun_complete_btn);
        mCompleteBtn.setOnClickListener(this);
        mRecordTimelineView = (RecordTimelineView) findViewById(R.id.aliyun_record_timeline);
        mRecordTimelineView.setColor(mTintColor, mTimelineDelBgColor, R.color.qupai_black_opacity_70pct, mTimelineBgColor);
        mRecordTimeTxt = (TextView) findViewById(R.id.aliyun_record_time);
        mGalleryBtn = (ImageView) findViewById(R.id.aliyun_icon_default);
        if (!isNeedGallery) {
            mGalleryBtn.setVisibility(View.GONE);
        }
        mToolBar = (FrameLayout) findViewById(R.id.aliyun_tools_bar);
        mRecorderBar = (FrameLayout) findViewById(R.id.aliyun_record_layout);
        mFilterTxt = (TextView) findViewById(R.id.aliyun_filter_txt);
        mFilterTxt.setVisibility(View.GONE);
        mGalleryBtn.setOnClickListener(this);
        scaleGestureDetector = new ScaleGestureDetector(this, this);
        gestureDetector = new GestureDetector(this, this);


        mSurfaceView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvRecordFocusView.setDrawingSize(mSurfaceView.getMeasuredWidth(), mSurfaceView.getMeasuredHeight());
            }
        });

        mIvRecordFocusView.setOnViewHideListener(new SeekWrapperLayout.OnViewHideListener() {
            @Override
            public void onHided() {
                isExposureShow = false;
            }
        });
    }

    /**
     * 添加标志，录制结束后不允许调用startRecord,否则会出现下次进入录制的视频全部为不可用
     */
    private boolean isRecordFinish;

    private void initSDK() {
        mRecorder = AliyunRecorderCreator.getRecorderInstance(this);
        mRecorder.setDisplayView(mSurfaceView);
        mRecorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height, Camera.CameraInfo info) {
                isOpenFailed = false;
            }

            @Override
            public Camera.Size onChoosePreviewSize(List<Camera.Size> supportedPreviewSizes, Camera.Size preferredPreviewSizeForVideo) {
                return null;
            }

            @Override
            public void openFailed() {
                isOpenFailed = true;
            }
        });
        mRecorder.setOnTextureIdCallback(new OnTextureIdCallBack() {
            @Override
            public int onTextureIdBack(int textureId, int textureWidth, int textureHeight, float[] matrix) {
                return textureId;
            }

            @Override
            public int onScaledIdBack(int scaledId, int textureWidth, int textureHeight, float[] matrix) {
                return scaledId;
            }

            @Override
            public void onTextureDestroyed() {

            }
        });
        mClipManager = mRecorder.getClipManager();
        mClipManager.setMinDuration(mMinDuration);
        mClipManager.setMaxDuration(mMaxDuration);
        mRecordTimelineView.setMaxDuration(mClipManager.getMaxDuration());
        mRecordTimelineView.setMinDuration(mClipManager.getMinDuration());
        int[] resolution = getResolution();
        final MediaInfo info = new MediaInfo();
        info.setVideoWidth(resolution[0]);
        info.setVideoHeight(resolution[1]);
        info.setVideoCodec(mVideoCodec);
        info.setCrf(25);
//        EncoderDebugger debugger = EncoderDebugger.debug(this, 528, 704);
        mRecorder.setMediaInfo(info);
        mCameraType = mRecorder.getCameraCount() == 1 ? CameraType.BACK : mCameraType;
        mRecorder.setCamera(mCameraType);
        mRecorder.setGop(mGop);
        mRecorder.setVideoBitrate(mBitrate);
        mRecorder.setVideoQuality(mVideoQuality);

        mRecorder.setRecordCallback(new RecordCallback() {
            @Override
            public void onComplete(boolean validClip, long clipDuration) {
                Log.d(TAG, "onComplete");
                handleRecordCallback(validClip, clipDuration);
                if (isOnMaxDuration) {
                    isOnMaxDuration = false;
                    toEditor();
//                    toEditor();
//                    toSourceType();
                }
                if (!isNeedClip) {
                    toEditor();
                }
            }

            @Override
            public void onFinish(String outputPath) {
                Log.d(TAG, "onFinish");
                isRecordFinish = true;

                if (mIsToEditor) {
                    scanFile(outputPath);
                    Log.e("scan", "outputPath : " + outputPath);
                    //AliyunIImport aliyunIImport = AliyunImportCreator.getImportInstance(AliyunVideoRecorder.this);
                    //MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    //int duration = 0;
                    //int width = 0;
                    //int height = 0;
                    //try {
                    //    mmr.setDataSource(outputPath);
                    //    aliyunIImport.setVideoParam(mVideoParam);
                    //    width = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    //    height = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    //    duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    //}catch (Exception e){
                    //    Log.e(AliyunTag.TAG,"video invalid, return");
                    //    return ;
                    //}
                    //mmr.release();
                    //mVideoParam.setScaleMode(VideoDisplayMode.SCALE);
                    //mVideoParam.setOutputWidth(width);
                    //mVideoParam.setOutputHeight(height);
                    ////aliyunIImport.addVideo(outputPath,0, duration,0, AliyunDisplayMode.DEFAULT);
                    //aliyunIImport.addVideo(outputPath,0,duration,null,AliyunDisplayMode.DEFAULT);
                    //Uri projectUri = Uri.fromFile(new File(aliyunIImport.generateProjectConfigure()));
                    //AliyunIClipManager mClipManager = mRecorder.getClipManager();
                    //List<String> tempFileList = mClipManager.getVideoPathList();
                    Class editor = null;
                    try {
                        editor = Class.forName("com.aliyun.svideo.editor.editor.EditorActivity");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (editor == null) {
                        toStitch();
                        return;
                    }
                    //Intent intent = new Intent(AliyunVideoRecorder.this,editor);
                    //intent.putExtra("video_param", mVideoParam);
                    //intent.putExtra("project_json_path", projectUri.getPath());
                    //intent.putStringArrayListExtra("temp_file_list", (ArrayList<String>) tempFileList);
//        intent.putExtra(AliyunConfig.KEY_FROM, getIntent().getStringExtra(AliyunConfig.KEY_FROM));
                    try {
                        //startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        toStitch();
                    }
                } else {
                    scanFile(outputPath);
                    mClipManager.deleteAllPart();
                    Intent intent = new Intent();
                    intent.putExtra(OUTPUT_PATH, outputPath);
                    intent.putExtra(RESULT_TYPE, RESULT_TYPE_RECORD);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }

            @Override
            public void onProgress(final long duration) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecordTimelineView.setDuration((int) duration);
                        int time = (int) (mClipManager.getDuration() + duration) / 1000;
                        int min = time / 60;
                        int sec = time % 60;
                        mRecordTimeTxt.setText(String.format("%1$02d:%2$02d", min, sec));
                        if (mRecordTimeTxt.getVisibility() != View.VISIBLE) {
                            mRecordTimeTxt.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }

            @Override
            public void onMaxDuration() {
                Log.d(TAG, "onMaxDuration");
                isOnMaxDuration = true;
            }

            @Override
            public void onError(int errorCode) {
                isRecordError = true;
                handleRecordCallback(false, 0);
            }

            @Override
            public void onInitReady() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFilterList != null && mFilterList.length > mFilterIndex) {
                            EffectFilter effectFilter = new EffectFilter(mFilterList[mFilterIndex]);
                            mRecorder.applyFilter(effectFilter);
                        }
                        if (isBeautyOn) {
                            mRecorder.setBeautyLevel(mBeautyLevel);
                        }
                    }
                });
            }

            @Override
            public void onDrawReady() {

            }

            @Override
            public void onPictureBack(Bitmap bitmap) {

            }

            @Override
            public void onPictureDataBack(byte[] data) {

            }

        });

        setRecordMode(getIntent().getIntExtra(AliyunSnapVideoParam.RECORD_MODE, AliyunSnapVideoParam.RECORD_MODE_AUTO));
        setFilterList(getIntent().getStringArrayExtra(AliyunSnapVideoParam.FILTER_LIST));
        mBeautyLevel = getIntent().getIntExtra(AliyunSnapVideoParam.BEAUTY_LEVEL, 80);
        setBeautyLevel(mBeautyLevel);
        setBeautyStatus(getIntent().getBooleanExtra(AliyunSnapVideoParam.BEAUTY_STATUS, true));
        setCameraType((CameraType) getIntent().getSerializableExtra(AliyunSnapVideoParam.CAMERA_TYPE));
        setFlashType((FlashType) getIntent().getSerializableExtra(AliyunSnapVideoParam.FLASH_TYPE));
        mRecorder.setExposureCompensationRatio(mExposureCompensationRatio);
        mIvRecordFocusView.setProgress(mExposureCompensationRatio);
        mRecorder.setFocusMode(CameraParam.FOCUS_MODE_CONTINUE);
    }

    private void scanFile(String path) {
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{path}, new String[]{"video/mp4"}, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MyGlSurfaceView", "onResume");
        /**
         * 部分android4.4机型会出现跳转Activity gl为空的问题，如果不需要适配，显示视图代码可以去掉
         */
        mSurfaceView.setVisibility(View.VISIBLE);
        mRecorder.startPreview();
        if (mOrientationDetector != null && mOrientationDetector.canDetectOrientation()) {
            mOrientationDetector.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            mRecorder.cancelRecording();
            isRecording = false;
        }
        mRecorder.stopPreview();

        // 部分android4.4机型会出现跳转Activity gl为空的问题，如果不需要适配，隐藏视图代码可以去掉
        mSurfaceView.setVisibility(View.INVISIBLE);

        // 退后台时, 闪光灯icon要显示为off
        if (mCameraType == CameraType.BACK && mSwitchLightBtn != null && mFlashType == FlashType.TORCH) {
            mSwitchLightBtn.setActivated(true);
            mSwitchLightBtn.setSelected(true);
            mFlashType = FlashType.OFF;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mOrientationDetector != null) {
            mOrientationDetector.disable();
        }

        if (mIvRecordFocusView != null) {
            mIvRecordFocusView.activityStop();
        }

    }

    private void getData() {

        mResolutionMode = getIntent().getIntExtra(AliyunSnapVideoParam.VIDEO_RESOLUTION, AliyunSnapVideoParam.RESOLUTION_540P);
        mMinDuration = getIntent().getIntExtra(AliyunSnapVideoParam.MIN_DURATION, 2000);
        mMaxDuration = getIntent().getIntExtra(AliyunSnapVideoParam.MAX_DURATION, 1000 * 60);
        mRatioMode = getIntent().getIntExtra(AliyunSnapVideoParam.VIDEO_RATIO, AliyunSnapVideoParam.RATIO_MODE_3_4);
        mGop = getIntent().getIntExtra(AliyunSnapVideoParam.VIDEO_GOP, 5);
        mBitrate = getIntent().getIntExtra(AliyunSnapVideoParam.VIDEO_BITRATE, 0);
        mVideoQuality = (VideoQuality) getIntent().getSerializableExtra(AliyunSnapVideoParam.VIDEO_QUALITY);
        if (mVideoQuality == null) {
            mVideoQuality = VideoQuality.HD;
        }
        mVideoCodec = (VideoCodecs) getIntent().getSerializableExtra(AliyunSnapVideoParam.VIDEO_CODEC);
        if (mVideoCodec == null) {
            mVideoCodec = VideoCodecs.H264_HARDWARE;
        }
        isNeedClip = getIntent().getBooleanExtra(AliyunSnapVideoParam.NEED_CLIP, true);
        isNeedGallery = getIntent().getBooleanExtra(NEED_GALLERY, true) && mGalleryVisibility == 0;
        mVideoParam = new AliyunVideoParam.Builder()
                .gop(mGop)
                .bitrate(mBitrate)
                .frameRate(25)
                .videoQuality(mVideoQuality)
                .videoCodec(mVideoCodec)
                .build();

        /**
         * 裁剪参数
         */
        mFrame = getIntent().getIntExtra(AliyunSnapVideoParam.VIDEO_FRAMERATE, 25);
        mCropMode = (VideoDisplayMode) getIntent().getSerializableExtra(AliyunSnapVideoParam.CROP_MODE);
        if (mCropMode == null) {
            mCropMode = VideoDisplayMode.SCALE;
        }
        mMinCropDuration = getIntent().getIntExtra(AliyunSnapVideoParam.MIN_CROP_DURATION, 2000);
        mMinVideoDuration = getIntent().getIntExtra(AliyunSnapVideoParam.MIN_VIDEO_DURATION, 2000);
        mMaxVideoDuration = getIntent().getIntExtra(AliyunSnapVideoParam.MAX_VIDEO_DURATION, 10000);
        mSortMode = getIntent().getIntExtra(AliyunSnapVideoParam.SORT_MODE, AliyunSnapVideoParam.SORT_MODE_MERGE);

    }

    public void setRecordMode(int recordMode) {
        this.mRecordMode = recordMode;
    }

    public void setFilterList(String[] filterList) {
        this.mFilterList = filterList;
    }

    public void setBeautyStatus(boolean on) {
        isBeautyOn = on;
        if (isBeautyOn) {
            mSwitchBeautyBtn.setActivated(true);
        } else {
            mSwitchBeautyBtn.setActivated(false);
        }
        mRecorder.setBeautyStatus(on);
    }

    public void setBeautyLevel(int level) {
        if (isBeautyOn) {
            mRecorder.setBeautyLevel(level);
        }
    }

    public void setCameraType(CameraType cameraType) {
        if (cameraType == null) {
            return;
        }
        mRecorder.setCamera(cameraType);
        mCameraType = cameraType;
        if (mCameraType == CameraType.BACK) {
            mSwitchCameraBtn.setActivated(false);
        } else if (mCameraType == CameraType.FRONT) {
            mSwitchCameraBtn.setActivated(true);
        }
    }

    /**
     * 切换闪光灯
     *
     * @param flashType ON:开, OFF:关, AUTO:自动(目前sdk无效, Demo中只有开关两种状态)
     */
    public void setFlashType(FlashType flashType) {
        if (flashType == null) {
            return;
        }
        if (mCameraType == CameraType.FRONT) {
            mSwitchLightBtn.setEnabled(false);
            mSwitchLightBtn.setImageResource(mLightDisableRes);
            return;
        } else if (mCameraType == CameraType.BACK) {
            mSwitchLightBtn.setEnabled(true);
            mSwitchLightBtn.setImageResource(mLightSwitchRes);
        }
        mFlashType = flashType;
        switch (mFlashType) {
            case TORCH:
                mSwitchLightBtn.setSelected(true);
                mSwitchLightBtn.setActivated(false);
                break;
            case OFF:
                mSwitchLightBtn.setSelected(true);
                mSwitchLightBtn.setActivated(true);
                break;
            default:
                break;
        }
        mRecorder.setLight(mFlashType);
    }

    private int[] getResolution() {
        int[] resolution = new int[2];
        int width = 0;
        int height = 0;
        switch (mResolutionMode) {
            case AliyunSnapVideoParam.RESOLUTION_360P:
                width = 360;
                break;
            case AliyunSnapVideoParam.RESOLUTION_480P:
                width = 480;
                break;
            case AliyunSnapVideoParam.RESOLUTION_540P:
                width = 540;
                break;
            case AliyunSnapVideoParam.RESOLUTION_720P:
                width = 720;
                break;
            default:
                break;
        }
        switch (mRatioMode) {
            case AliyunSnapVideoParam.RATIO_MODE_1_1:
                height = width;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_3_4:
                height = width * 4 / 3;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_9_16:
                height = width * 16 / 9;
                break;
            default:
                height = width;
                break;
        }
        resolution[0] = width;
        resolution[1] = height;
        return resolution;
    }

    @Override
    public void onBackPressed() {
        if (!isRecording) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        if (mRecorder != null) {
            mRecorder.getClipManager().deleteAllPart();//直接返回则删除所有临时文件
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorder.destroy();
        if (mOrientationDetector != null) {
            mOrientationDetector.setOrientationChangedListener(null);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSwitchBeautyBtn) {
            if (isBeautyOn) {
                isBeautyOn = false;
                mSwitchBeautyBtn.setActivated(false);
            } else {
                isBeautyOn = true;
                mSwitchBeautyBtn.setActivated(true);
            }
            mRecorder.setBeautyStatus(isBeautyOn);
        } else if (v == mSwitchCameraBtn) {
            if (FastClickUtil.isFastClick()) {
                return;
            }
            int type = mRecorder.switchCamera();
            if (type == CameraType.BACK.getType()) {
                mCameraType = CameraType.BACK;
                mSwitchLightBtn.setEnabled(true);
                mSwitchLightBtn.setImageResource(mLightSwitchRes);
                mSwitchCameraBtn.setActivated(false);
                setFlashType(mFlashType);
            } else if (type == CameraType.FRONT.getType()) {
                mCameraType = CameraType.FRONT;
                mSwitchLightBtn.setEnabled(false);
                mSwitchLightBtn.setImageResource(mLightDisableRes);
                mSwitchCameraBtn.setActivated(true);
            }
        } else if (v == mSwitchLightBtn) {
            if (mFlashType == FlashType.TORCH) {
                mFlashType = FlashType.OFF;
            } else {
                mFlashType = FlashType.TORCH;
            }


            switch (mFlashType) {
                case TORCH:
                    v.setSelected(true);
                    v.setActivated(false);
                    break;
                case OFF:
                    v.setSelected(true);
                    v.setActivated(true);
                    break;
                default:
                    break;
            }
            mRecorder.setLight(mFlashType);
        } else if (v == mBackBtn) {
            onBackPressed();
        } else if (v == mCompleteBtn) {
            if (mClipManager.getDuration() >= mClipManager.getMinDuration()) {
                toEditor();
            }
        } else if (v == mDeleteBtn) {
            if (!isSelected) {
                mRecordTimelineView.selectLast();
                mDeleteBtn.setActivated(true);
                isSelected = true;
            } else {
                mRecordTimelineView.deleteLast();
                mDeleteBtn.setActivated(false);
                mClipManager.deletePart();
                isRecordFinish = false;
                isSelected = false;
                showComplete();
                if (mClipManager.getDuration() == 0) {
                    if (isNeedGallery) {
                        mGalleryBtn.setVisibility(View.VISIBLE);
                    }
                    mCompleteBtn.setVisibility(View.GONE);
                    mDeleteBtn.setVisibility(View.GONE);
                }
            }
        } else if (v == mGalleryBtn) {
            Class crop = null;
            try {
                crop = Class.forName(ACTIVITY_NAME_CROP);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (crop == null) {
                Toast.makeText(this, R.string.aliyun_no_import_moudle, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!PermissionUtils.checkPermissionsGroup(this, permission)) {
                PermissionUtils.requestPermissions(this, permission, PERMISSION_REQUEST_CODE);
                return;
            }

            Intent intent = new Intent(this, crop);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_RESOLUTION, mResolutionMode);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_RATIO, mRatioMode);
            intent.putExtra(AliyunSnapVideoParam.NEED_RECORD, false);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_QUALITY, mVideoQuality);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_GOP, mGop);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_BITRATE, mBitrate);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_FRAMERATE, mFrame);
            intent.putExtra(AliyunSnapVideoParam.CROP_MODE, mCropMode);
            intent.putExtra(AliyunSnapVideoParam.MIN_CROP_DURATION, mMinCropDuration);
            intent.putExtra(AliyunSnapVideoParam.MIN_VIDEO_DURATION, mMinVideoDuration);
            intent.putExtra(AliyunSnapVideoParam.MAX_VIDEO_DURATION, mMaxVideoDuration);
            intent.putExtra(AliyunSnapVideoParam.SORT_MODE, mSortMode);
            intent.putExtra(AliyunSnapVideoParam.VIDEO_CODEC, mVideoCodec);
            if (!FastClickUtil.isFastClickActivity(ACTIVITY_NAME_CROP)) {
                startActivityForResult(intent, REQUEST_CROP);
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CROP && resultCode == RESULT_OK) {
            data.putExtra(RESULT_TYPE, RESULT_TYPE_CROP);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    private int getPictureRotation() {
        int orientation = mOrientationDetector.getOrientation();
        int rotation = 90;
        if ((orientation >= 45) && (orientation < 135)) {
            rotation = 180;
        }
        if ((orientation >= 135) && (orientation < 225)) {
            rotation = 270;
        }
        if ((orientation >= 225) && (orientation < 315)) {
            rotation = 0;
        }
        if (mCameraType == CameraType.FRONT) {
            if (rotation != 0) {
                rotation = 360 - rotation;
            }
        }
        Log.d("MyOrientationDetector", "generated rotation ..." + rotation);
        return rotation;
    }

    private void toEditor() {
        mIsToEditor = true;
        mRecorder.finishRecording();
    }

    private void toStitch() {
        mIsToEditor = false;
        mRecorder.finishRecording();
        AliyunIClipManager mClipManager = mRecorder.getClipManager();
        mClipManager.deleteAllPart();//删除所有的临时文件
    }

    /**
     * 权限申请
     */
    String[] permission = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final int PERMISSION_REQUEST_CODE = 1001;

    private void startRecording() {
        if (!PermissionUtils.checkPermissionsGroup(this, permission)) {
            PermissionUtils.requestPermissions(this, permission, PERMISSION_REQUEST_CODE);
            return;
        }


        File file = new File(VIDEOPATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        String fileName = VIDEOPATH + File.separator + System.currentTimeMillis() + ".mp4";
        User user = DataManager.getInstance().getUser();
        if (user != null) {
            fileName = VIDEOPATH + File.separator + "uid_" + user.getUserId() + "_dcode_" + user.getCodeId() + "_" + System.currentTimeMillis() + ".mp4";
        }

        mRecorder.setOutputPath(fileName);
        handleRecordStart();
        mRecorder.setRotation(getPictureRotation());
        isRecordError = false;
        if (!isRecordFinish) {
            mRecorder.startRecording();
            Log.d(TAG, "startRecording");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGranted = true;

        // 判断是否所有的权限都已经授予了
        for (int grant : grantResults) {
            if (grant != PackageManager.PERMISSION_GRANTED) {
                isAllGranted = false;
                break;
            }
        }

        if (!isAllGranted) {
            // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
            //showPermissionDialog();
            Toast.makeText(this, R.string.aliyun_camera_permission_tip, Toast.LENGTH_SHORT).show();
        }
    }

    //系统授权设置的弹框
    AlertDialog openAppDetDialog = null;

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.app_name) + "需要访问 \"相册\"、\"摄像头\" 和 \"外部存储器\",否则会影响绝大部分功能使用, 请到 \"应用信息 -> 权限\" 中设置！");
        builder.setPositiveButton("去设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setCancelable(false);
        builder.setNegativeButton("暂不设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //finish();
            }
        });
        if (null == openAppDetDialog) {
            openAppDetDialog = builder.create();
        }
        if (null != openAppDetDialog && !openAppDetDialog.isShowing()) {
            openAppDetDialog.show();
        }
    }


    private void stopRecording() {
        if (!isRecordFinish) {
            mRecorder.stopRecording();
        }
        handleRecordStop();
    }

    private boolean checkIfStartRecording() {
        if (mRecordBtn.isActivated()) {
            return false;
        }
        if (CommonUtil.SDFreeSize() < 50 * 1000 * 1000) {
            Toast.makeText(this, R.string.aliyun_no_free_memory, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showFilter(String name) {
        if (name == null || name.isEmpty()) {
            name = getString(R.string.aliyun_filter_null);
        }
        mFilterTxt.animate().cancel();
        mFilterTxt.setText(name);
        mFilterTxt.setVisibility(View.VISIBLE);
        mFilterTxt.setAlpha(FADE_IN_START_ALPHA);
        txtFadeIn();
    }

    private void txtFadeIn() {
        mFilterTxt.animate().alpha(1).setDuration(FILTER_ANIMATION_DURATION / 2).setListener(
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        txtFadeOut();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
    }

    private void txtFadeOut() {
        mFilterTxt.animate().alpha(0).setDuration(FILTER_ANIMATION_DURATION / 2).start();
        mFilterTxt.animate().setListener(null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mRecordBtn) {
            if (isOpenFailed) {
                Toast.makeText(this, R.string.aliyun_camera_permission_tip, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (mRecordMode == AliyunSnapVideoParam.RECORD_MODE_TOUCH) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isRecording) {
                        if (!checkIfStartRecording()) {
                            return false;
                        }
                        mRecordBtn.setHovered(true);
                        startRecording();
                        isRecording = true;
                    } else {
                        stopRecording();
                        isRecording = false;
                    }
                }
            } else if (mRecordMode == AliyunSnapVideoParam.RECORD_MODE_PRESS) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!checkIfStartRecording()) {
                        return false;
                    }
                    mRecordBtn.setSelected(true);
                    startRecording();
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    stopRecording();
                }
            } else if (mRecordMode == AliyunSnapVideoParam.RECORD_MODE_AUTO) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mDownTime = System.currentTimeMillis();
                    if (!isRecording) {
                        if (FastClickUtil.isFastClick()) {
                            return true;
                        }
                        if (!checkIfStartRecording()) {
                            return false;
                        }

                        mRecordBtn.setPressed(true);
                        startRecording();
                        mRecordBtn.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mRecordBtn.isPressed()) {
                                    mRecordBtn.setSelected(true);
                                    mRecordBtn.setHovered(true);
                                }
                            }
                        }, 200);
                        isRecording = true;
                    } else {
                        stopRecording();
                        isRecording = false;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    long timeOffset = System.currentTimeMillis() - mDownTime;
                    mRecordBtn.setPressed(false);
                    if (timeOffset > 1000) {
                        stopRecording();
                        isRecording = false;
                    } else {
                        if (!isRecordError) {
                            mRecordBtn.setSelected(false);
                            mRecordBtn.setHovered(true);
                        } else {
                            isRecording = false;
                        }
                    }
                }
            }
        } else if (v.equals(mSurfaceView)) {
            if (event.getPointerCount() >= 2) {
                scaleGestureDetector.onTouchEvent(event);
            } else if (event.getPointerCount() == 1) {
                gestureDetector.onTouchEvent(event);
            }


        }
        return true;
    }

    private void handleRecordStart() {
        mRecordBtn.setActivated(true);
        mGalleryBtn.setVisibility(View.GONE);
        mCompleteBtn.setVisibility(View.VISIBLE);
        mDeleteBtn.setVisibility(View.VISIBLE);
        mSwitchBeautyBtn.setEnabled(false);
        mSwitchCameraBtn.setEnabled(false);
        mSwitchLightBtn.setEnabled(false);
        mCompleteBtn.setEnabled(false);
        mDeleteBtn.setEnabled(false);
        mDeleteBtn.setActivated(false);
        isSelected = false;
    }

    private void handleRecordStop() {
        if (mFlashType == FlashType.ON && mCameraType == CameraType.BACK) {
            mRecorder.setLight(FlashType.OFF);
        }
    }


    private void showComplete() {
        if (mClipManager.getDuration() > mClipManager.getMinDuration()) {
            mCompleteBtn.setActivated(true);
        } else {
            mCompleteBtn.setActivated(false);
        }
    }

    private void handleRecordCallback(final boolean validClip, final long clipDuration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setActivated(false);
                mRecordBtn.setHovered(false);
                mRecordBtn.setSelected(false);
                if (validClip) {
                    mRecordTimelineView.setDuration((int) clipDuration);
                    mRecordTimelineView.clipComplete();
                } else {
                    mRecordTimelineView.setDuration(0);
                }
                Log.e("validClip", "validClip : " + validClip);
                mRecordTimeTxt.setVisibility(View.GONE);
                mSwitchBeautyBtn.setEnabled(true);
                mSwitchCameraBtn.setEnabled(true);
                mSwitchLightBtn.setEnabled(true);
                mCompleteBtn.setEnabled(true);
                mDeleteBtn.setEnabled(true);
                showComplete();
                isRecording = false;
            }
        });

    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factorOffset = detector.getScaleFactor() - mLastScaleFactor;
        mScaleFactor += factorOffset;
        mLastScaleFactor = detector.getScaleFactor();
        if (mScaleFactor < 0) {
            mScaleFactor = 0;
        }
        if (mScaleFactor > 1) {
            mScaleFactor = 1;
        }
        mRecorder.setZoom(mScaleFactor);
        return false;

    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mLastScaleFactor = detector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    int mTestFocusIndex;
    boolean isExposureShow;

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        isExposureShow = true;
        float x = e.getX();
        float y = e.getY();
        handleFocus(x, y);
        return false;
    }

    /**
     * 对焦
     *
     * @param x position x
     * @param y position y
     */
    private void handleFocus(float x, float y) {
        //        float[] testpoint = new float[] {0, 0,
        //                                         1, 0,
        //                                         0, 1,
        //                                         1, 1,
        //                                         0.5f, 0.5f,
        //                                         0.1f, 0.1f,
        //                                         0.5f, 0.8f};
        //        int index = mTestFocusIndex % 7 * 2;

        mRecorder.setFocus(x / mSurfaceView.getWidth(), y / mSurfaceView.getHeight());

        mIvRecordFocusView.showView();
        mIvRecordFocusView.setLocation(x, y);
        //        mTestFocusIndex++;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (isExposureShow) {
            mIvRecordFocusView.showView();
            handleCompensationRatio(distanceX, distanceY);
        }
        return false;
    }

    /**
     * 根据y方向滑动手势, 调整曝光度比例
     *
     * @param distanceY
     */
    private void handleCompensationRatio(float distanceX, float distanceY) {
        if (Math.abs(distanceX) > 20) {
            return;
        }
        mExposureCompensationRatio += (distanceY / mSurfaceView.getHeight());
        if (mExposureCompensationRatio > 1) {
            mExposureCompensationRatio = 1;
        }
        if (mExposureCompensationRatio < 0) {
            mExposureCompensationRatio = 0;
        }
        mRecorder.setExposureCompensationRatio(mExposureCompensationRatio);
        mIvRecordFocusView.setProgress(mExposureCompensationRatio, distanceY < 0);
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mFilterList == null || mFilterList.length == 0) {
            return true;
        }
        if (mRecordBtn.isActivated()) {
            return true;
        }
        if (velocityX > MAX_SWITCH_VELOCITY) {
            mFilterIndex++;
            if (mFilterIndex >= mFilterList.length) {
                mFilterIndex = 0;
            }
        } else if (velocityX < -MAX_SWITCH_VELOCITY) {
            mFilterIndex--;
            if (mFilterIndex < 0) {
                mFilterIndex = mFilterList.length - 1;
            }
        } else {
            return true;
        }
        EffectFilter effectFilter = new EffectFilter(mFilterList[mFilterIndex]);
        mRecorder.applyFilter(effectFilter);
        showFilter(effectFilter.getName());
        return false;
    }
}
