package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/community")
@Tag(name = "community", description = "커뮤니티 게시판 관련 API")
public class CommunityBoardController {

    private final CommunityBoardService communityBoardService;

    @Autowired
    public CommunityBoardController(CommunityBoardService communityBoardService) {
        this.communityBoardService = communityBoardService;
    }

    @GetMapping("")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "커뮤니티 게시판 글 목록 가져오기", description = "최신 글부터 페이지별로 반환합니다.")
    @Parameter(name = "page", description = "페이지번호", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "커뮤니티 게시판 글 목록 반환 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, Object>> getCommunityBoard(@RequestParam(name = "page", defaultValue = "1") int page) {
        try {
            Map<String, Object> communityBoardList = communityBoardService.getCommunityBoardPosts(page);
            log.info("communityBoardList: " + communityBoardList);

            return ResponseEntity.ok(communityBoardList);
        } catch (Exception e) {
            log.error("Failed to fetch community board posts", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
