package com.qa.qa_orchestrator_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight ping endpoint for cron job keep-alive.
 * Does NOT touch the database — just returns OK.
 * Used by cron-job.org to keep Render from sleeping.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}