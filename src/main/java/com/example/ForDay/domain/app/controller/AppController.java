package com.example.ForDay.domain.app.controller;

import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.PresignedUrlResDto;
import com.example.ForDay.domain.app.service.AppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
public class AppController {
    private final AppService appService;

    @GetMapping("/metadata")
    public AppMetaDataResDto getMetaData() {
        return appService.getMetaData();
    }

    @PostMapping(value = "/presign")
    public List<PresignedUrlResDto> generatePresignedUrl(@RequestBody @Valid GeneratePresignedReqDto reqDto) {
        return appService.generatePresignedUrls(reqDto);
    }
}
