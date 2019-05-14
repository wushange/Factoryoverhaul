package com.wsg.factoryoverhaul.manager;

import com.blankj.utilcode.util.CacheDiskUtils;
import com.blankj.utilcode.util.SPUtils;
import com.wsg.factoryoverhaul.bean.User;

public class DataManager {

    private static volatile DataManager singleton;

    private DataManager() {
    }

    public static DataManager getInstance() {
        if (singleton == null) {
            synchronized (DataManager.class) {
                if (singleton == null) {
                    singleton = new DataManager();
                }
            }
        }
        return singleton;
    }


    public void putUser(User user) {
        CacheDiskUtils.getInstance().put("USER", user);
    }

    public User getUser() {
        return (User) CacheDiskUtils.getInstance().getSerializable("USER");
    }
}