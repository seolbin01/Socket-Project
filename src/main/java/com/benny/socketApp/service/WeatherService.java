package com.benny.socketApp.service;

import com.benny.socketApp.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class WeatherService {

    @Value("${openweathermap.api.key}")
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final String API_URL = "http://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={apiKey}";

    public String getWeather(double lat, double lon) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory());
            ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setConnectTimeout(5000);
            ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(5000);

            String url = API_URL.replace("{lat}", String.valueOf(lat))
                    .replace("{lon}", String.valueOf(lon))
                    .replace("{apiKey}", apiKey);
            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
            logger.info("Weather API response for lat: {}, lon: {}: {}", lat, lon, response);
            return response != null && response.getWeather() != null && !response.getWeather().isEmpty()
                    ? response.getWeather().get(0).getMain()
                    : "Unknown";
        } catch (Exception e) {
            logger.error("Error fetching weather data for lat: {}, lon: {}", lat, lon, e);
            return "Unknown";
        }
    }
}

class WeatherResponse {
    private List<Weather> weather;

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }
}

class Weather {
    private String main;

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }
}