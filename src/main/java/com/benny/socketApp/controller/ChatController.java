package com.benny.socketApp.controller;

import com.benny.socketApp.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Value("${googlemaps.api.key}")
    private String googleMapsApiKey;

    @Value("${openweathermap.api.key}")
    private String openWeatherMapApiKey;

    @GetMapping("/")
    public String index(Model model) {
        List<String> weatherRooms = chatWebSocketHandler.getWeatherRooms();
        Map<String, Integer> weatherRoomParticipants = chatWebSocketHandler.getWeatherRoomParticipants();
        Map<String, String> weatherRoomLocations = chatWebSocketHandler.getWeatherRoomLocations();
        model.addAttribute("weatherRooms", weatherRooms);
        model.addAttribute("weatherRoomParticipants", weatherRoomParticipants);
        model.addAttribute("weatherRoomLocations", weatherRoomLocations);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("openWeatherMapApiKey", openWeatherMapApiKey);
        return "index";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
