package com.example.kuohsuan.camera2mediacodec.bean;

import java.io.Serializable;

/**
 * Created by kuohsuan on 2018/2/2.
 */

public class ConnectResultBean implements Serializable {

    private String hostName;
    private int listenPort;
    private boolean isPassive;
    private String passiveIp;
    private int passivePort;
    private String passiveAccount;
    private String passivePassword;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }


    public boolean isPassive() {
        return isPassive;
    }

    public void setPassive(boolean passive) {
        isPassive = passive;
    }

    public String getPassiveIp() {
        return passiveIp;
    }

    public void setPassiveIp(String passiveIp) {
        this.passiveIp = passiveIp;
    }

    public int getPassivePort() {
        return passivePort;
    }

    public void setPassivePort(int passivePort) {
        this.passivePort = passivePort;
    }

    public String getPassiveAccount() {
        return passiveAccount;
    }

    public void setPassiveAccount(String passiveAccount) {
        this.passiveAccount = passiveAccount;
    }

    public String getPassivePassword() {
        return passivePassword;
    }

    public void setPassivePassword(String passivePassword) {
        this.passivePassword = passivePassword;
    }
}

