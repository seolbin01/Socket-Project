package com.benny.socketApp;

import com.benny.socketApp.service.WeatherService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<WebSocketSession, String>> weatherRooms = new ConcurrentHashMap<>();
    private final Map<String, String> locationWeatherCache = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionWeatherMap = new ConcurrentHashMap<>();

    private final WeatherService weatherService;

    @Autowired
    public ChatWebSocketHandler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 시 처리는 사용자가 날씨와 이름을 보낼 때 수행
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> msg = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {});
            String type = (String) msg.get("type");
            logger.info("Received message of type: {}", type);

            switch (type) {
                case "join":
                    handleJoin(session, msg);
                    break;
                case "chat":
                    handleChat(session, msg);
                    break;
                default:
                    logger.warn("Unhandled message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling message: {}", message.getPayload(), e);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "error", "message", "An error occurred while processing your request."))));
        }
    }

    private void handleJoin(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String username = (String) msg.get("username");
        String weather;

        if (msg.containsKey("weather")) {
            // 클라이언트에서 선택한 위치의 날씨 정보를 사용
            Map<String, Double> selectedWeather = (Map<String, Double>) msg.get("weather");
            if (selectedWeather != null && selectedWeather.containsKey("lat") && selectedWeather.containsKey("lon")) {
                double lat = selectedWeather.get("lat");
                double lon = selectedWeather.get("lon");
                weather = weatherService.getWeather(lat, lon);
            } else {
                weather = "Unknown";
            }
        } else if (msg.containsKey("lat") && msg.containsKey("lon")) {
            double lat = ((Number) msg.get("lat")).doubleValue();
            double lon = ((Number) msg.get("lon")).doubleValue();
            weather = weatherService.getWeather(lat, lon);
        } else {
            weather = "Unknown";
        }

        weatherRooms.computeIfAbsent(weather, k -> new ConcurrentHashMap<>()).put(session, username);
        sessionWeatherMap.put(session, weather);  // 세션과 날씨 매핑

        // 입장 메시지 브로드캐스트
        broadcastToRoom(weather, createMessage("시스템", username + "님이 방을 입장하였습니다. 현재 날씨: " + weather, new Date()));

        // 참여자 목록 업데이트
        updateParticipants(weather);

        // 클라이언트에게 할당된 날씨 정보 전송
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "weather", "weather", weather))));
    }

    private void handleChat(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String weather = sessionWeatherMap.get(session);
        if (weather == null) {
            logger.warn("No weather found for session: {}", session.getId());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "error", "message", "You are not in a chat room. Please join first.")
            )));
            return;
        }

        Map<WebSocketSession, String> room = weatherRooms.get(weather);
        String username = room.get(session);
        String content = (String) msg.get("content");

        broadcastToRoom(weather, createMessage(username, content, new Date()));
    }

    private String getWeatherForSession(WebSocketSession session) {
        return sessionWeatherMap.get(session);
    }

    private void broadcastToRoom(String weather, String message) throws IOException {
        Map<WebSocketSession, String> room = weatherRooms.get(weather);
        if (room != null) {
            for (WebSocketSession session : room.keySet()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        }
    }

    private String createMessage(String username, String content, Date timestamp) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "chat");
        message.put("username", username);
        message.put("content", content);
        message.put("timestamp", timestamp.getTime());
        return objectMapper.writeValueAsString(message);
    }

    private void updateParticipants(String weather) throws IOException {
        Map<WebSocketSession, String> room = weatherRooms.get(weather);
        if (room != null) {
            List<String> participants = new ArrayList<>(room.values());
            Map<String, Object> message = new HashMap<>();
            message.put("type", "participants");
            message.put("list", participants);
            broadcastToRoom(weather, objectMapper.writeValueAsString(message));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String weather = sessionWeatherMap.remove(session);
        if (weather != null) {
            Map<WebSocketSession, String> room = weatherRooms.get(weather);
            if (room != null) {
                String username = room.remove(session);
                if (room.isEmpty()) {
                    weatherRooms.remove(weather);
                } else {
                    // 퇴장 메시지 브로드캐스트
                    broadcastToRoom(weather, createMessage("시스템", username + "님이 방을 떠났습니다.", new Date()));
                    // 참여자 목록 업데이트
                    updateParticipants(weather);
                }
            }
        }
    }

    public List<String> getWeatherRooms() {
        return new ArrayList<>(weatherRooms.keySet());
    }

    public Map<String, Integer> getWeatherRoomParticipants() {
        Map<String, Integer> roomParticipants = new HashMap<>();
        for (Map.Entry<String, Map<WebSocketSession, String>> entry : weatherRooms.entrySet()) {
            roomParticipants.put(entry.getKey(), entry.getValue().size());
        }
        return roomParticipants;
    }

    public Map<String, String> getWeatherRoomLocations() {
        Map<String, String> roomLocations = new HashMap<>();
        for (String room : weatherRooms.keySet()) {
            String[] parts = room.split(",");
            if (parts.length == 2) {
                roomLocations.put(room, String.format("%s,%s", parts[0], parts[1]));
            }
        }
        return roomLocations;
    }


}