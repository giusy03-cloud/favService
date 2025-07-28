package com.dipartimento.favservice.config;




import com.dipartimento.favservice.dto.EventDTO;
import com.dipartimento.favservice.dto.EventResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class EventClient {

    private final WebClient eventClient;

    public EventClient(@Qualifier("eventWebClient") WebClient eventClient) {
        this.eventClient = eventClient;
    }


    public EventResponseDTO getEventById(Long eventId) {
        return eventClient.get()
                .uri("/events/public/{id}", eventId)
                .retrieve()
                .bodyToMono(EventResponseDTO.class)
                .block();
    }

    public List<EventResponseDTO> getEventsByIds(List<Long> eventIds, String authHeader) {
        return eventClient.post()
                .uri("/events/public/byIds")
                .header("Authorization", authHeader)
                .bodyValue(eventIds)
                .retrieve()
                .bodyToFlux(EventResponseDTO.class)
                .collectList()
                .block();
    }



}

