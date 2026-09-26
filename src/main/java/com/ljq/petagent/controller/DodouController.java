package com.ljq.petagent.controller;

import com.ljq.petagent.service.DodouService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DodouController {

    private final DodouService dodouService;

    public DodouController(DodouService dodouService) {
        this.dodouService = dodouService;
    }

    @PostMapping("/dodou/chat")
    public ResponseEntity<Map<String, Object>> chat(
        @RequestBody(required = false) Map<String, Object> body,
        HttpServletRequest request
    ) {
        Map<String, Object> data = body == null ? new LinkedHashMap<String, Object>() : body;
        String message = string(data.get("message")).trim();
        if (message.isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("ok", false);
            error.put("text", "豆豆没听清问题，再输入一次吧。");
            return ResponseEntity.badRequest().body(error);
        }

        List<Map<String, Object>> history = normalizeHistory(data.get("history"));
        if (dodouService.isWeatherIntent(message)) {
            String city = dodouService.extractCity(message);
            Coordinates coordinates = parseCoordinates(data);
            Map<String, Object> result = dodouService.weatherReply(
                city,
                city.isEmpty() ? coordinates.lat : null,
                city.isEmpty() ? coordinates.lon : null,
                clientIp(request)
            );
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("ok", true);
            response.put("kind", "weather");
            response.put("text", result.get("text"));
            return ResponseEntity.ok(response);
        }

        String text = dodouService.llmReply(message, history);
        if (text == null) {
            text = dodouService.diagnosisReply(message);
        }
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("kind", "ai");
        response.put("text", text);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dodou/weather")
    public Map<String, Object> weather(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lon,
        HttpServletRequest request
    ) {
        return dodouService.weatherReply(
            city == null ? "" : city.trim(),
            lat,
            lon,
            clientIp(request)
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeHistory(Object value) {
        List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
        if (!(value instanceof List)) {
            return history;
        }
        List<?> items = (List<?>) value;
        int start = Math.max(0, items.size() - 8);
        for (Object item : items.subList(start, items.size())) {
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                String role = string(map.get("role"));
                String content = string(map.get("content"));
                if (("user".equals(role) || "assistant".equals(role)) && !content.isEmpty()) {
                    history.add(map);
                }
            }
        }
        return history;
    }

    private Coordinates parseCoordinates(Map<String, Object> data) {
        Object locationValue = data.get("location");
        if (!(locationValue instanceof Map)) {
            return new Coordinates(null, null);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) locationValue;
        Double lat = number(location.get("lat"));
        Double lon = number(location.get("lon"));
        if (lat == null || lon == null || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return new Coordinates(null, null);
        }
        return new Coordinates(lat, lon);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? null : Double.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static class Coordinates {
        private final Double lat;
        private final Double lon;

        Coordinates(Double lat, Double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
