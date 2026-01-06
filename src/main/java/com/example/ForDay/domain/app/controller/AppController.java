package com.example.ForDay.domain.app.controller;

import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
public class AppController {
    private final AppService appService;

    @GetMapping("/metadata")
    public AppMetaDataResDto getMetaData() {
        return appService.getMetaData();
    }
}
