package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.MatchingBoardService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/matching")
@Tag(name = "matching", description = "매칭 게시판 관련 API")
public class MatchingBoardController {
    private final MatchingBoardService matchingBoardService;
    private final KakaoService kakaoService;

    @Autowired
    public MatchingBoardController(MatchingBoardService matchingBoardService, KakaoService kakaoService) {
        this.matchingBoardService = matchingBoardService;
        this.kakaoService = kakaoService;
    }


}
