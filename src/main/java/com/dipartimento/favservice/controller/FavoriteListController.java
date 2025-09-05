package com.dipartimento.favservice.controller;

import com.dipartimento.favservice.domain.FavoriteList;
import com.dipartimento.favservice.dto.*;
import com.dipartimento.favservice.service.FavoriteListService;
import com.dipartimento.favservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import com.dipartimento.favservice.service.FavoriteListService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteListController {

    private static final Logger log = LoggerFactory.getLogger(FavoriteListController.class);

    @Autowired
    private FavoriteListService service;

    @Autowired
    private JwtUtil jwtUtil;

    public FavoriteListController() {
        System.out.println("FavoriteListController loaded!");
    }

    @GetMapping("/test")
    public String test() {
        System.out.println("Test endpoint called");
        return "OK";
    }

    @PostMapping("/lists")
    public ResponseEntity<?> createList(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FavoriteListRequest request) {
        try {
            Long userId = jwtUtil.extractUserId(authHeader);
            FavoriteList createdList = service.createList(userId, request);
            return ResponseEntity.ok(createdList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore: " + e.getMessage());
        }
    }

    @GetMapping("/lists")
    public ResponseEntity<?> getMyLists(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);

        List<FavoriteList> existingLists = service.getMyLists(userId);

        // SE NON HA LISTE, CREA LE 3 DI DEFAULT
        if (existingLists.isEmpty()) {
            service.createDefaultListsForUser(userId);
            existingLists = service.getMyLists(userId);
        }

        return ResponseEntity.ok(existingLists);
    }


    @GetMapping("/lists/{id}")
    public ResponseEntity<?> getList(@RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
        Long userId = jwtUtil.extractUserId(authHeader);
        return service.getById(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping("/lists/{id}/events/{eventId}")
    public ResponseEntity<EventResponseDTO> addEvent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @PathVariable Long eventId) {
        Long userId = jwtUtil.extractUserId(authHeader);
        EventResponseDTO addedEvent = service.addEvent(id, userId, eventId);
        return ResponseEntity.ok(addedEvent);
    }

    @DeleteMapping("/lists/{id}/events/{eventId}")
    public ResponseEntity<String> removeEvent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @PathVariable Long eventId) {
        Long userId = jwtUtil.extractUserId(authHeader);
        service.removeEvent(id, userId, eventId);
        return ResponseEntity.ok("{\"message\": \"Event removed from favorite list\"}");
    }

    @DeleteMapping("/lists/{id}")
    public ResponseEntity<?> deleteList(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        Long userId = jwtUtil.extractUserId(authHeader);
        service.deleteList(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<?> getPublicList(@PathVariable String token) {
        log.info("Received request to get public list with token: {}", token);

        return service.getPublicByToken(token)
                .map(list -> {
                    log.info("Found favorite list: id={}, name={}", list.getId(), list.getName());
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> {
                    log.warn("No favorite list found for token: {}", token);
                    return ResponseEntity.notFound().build();
                });
    }


    @GetMapping("/lists/public")
    public ResponseEntity<List<FavoriteList>> getAllPublicLists() {
        List<FavoriteList> lists = service.getAllPublicLists();
        return ResponseEntity.ok(lists);
    }


    @GetMapping("/public/{token}/with-events")
    public ResponseEntity<?> getPublicListWithEvents(@PathVariable String token) {
        log.info("Ricevuta richiesta lista pubblica con eventi per token: {}", token);

        Optional<FavoriteList> listOpt = service.getPublicByToken(token);
        if (listOpt.isEmpty()) {
            log.warn("Nessuna lista trovata per token: {}", token);
            return ResponseEntity.notFound().build();
        }

        FavoriteList list = listOpt.get();
        log.info("Lista trovata: id={}, name={}, eventIds={}", list.getId(), list.getName(), list.getEventIds());

        List<EventResponseDTO> events;
        try {
            // Passo null perché è pubblico e non hai header di auth
            events = service.getEventsByIdsWithAuth(list.getEventIds(), null);
            log.info("Numero eventi recuperati: {}", events.size());
        } catch (Exception e) {
            log.error("Errore recupero eventi per lista pubblica token {}: {}", token, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nel recupero degli eventi: " + e.getMessage());
        }

        List<EventDTO> eventDTOs = events.stream().map(resp -> {
            EventDTO dto = new EventDTO();
            dto.setId(resp.getId());
            dto.setName(resp.getName());
            dto.setStartDate(resp.getStartDate());
            dto.setEndDate(resp.getEndDate());
            dto.setLocation(resp.getLocation());
            dto.setDescription(resp.getDescription());
            dto.setPrice(resp.getPrice());
            dto.setCapacity(resp.getCapacity());
            dto.setStatus(resp.getStatus());
            dto.setOrganizerId(resp.getOrganizerId());
            return dto;
        }).collect(Collectors.toList());

        FavoriteListWithEventDetailsDTO dto = new FavoriteListWithEventDetailsDTO();
        dto.setFavoriteList(list);
        dto.setEvents(eventDTOs);

        log.info("Risposta composta correttamente per token: {}", token);
        return ResponseEntity.ok(dto);
    }





    @PutMapping("/lists/{id}/shared-with")
    public ResponseEntity<?> updateSharedWith(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @RequestBody List<Long> sharedWith) {
        Long userId = jwtUtil.extractUserId(authHeader);
        log.info("updateSharedWith called for listId={} by userId={}", id, userId);
        log.info("SharedWith list received: {}", sharedWith);

        try {
            service.updateSharedWith(id, userId, sharedWith);
            log.info("SharedWith updated successfully");
            return ResponseEntity.ok(Map.of("message", "Shared users updated"));
        } catch (Exception e) {
            log.error("Error updating sharedWith", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized or invalid user ids");
        }
    }



    @GetMapping("/lists/shared-with-me")
    public ResponseEntity<List<FavoriteList>> getSharedWithMe(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        List<FavoriteList> lists = service.getSharedWithMe(userId);
        return ResponseEntity.ok(lists);
    }

    // Alias compatibile
    @GetMapping("/shared-with-me")
    public ResponseEntity<?> getSharedWithMeAlias(@RequestHeader("Authorization") String authHeader) {
        return getSharedWithMe(authHeader);
    }

    @GetMapping("/lists/{id}/with-events")
    public ResponseEntity<?> getListWithEvents(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {

        Long userId = jwtUtil.extractUserId(authHeader);
        var favoriteListOpt = service.getById(id, userId);
        if (favoriteListOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FavoriteList favoriteList = favoriteListOpt.get();
        List<Long> eventIds = favoriteList.getEventIds();
        List<EventResponseDTO> events = service.getEventsByIdsWithAuth(eventIds, authHeader);

        Map<String, Object> response = Map.of(
                "favoriteList", favoriteList,
                "events", events
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getListWithDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {

        Long userId = jwtUtil.extractUserId(authHeader);
        var favoriteListOpt = service.getById(id, userId);
        if (favoriteListOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FavoriteList favoriteList = favoriteListOpt.get();
        List<Long> eventIds = favoriteList.getEventIds();
        List<EventResponseDTO> responseDTOs = service.getEventsByIdsWithAuth(eventIds, authHeader);

        // 👇 Conversione da EventResponseDTO a EventDTO (puoi usare un mapper dedicato)

        List<EventDTO> eventDTOs = responseDTOs.stream().map(resp -> {
            EventDTO dto = new EventDTO();
            dto.setId(resp.getId());
            dto.setName(resp.getName()); // ✅ invece di setTitle
            dto.setStartDate(resp.getStartDate()); // ✅ invece di setDate
            dto.setEndDate(resp.getEndDate());
            dto.setLocation(resp.getLocation());
            dto.setDescription(resp.getDescription());
            dto.setPrice(resp.getPrice());
            dto.setCapacity(resp.getCapacity());
            dto.setStatus(resp.getStatus());
            dto.setOrganizerId(resp.getOrganizerId());
            return dto;
        }).collect(Collectors.toList());


        FavoriteListWithEventDetailsDTO dto = new FavoriteListWithEventDetailsDTO();
        dto.setFavoriteList(favoriteList);
        dto.setEvents(eventDTOs);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/lists/events/byIds")
    public ResponseEntity<List<EventResponseDTO>> getEventsByIds(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<Long> ids) {
        List<EventResponseDTO> events = service.getEventsByIdsWithAuth(ids, authHeader);
        return ResponseEntity.ok(events);
    }




    @GetMapping("/lists/shared-with-me/with-events")
    public ResponseEntity<?> getSharedWithMeWithEvents(@RequestHeader("Authorization") String authHeader) {
        System.out.println("Entrato in getSharedWithMeWithEvents");
        log.info("Entrato in getSharedWithMeWithEvents");
        Long userId = jwtUtil.extractUserId(authHeader);
        log.info("UserId estratto dal token: {}", userId);

        List<FavoriteList> sharedLists = service.getSharedWithMe(userId);
        log.info("Liste condivise trovate: {}", sharedLists.size());

        List<FavoriteListWithEventDetailsDTO> result = sharedLists.stream().map(list -> {
            log.info("Processando lista id={} name={}", list.getId(), list.getName());
            List<EventResponseDTO> events = service.getEventsByIdsWithAuth(list.getEventIds(), authHeader);
            log.info("Eventi trovati per lista {}: {}", list.getId(), events.size());

            FavoriteListWithEventDetailsDTO dto = new FavoriteListWithEventDetailsDTO();
            dto.setFavoriteList(list);

            List<EventDTO> eventDTOs = events.stream().map(resp -> {
                EventDTO dtoEvent = new EventDTO();
                dtoEvent.setId(resp.getId());
                dtoEvent.setName(resp.getName());
                dtoEvent.setStartDate(resp.getStartDate());
                dtoEvent.setEndDate(resp.getEndDate());
                dtoEvent.setLocation(resp.getLocation());
                dtoEvent.setDescription(resp.getDescription());
                dtoEvent.setPrice(resp.getPrice());
                dtoEvent.setCapacity(resp.getCapacity());
                dtoEvent.setStatus(resp.getStatus());
                dtoEvent.setOrganizerId(resp.getOrganizerId());
                return dtoEvent;
            }).collect(Collectors.toList());

            dto.setEvents(eventDTOs);

            // Aggiungi qui la chiamata per settare ownerUsername
            String ownerUsername = service.getUserNameById(list.getOwnerId());
            dto.setOwnerUsername(ownerUsername);

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }



    @GetMapping("/lists/{id}/owner-name")
    public ResponseEntity<String> getListOwnerName(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {

        try {
            Long userId = jwtUtil.extractUserId(authHeader); // Verifica che l'utente sia autenticato
            Optional<FavoriteList> listOpt = service.getById(id, userId);

            if (listOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lista non trovata o non accessibile");
            }

            FavoriteList list = listOpt.get();
            Long ownerId = list.getOwnerId();  // 👈 prendi l’owner

            String ownerName = service.getUserNameById(ownerId); // 👈 chiama l'altro servizio passando l'id
            return ResponseEntity.ok(ownerName);

        } catch (Exception e) {
            log.error("Errore nel recupero del nome del proprietario della lista {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nel recupero del nome del proprietario");
        }
    }

    @GetMapping("/lists/{id}/with-owner")
    public ResponseEntity<?> getListWithOwner(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {

        log.info("Chiamato endpoint /lists/{}/with-owner", id);

        try {
            FavoriteListWithOwnerDTO dto = service.getFavoriteListWithOwnerAndSharedBy(id);
            log.info("DTO recuperato: {}", dto);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.error("Errore nel recupero lista con owner: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/my/{userId}")
    public ResponseEntity<List<FavoriteListWithOwnerDTO>> getMyLists(@PathVariable Long userId) {
        List<FavoriteListWithOwnerDTO> lists = service.getMyListsWithOwner(userId);
        return ResponseEntity.ok(lists);
    }












}
