package com.dipartimento.favservice.repository;

import com.dipartimento.favservice.domain.FavoriteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, UUID> {
    List<FavoriteList> findByOwnerId(Long ownerId);
    Optional<FavoriteList> findByCapabilityToken(String token);

    List<FavoriteList> findByVisibility(String visibility);

    // Assuming sharedWith is a collection of Long userIds
    List<FavoriteList> findBySharedWithContains(Long userId);
}

