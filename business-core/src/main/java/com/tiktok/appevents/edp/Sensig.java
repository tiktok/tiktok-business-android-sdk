package com.tiktok.appevents.edp;

import static com.tiktok.appevents.edp.EDPConfig.DEFAULT_SENSIG_FILTERING_REGEX_LIST;

import android.text.TextUtils;

public class Sensig {
    public String regexList;
    public int version;

    public Sensig(int version, String regexList) {
        this.version = version;
        this.regexList = regexList;
        if(TextUtils.isEmpty(regexList)){
            this.regexList = DEFAULT_SENSIG_FILTERING_REGEX_LIST;
        }
    }

    public String getRegexList() {
        return regexList;
    }

    public void setRegexList(String regexList) {
        this.regexList = regexList;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
