package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WeatherInfoRequest {
    private double temperature;
    private double windspeed;
    private int weathercode;
    private String description;
}
