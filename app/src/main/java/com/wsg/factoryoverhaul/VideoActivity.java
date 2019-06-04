package com.wsg.factoryoverhaul;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.FileUtils;
import com.wsg.factoryoverhaul.bean.SpeedEvent;
import com.wsg.factoryoverhaul.view.MyIJKMediaSystem;
import com.wsg.factoryoverhaul.view.MyJZMediaSystem;
import com.wsg.factoryoverhaul.view.MyJZVideoPlayerStandard;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import cn.jzvd.JZVideoPlayer;

public class VideoActivity extends AppCompatActivity {
	String filePth = "";
	private MyJZVideoPlayerStandard mPlayerStandard;
	//系统播放器引擎
	MyJZMediaSystem mJZMediaSystem;
	MyIJKMediaSystem mIJKMediaSystem;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);
		mJZMediaSystem = new MyJZMediaSystem();
		mIJKMediaSystem = new MyIJKMediaSystem();
		mPlayerStandard = findViewById(R.id.videoplayer);
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				filePth = bundle.getString("PATH");
				mPlayerStandard.setUp(filePth, JZVideoPlayer.SCREEN_WINDOW_NORMAL, FileUtils.getFileName(filePth));
			}
		}
//		mPlayerStandard.backButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				finish();
//			}
//		});
//		mPlayerStandard.fullscreenButton.setVisibility(View.GONE);

	}


	@Override
	public void onBackPressed() {
		if (JZVideoPlayer.backPress()) {
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
		JZVideoPlayer.releaseAllVideos();
		JZVideoPlayer.setMediaInterface(mIJKMediaSystem);
		//Change these two variables back
		JZVideoPlayer.FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		JZVideoPlayer.NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	}


	@Override
	protected void onResume() {
		super.onResume();
		JZVideoPlayer.setMediaInterface(mIJKMediaSystem);
		initPlayer();
	}

	@Override
	protected void onStart() {
		super.onStart();
		//注册消息总线
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);

	}


	/**
	 * 设置屏幕方向
	 */
	private void initPlayer() {
		JZVideoPlayer.FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		JZVideoPlayer.NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * 倍速切换
	 */
	@Subscribe(threadMode = ThreadMode.POSTING)
	public void onMessageEventPostSpeed(SpeedEvent event) {
		mJZMediaSystem.setSpeeding(event.getSpeed());
		mIJKMediaSystem.setSpeeding(event.getSpeed());
		Toast.makeText(this, "正在切换倍速:" + event.getSpeed(), Toast.LENGTH_LONG).show();
	}


}
