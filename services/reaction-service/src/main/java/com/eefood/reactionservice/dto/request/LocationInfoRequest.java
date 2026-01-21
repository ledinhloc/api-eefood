package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationInfoRequest {
    private double latitude;
    private double longitude;
    private String province;
}
