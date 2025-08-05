package com.dipartimento.favservice.dto;

import com.dipartimento.favservice.domain.FavoriteList;

public class FavoriteListWithOwnerDTO {
    private FavoriteList favoriteList;  // o un DTO equivalente della lista
    private UsersAccountsDTO owner;
    private UsersAccountsDTO sharedBy;

    public FavoriteListWithOwnerDTO() {}

    public FavoriteList getFavoriteList() {
        return favoriteList;
    }

    public void setFavoriteList(FavoriteList favoriteList) {
        this.favoriteList = favoriteList;
    }

    public UsersAccountsDTO getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(UsersAccountsDTO sharedBy) {
        this.sharedBy = sharedBy;
    }

    public UsersAccountsDTO getOwner() {
        return owner;
    }

    public void setOwner(UsersAccountsDTO owner) {
        this.owner = owner;
    }
}
