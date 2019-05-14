package com.wsg.factoryoverhaul;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.aliyun.svideo.sdk.external.struct.common.CropKey;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraType;
import com.aliyun.svideo.sdk.external.struct.snap.AliyunSnapVideoParam;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.FileUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.wsg.factoryoverhaul.bean.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.wsg.factoryoverhaul.util.Contants.VIDEOPATH;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD = 2001;
    VideoAdapter videoAdapter;
    @BindView(R.id.rclview)
    RecyclerView rclview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        videoAdapter = new VideoAdapter(new ArrayList<File>());
        rclview.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        videoAdapter.bindToRecyclerView(rclview);
        videoAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Intent intent = new Intent(getBaseContext(), VideoActivity.class);
                intent.putExtra("PATH", videoAdapter.getItem(position).getPath());
                ActivityUtils.startActivity(intent);
            }
        });
        getData();
        AliyunSnapVideoParam a = new AliyunSnapVideoParam();
        a.setCameraType(CameraType.BACK);
        a.setMaxDuration(300 * 1000);
        AliyunVideoRecorder.startRecordForResult(this, REQUEST_RECORD, a);
    }

    private void getData() {
        List<File> files = FileUtils.listFilesInDir(VIDEOPATH);
        if (files != null && files.size() > 0) {
            videoAdapter.setNewData(files);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_RECORD) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                int type = data.getIntExtra(AliyunVideoRecorder.RESULT_TYPE, 0);
                if (type == AliyunVideoRecorder.RESULT_TYPE_CROP) {
                    String path = data.getStringExtra(CropKey.RESULT_KEY_CROP_PATH);
                    Toast.makeText(this, "文件路径为 " + path + " 时长为 " + data.getLongExtra(CropKey.RESULT_KEY_DURATION, 0), Toast.LENGTH_SHORT).show();
                } else if (type == AliyunVideoRecorder.RESULT_TYPE_RECORD) {
                    Toast.makeText(this, "文件路径为 " + data.getStringExtra(AliyunVideoRecorder.OUTPUT_PATH),
                            Toast.LENGTH_SHORT).show();
                }
                getData();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "用户取消录制", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
