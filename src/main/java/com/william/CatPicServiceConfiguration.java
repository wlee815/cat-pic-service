package com.william;

import io.dropwizard.Configuration;

public class CatPicServiceConfiguration extends Configuration {
    private int maxCatPicFileSizeMb;

    public int getMaxCatPicFileSizeMb() {
        return maxCatPicFileSizeMb;
    }

    public void setMaxCatPicFileSizeMb(int maxCatPicFileSizeMb) {
        this.maxCatPicFileSizeMb = maxCatPicFileSizeMb;
    }
}
