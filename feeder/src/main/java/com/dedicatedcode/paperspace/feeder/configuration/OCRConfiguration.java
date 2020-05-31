package com.dedicatedcode.paperspace.feeder.configuration;

public class OCRConfiguration {
    private String datapath;
    private String language;

    public void setDatapath(String datapath) {
        this.datapath = datapath;
    }

    public String getDatapath() {
        return datapath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
