package com.eefood.reactionservice.util;

import com.eefood.reactionservice.dto.request.WeatherInfoRequest;
import com.eefood.reactionservice.dto.response.OpenMeteoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


public class WeatherCodeMapperUtils {
    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast";

    private static final RestTemplate restTemplate = new RestTemplate();

    public static WeatherInfoRequest getCurrentWeather(double latitude, double longitude) {

        String url = UriComponentsBuilder.fromHttpUrl(OPEN_METEO_URL)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current_weather", true)
                .queryParam("timezone", "Asia/Ho_Chi_Minh")
                .toUriString();

        OpenMeteoResponse response =
                restTemplate.getForObject(url, OpenMeteoResponse.class);

        if (response == null || response.getCurrent_weather() == null) {
            throw new RuntimeException("Không thể lấy dữ liệu thời tiết từ Open-Meteo");
        }

        return WeatherInfoRequest.builder()
                .temperature(response.getCurrent_weather().getTemperature())
                .windspeed(response.getCurrent_weather().getWindspeed())
                .weathercode(response.getCurrent_weather().getWeathercode())
                .description(WeatherCodeMapperUtils.description(response.getCurrent_weather().getWeathercode()))
                .build();
    }

    static String description(int code) {
        switch (code) {
            case 0:
                return "Trời quang";
            case 1:
            case 2:
            case 3:
                return "Có mây";
            case 45:
            case 48:
                return "Sương mù";
            case 51:
            case 53:
            case 55:
                return "Mưa phùn";
            case 61:
            case 63:
            case 65:
                return "Mưa";
            case 71:
            case 73:
            case 75:
                return "Tuyết rơi";
            case 80:
            case 81:
            case 82:
                return "Mưa rào";
            case 95:
                return "Dông";
            default:
                return "Thời tiết không xác định";
        }
    }
}
