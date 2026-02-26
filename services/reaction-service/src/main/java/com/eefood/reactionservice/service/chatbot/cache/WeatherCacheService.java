package com.eefood.reactionservice.service.chatbot.cache;

import com.eefood.reactionservice.util.WeatherCodeMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheService {

    private final WeatherCodeMapperUtils weatherCodeMapperUtils;

    @Cacheable(
            value = "weatherInfoCache",
            key = "#lat + ':' + #lng"
    )
    public String getWeatherInfo(double lat, double lng) {
        return weatherCodeMapperUtils
                .getCurrentWeather(lat, lng)
                .getDescription();
    }

}
