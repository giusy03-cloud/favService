package com.dipartimento.favservice.controller;

import com.dipartimento.favservice.domain.FavoriteList;
import com.dipartimento.favservice.dto.EventDTO;
import com.dipartimento.favservice.dto.EventResponseDTO;
import com.dipartimento.favservice.dto.FavoriteListRequest;
import com.dipartimento.favservice.service.FavoriteListService;
import com.dipartimento.favservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        return ResponseEntity.ok(service.getMyLists(userId));
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
    @PutMapping("/lists/{id}/shared-with")
    public ResponseEntity<?> updateSharedWith(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @RequestBody List<Long> sharedWith) {

        Long userId = jwtUtil.extractUserId(authHeader);
        try {
            service.updateSharedWith(id, userId, sharedWith);
            return ResponseEntity.ok(Map.of("message", "Shared users updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized or invalid user ids");
        }
    }

    @GetMapping("/lists/shared-with-me")
    public ResponseEntity<?> getSharedWithMe(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        return ResponseEntity.ok(service.getSharedWithMe(userId));

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

        // Usa il nuovo metodo passando l'Authorization header
        List<EventResponseDTO> events = service.getEventsByIdsWithAuth(eventIds, authHeader);

        Map<String, Object> response = Map.of(
                "favoriteList", favoriteList,
                "events", events
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/lists/events/byIds")
    public ResponseEntity<List<EventResponseDTO>> getEventsByIds(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<Long> ids) {

        List<EventResponseDTO> events = service.getEventsByIdsWithAuth(ids, authHeader);
        return ResponseEntity.ok(events);
    }




}
