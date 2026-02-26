package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.dto.request.WeatherInfoRequest;
import lombok.*;

@Getter
@Setter
public class OpenMeteoResponse {
    private WeatherInfoRequest current_weather;
}
