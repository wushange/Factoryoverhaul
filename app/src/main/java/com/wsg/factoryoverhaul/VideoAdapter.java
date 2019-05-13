package com.wsg.factoryoverhaul;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.List;

public class VideoAdapter extends BaseQuickAdapter<File, BaseViewHolder> {
    public VideoAdapter(@Nullable List<File> data) {
        super(R.layout.recycler_view_item, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, File item) {
        helper.setText(R.id.textView1, FileUtils.getFileName(item));
        helper.setText(R.id.textView2, FileUtils.getFileSize(item));
        helper.setText(R.id.textView3, "" + TimeUtils.millis2String(FileUtils.getFileLastModified(item)));
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(item.getPath());
        Bitmap bitmap = media.getFrameAtTime();
        helper.setImageBitmap(R.id.imageview, bitmap);

    }
}
