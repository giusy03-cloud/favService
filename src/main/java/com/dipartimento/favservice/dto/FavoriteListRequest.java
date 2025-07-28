package com.dipartimento.favservice.dto;


import java.util.List;

public class FavoriteListRequest {
    private String name;
    private String visibility;
    private List<Long> sharedWith; // solo se visibility == SHARED

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<Long> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<Long> sharedWith) {
        this.sharedWith = sharedWith;
    }
}
