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

            // Trời quang – nắng rõ
            case 0:
                return "Trời nắng, quang mây";

            // Ít mây → nhiều mây (nhưng vẫn oi)
            case 1:
                return "Trời nắng nhẹ, có mây";
            case 2:
                return "Nhiều mây, trời oi";
            case 3:
                return "Trời nhiều mây, khá oi bức";

            // Sương mù – trời âm u
            case 45:
            case 48:
                return "Trời âm u, có sương mù";

            // Mưa phùn – mưa nhẹ
            case 51:
                return "Mưa phùn nhẹ";
            case 53:
                return "Mưa phùn";
            case 55:
                return "Mưa phùn dày, ẩm ướt";

            // Mưa thường
            case 61:
                return "Mưa nhỏ";
            case 63:
                return "Mưa vừa";
            case 65:
                return "Mưa to";

            // Tuyết – không phù hợp VN
            case 71:
            case 73:
            case 75:
                return "Thời tiết lạnh bất thường";

            // Mưa rào
            case 80:
                return "Mưa rào nhẹ";
            case 81:
                return "Mưa rào";
            case 82:
                return "Mưa rào lớn";

            // Dông, sấm sét
            case 95:
                return "Mưa dông, sấm sét";

            default:
                return "Thời tiết thất thường";
        }
    }
}
