package com.wsg.factoryoverhaul.bean;

import java.io.Serializable;

public class User implements Serializable {
    int userId;
    int codeId;

    public User(int userId, int codeId) {
        this.userId = userId;
        this.codeId = codeId;
    }

    public User(String userId, String codeId) {
        this.userId = Integer.parseInt(userId);
        this.codeId = Integer.parseInt(codeId);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCodeId() {
        return codeId;
    }

    public void setCodeId(int codeId) {
        this.codeId = codeId;
    }
}
