package com.benny.socketApp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChatRoomService {

    private final Map<String, List<WebSocketSession>> chatRooms = new ConcurrentHashMap<>();

    public void addUserToRoom(String weather, WebSocketSession session) {
        chatRooms.computeIfAbsent(weather, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void removeUserFromRoom(String weather, WebSocketSession session) {
        chatRooms.computeIfPresent(weather, (k, v) -> {
            v.remove(session);
            return v.isEmpty() ? null : v;
        });
    }

    public List<WebSocketSession> getUsersInRoom(String weather) {
        return chatRooms.getOrDefault(weather, new CopyOnWriteArrayList<>());
    }
}