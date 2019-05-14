package com.wsg.factoryoverhaul;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.wsg.factoryoverhaul.bean.User;
import com.wsg.factoryoverhaul.manager.DataManager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.et_user_code)
    TextInputEditText etUserCode;
    @BindView(R.id.et_device_code)
    TextInputEditText etDeviceCode;
    @BindView(R.id.btn_start)
    Button btnStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ucode = etUserCode.getText().toString().trim();
                String dcode = etDeviceCode.getText().toString().trim();

                if (StringUtils.isEmpty(ucode) || StringUtils.isEmpty(dcode)) {
                    ToastUtils.showShort("请输入！");
                    return;
                }
                User user = new User(ucode, dcode);
                DataManager.getInstance().putUser(user);
                ActivityUtils.startActivity(MainActivity.class);
            }
        });
    }
}
