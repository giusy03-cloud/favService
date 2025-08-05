package com.dipartimento.favservice.service;

import com.dipartimento.favservice.config.EventClient;
import com.dipartimento.favservice.domain.FavoriteList;
import com.dipartimento.favservice.dto.EventDTO;
import com.dipartimento.favservice.dto.EventResponseDTO;
import com.dipartimento.favservice.dto.FavoriteListRequest;
import com.dipartimento.favservice.dto.UsersAccountsDTO;
import com.dipartimento.favservice.repository.FavoriteListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import com.dipartimento.favservice.dto.FavoriteListWithOwnerDTO;


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
        Optional<FavoriteList> listOpt = repository.findByCapabilityToken(token)
                .filter(list -> list.getVisibility() == FavoriteList.Visibility.PUBLIC);

        listOpt.ifPresentOrElse(
                list -> log.info("Lista pubblica trovata per token {}", token),
                () -> log.warn("Lista trovata per token {} ma non è pubblica", token)
        );

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
        list.setSharedByUserId(ownerId);
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


    // il metodo che hai già scritto
    public void createDefaultListsForUser(Long userId) {
        List<FavoriteList> defaultLists = List.of(
                buildList("Preferiti Privati", FavoriteList.Visibility.PRIVATE, userId),
                buildList("Preferiti Condivisi", FavoriteList.Visibility.SHARED, userId),
                buildList("Preferiti Pubblici", FavoriteList.Visibility.PUBLIC, userId)
        );

        repository.saveAll(defaultLists);
    }

    private FavoriteList buildList(String name, FavoriteList.Visibility visibility, Long ownerId) {
        FavoriteList list = new FavoriteList();
        list.setName(name);
        list.setVisibility(visibility);
        list.setOwnerId(ownerId);
        list.setCapabilityToken(UUID.randomUUID().toString()); // utile se vuoi link pubblici
        return list;
    }



    public String getUserNameById(Long userId) {
        try {

            String userName = userClient.get()
                    .uri("/api/users/{id}/name", userId)
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Risposta userName: '{}'", userName);



            if (userName == null || userName.isEmpty()) {
                log.warn("Nome utente non trovato per id: {}", userId);
                return "Unknown";
            }

            log.info("Nome utente recuperato per id {}: {}", userId, userName);
            return userName;

        } catch (Exception e) {
            log.error("Errore recupero nome utente per id: {}", userId, e);
            return "Unknown";
        }
    }




    public FavoriteListWithOwnerDTO getFavoriteListWithOwnerAndSharedBy(UUID listId) {
        FavoriteList list = repository.findById(listId)
                .orElseThrow(() -> new RuntimeException("FavoriteList not found"));

        FavoriteListWithOwnerDTO dto = new FavoriteListWithOwnerDTO();
        dto.setFavoriteList(list);

        UsersAccountsDTO owner = new UsersAccountsDTO();
        owner.setName(getUserNameById(list.getOwnerId()));
        dto.setOwner(owner);

        if (list.getSharedByUserId() != null) {
            UsersAccountsDTO sharedBy = new UsersAccountsDTO();
            sharedBy.setName(getUserNameById(list.getSharedByUserId()));
            dto.setSharedBy(sharedBy);
        }

        return dto;
    }





    private UsersAccountsDTO getUserAccountDTO(Long userId) {
        log.info("Chiamata getUserAccountDTO per userId={}", userId);
        try {
            String userName = userClient.get()
                    .uri("/api/users/{id}/name", userId)
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Nome ricevuto per user {}: {}", userId, userName);

            if (userName == null || userName.isEmpty()) {
                log.warn("Utente non trovato per id: {}", userId);
                return createUnknownUserDTO();
            }

            UsersAccountsDTO dto = new UsersAccountsDTO();
            dto.setId(userId);
            dto.setName(userName);
            return dto;

        } catch (Exception e) {
            log.error("Errore recuperando nome utente per id: {}", userId, e);
            return createUnknownUserDTO();
        }
    }

    private UsersAccountsDTO createUnknownUserDTO() {
        UsersAccountsDTO dto = new UsersAccountsDTO();
        dto.setId(null);
        dto.setName("Unknown");
        return dto;
    }












}
