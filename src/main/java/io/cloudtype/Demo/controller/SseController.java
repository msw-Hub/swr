package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
public class SseController {
    private final SseService sseService;

    @Autowired
    public SseController(SseService sseService) {
        this.sseService = sseService;
    }
    @GetMapping("/match-events")
    public SseEmitter handleSse(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            return sseService.handleSse(accessToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
