package com.parking.backend.manager;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    @GetMapping("/ping")
    public Map<String, String> ping(Authentication auth) {
        return Map.of("message", "Manager area", "user", auth.getName());
    }
}