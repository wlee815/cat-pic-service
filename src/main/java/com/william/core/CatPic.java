package com.william.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class CatPic {

    @NotNull
    @JsonProperty
    private Integer id;

    @NotNull
    @JsonProperty
    private String filePath;

    public CatPic() {
        // Jackson deserialization
    }

    public CatPic(String filePath) {
        this.filePath = filePath;
    }

    public CatPic(int id, String filePath) {
        this.id = id;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getId() {
        return id;
    }
}
