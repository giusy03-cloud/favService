package com.dipartimento.favservice.dto;



import com.dipartimento.favservice.domain.FavoriteList;

import java.util.List;

public class FavoriteListWithEventDetailsDTO {
    private FavoriteList favoriteList;
    private List<EventDTO> events;

    // Getters & Setters
    public FavoriteList getFavoriteList() {
        return favoriteList;
    }

    public void setFavoriteList(FavoriteList favoriteList) {
        this.favoriteList = favoriteList;
    }

    public List<EventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<EventDTO> events) {
        this.events = events;
    }
}
