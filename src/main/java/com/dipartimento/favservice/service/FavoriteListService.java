package com.dipartimento.favservice.service;

import com.dipartimento.favservice.config.EventClient;
import com.dipartimento.favservice.domain.FavoriteList;
import com.dipartimento.favservice.dto.EventDTO;
import com.dipartimento.favservice.dto.EventResponseDTO;
import com.dipartimento.favservice.dto.FavoriteListRequest;
import com.dipartimento.favservice.repository.FavoriteListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;



@Service
public class FavoriteListService {

    private final FavoriteListRepository repository;
    private final WebClient userClient;
    private final EventClient eventClient; // 👈 Usa la classe custom EventClient
    private static final Logger log = LoggerFactory.getLogger(FavoriteListService.class);

    @Autowired
    public FavoriteListService(FavoriteListRepository repository,
                               @Qualifier("userClient") WebClient userClient,
                               EventClient eventClient) {
        this.repository = repository;
        this.userClient = userClient;
        this.eventClient = eventClient;
    }

    public FavoriteList createList(Long ownerId, FavoriteListRequest req) {
        for (Long sharedUserId : req.getSharedWith()) {
            Boolean exists = userClient.get()
                    .uri("/api/users/{id}/exists", sharedUserId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (exists == null || !exists) {
                throw new IllegalArgumentException("UserId " + sharedUserId + " does not exist");
            }
        }

        FavoriteList list = new FavoriteList();
        list.setName(req.getName());
        list.setOwnerId(ownerId);
        list.setVisibility(FavoriteList.Visibility.valueOf(req.getVisibility().toUpperCase()));

        if (list.getVisibility() == FavoriteList.Visibility.SHARED) {
            list.setSharedWith(req.getSharedWith());
        }

        if (list.getVisibility() == FavoriteList.Visibility.PUBLIC) {
            list.setCapabilityToken(UUID.randomUUID().toString());
        }

        return repository.save(list);
    }

    public List<FavoriteList> getMyLists(Long userId) {
        return repository.findByOwnerId(userId);
    }

    public Optional<FavoriteList> getById(UUID id, Long requesterId) {
        return repository.findById(id)
                .filter(list -> {
                    if (list.getVisibility() == FavoriteList.Visibility.PRIVATE)
                        return list.getOwnerId().equals(requesterId);
                    if (list.getVisibility() == FavoriteList.Visibility.SHARED)
                        return list.getOwnerId().equals(requesterId) || list.getSharedWith().contains(requesterId);
                    return true;
                });
    }

    public Optional<FavoriteList> getPublicByToken(String token) {
        Optional<FavoriteList> listOpt = repository.findByCapabilityToken(token);
        listOpt.ifPresent(list -> System.out.println("Event IDs: " + list.getEventIds()));
        return listOpt;
    }

    public FavoriteList getListByIdAndUser(UUID listId, Long userId) {
        FavoriteList list = repository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista non trovata"));

        if (!list.getOwnerId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        return list;
    }

    public EventResponseDTO addEvent(UUID listId, Long userId, Long eventId) {
        FavoriteList list = getListByIdAndUser(listId, userId);

        // Controllo che l'evento esista
        EventResponseDTO event;
        try {
            event = eventClient.getEventById(eventId);
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento non trovato con ID: " + eventId);
        }

        // Se esiste, aggiungo e salvo
        list.getEventIds().add(eventId);
        repository.save(list);

        return event;
    }



    public void removeEvent(UUID listId, Long userId, Long eventId) {
        FavoriteList list = repository.findById(listId).orElseThrow();
        if (!list.getOwnerId().equals(userId)) throw new RuntimeException("Unauthorized");
        list.getEventIds().remove(eventId);
        repository.save(list);
    }

    public void deleteList(UUID listId, Long userId) {
        FavoriteList list = repository.findById(listId).orElseThrow();
        if (!list.getOwnerId().equals(userId)) throw new RuntimeException("Unauthorized");
        repository.delete(list);
    }


    public void updateSharedWith(UUID listId, Long ownerId, List<Long> sharedWith) {
        FavoriteList list = repository.findById(listId)
                .orElseThrow(() -> new RuntimeException("FavoriteList not found"));

        if (!list.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        for (Long userId : sharedWith) {
            try {

                log.info("Verifica esistenza per userId={}", userId);

                Boolean exists = userClient.get()
                        .uri("/api/users/{id}/exists", userId)
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .block();

                log.info("Risultato per userId {}: {}", userId, exists);


                if (exists == null || !exists) {
                    throw new RuntimeException("UserId " + userId + " does not exist");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error checking user existence for userId " + userId + ": " + e.getMessage());
            }
        }

        list.setSharedWith(sharedWith);
        repository.save(list);
    }



    public List<EventResponseDTO> getEventsByIdsWithAuth(List<Long> eventIds, String authHeader) {
        return eventClient.getEventsByIds(eventIds, authHeader);
    }


    public List<FavoriteList> getAllPublicLists() {
        return repository.findByVisibility("PUBLIC");
    }

    public List<FavoriteList> getSharedWithMe(Long userId) {
        return repository.findBySharedWithContains(userId);
    }


}
