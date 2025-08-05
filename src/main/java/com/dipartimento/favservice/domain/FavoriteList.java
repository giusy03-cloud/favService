package com.dipartimento.favservice.domain;


import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "favorite_lists")
public class FavoriteList {

    @Id
    @GeneratedValue
    private UUID id;

    private Long ownerId;

    private String name;

    private Long sharedByUserId;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @ElementCollection
    private List<Long> eventIds = new ArrayList<>();

    @ElementCollection
    private List<Long> sharedWith = new ArrayList<>();

    private String capabilityToken;

    public enum Visibility {
        PRIVATE,
        SHARED,
        PUBLIC
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public void setSharedByUserId(Long sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public List<Long> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<Long> eventIds) {
        this.eventIds = eventIds;
    }

    public List<Long> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<Long> sharedWith) {
        this.sharedWith = sharedWith;
    }

    public String getCapabilityToken() {
        return capabilityToken;
    }

    public void setCapabilityToken(String capabilityToken) {
        this.capabilityToken = capabilityToken;
    }
}
