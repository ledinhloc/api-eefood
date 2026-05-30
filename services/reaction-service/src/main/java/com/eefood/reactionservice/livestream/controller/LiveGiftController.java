package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.request.SendGiftRequest;
import com.eefood.reactionservice.livestream.dto.response.LiveGiftItemResponse;
import com.eefood.reactionservice.livestream.dto.response.SendGiftResponse;
import com.eefood.reactionservice.livestream.service.LiveGiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams/gift")
@RequiredArgsConstructor
public class LiveGiftController {
    private final LiveGiftService liveGiftService;

    @GetMapping
    public ResponseData<List<LiveGiftItemResponse>> getAllLiveGifts() {
        List<LiveGiftItemResponse> responses = liveGiftService.getAvailableGifts();
        return new ResponseData<>(HttpStatus.OK.value(),"Success", responses);
    }

    @PostMapping
    public ResponseData<SendGiftResponse> sendGift(@RequestBody SendGiftRequest request) {
        SendGiftResponse response = liveGiftService.sendGift(request);
        return new ResponseData<>(HttpStatus.OK.value(),"Success", response);
    }
}
