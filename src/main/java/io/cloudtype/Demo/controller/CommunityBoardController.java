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
    private KakaoService kakaoService;
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    public CommunityBoardController(CommunityBoardService communityBoardService) {
        this.communityBoardService = communityBoardService;
    }

    @PostMapping("")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "커뮤니티 게시판 글 목록 가져오기", description = "최신 글부터 페이지별로 반환합니다.")
    @Parameter(name = "accessToken", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "page", description = "페이지 번호 (기본값 1)", required = true)
    @ApiResponse(responseCode = "200", description = "커뮤니티 게시판 글 목록 반환 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, Object>> getCommunityBoard(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int page = (int) requestBody.get("page");
            // 가입된 사람만 사용하도록 확인
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Map<String, Object> communityBoardList = communityBoardService.getCommunityBoardPosts(page);
            log.info("communityBoardList: " + communityBoardList);

            return ResponseEntity.ok(communityBoardList);
        } catch (Exception e) {
            log.error("Failed to fetch community board posts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create")
    @Operation(summary = "게시글 작성", description = "게시글을 작성합니다.")
    @Parameter(name = "accessToken", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "title", description = "게시글 제목", required = true)
    @Parameter(name = "content", description = "게시글 내용", required = true)
    @ApiResponse(responseCode = "200", description = "글 작성 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, String>> writePost(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            Long writerId = (Long) dbUserInfo.get("Id");
            String nickname = (String) dbUserInfo.get("nickname");

            String title = (String) requestBody.get("title");
            String content = (String) requestBody.get("content");
            
            // 게시글 작성
            communityBoardService.writePost(writerId, nickname, title, content);

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "글 작성 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to write post", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
