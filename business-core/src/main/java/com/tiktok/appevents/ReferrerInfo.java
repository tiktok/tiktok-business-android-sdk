package com.tiktok.appevents;

public class ReferrerInfo {
    private String googleInstallReferrer = "";
    private long gpReferrerInstallTs = 0l;
    private long gpReferrerClickTs = 0l;

    public ReferrerInfo(String googleInstallReferrer, long gpReferrerInstallTs, long gpReferrerClickTs) {
        this.googleInstallReferrer = googleInstallReferrer;
        this.gpReferrerInstallTs = gpReferrerInstallTs;
        this.gpReferrerClickTs = gpReferrerClickTs;
    }

    public String getGoogleInstallReferrer() {
        return googleInstallReferrer;
    }

    public void setGoogleInstallReferrer(String googleInstallReferrer) {
        this.googleInstallReferrer = googleInstallReferrer;
    }

    public long getGpReferrerInstallTs() {
        return gpReferrerInstallTs;
    }

    public void setGpReferrerInstallTs(long gpReferrerInstallTs) {
        this.gpReferrerInstallTs = gpReferrerInstallTs;
    }

    public long getGpReferrerClickTs() {
        return gpReferrerClickTs;
    }

    public void setGpReferrerClickTs(long gpReferrerClickTs) {
        this.gpReferrerClickTs = gpReferrerClickTs;
    }
}
