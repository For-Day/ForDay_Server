package com.example.ForDay.domain.app.controller;

import com.example.ForDay.domain.app.dto.request.DeleteS3ImageReqDto;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.GeneratePresignedUrlResDto;
import com.example.ForDay.domain.app.dto.response.VersionPolicyResDto;
import com.example.ForDay.domain.app.service.AppService;
import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.global.common.response.dto.MessageResDto;
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
    public List<GeneratePresignedUrlResDto> generatePresignedUrl(@RequestBody @Valid GeneratePresignedReqDto reqDto) {
        return appService.generatePresignedUrls(reqDto);
    }

    @DeleteMapping("/images/temp")
    public MessageResDto deleteS3Image(@RequestBody @Valid DeleteS3ImageReqDto reqDto) {
        return appService.deleteS3Image(reqDto);
    }

    @GetMapping("/version-policy")
    public VersionPolicyResDto getVersionPolicy(
            @RequestParam Platform platform,
            @RequestParam String appVersion,
            @RequestParam int build
    ) {
        return appService.getPolicy(platform, appVersion, build);
    }
}
