package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.MatchingBoardService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @Operation(summary = "매칭-산책 글 작성", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pet_id",description = "서비스 이용할 반려동물 고유id / Long", required = true)
    @Parameter(name = "loaction",description = "산책 지역 / String", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info/profile/edit")
    public ResponseEntity<String> petInfoDelete(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Long petId = (Long) requestBody.get("pet_id");
            Map<String,Object> petInfo = userInfoService.getPetInfoByPetId(petId);
            String ownerNickname = (String) petInfo.get("owner_nickname");
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            String imageUrl = userInfoService.getImageUrlByPetId(petId);
            userInfoService.deletePetInfo(petId, imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("success", "반려동물 프로필 삭제 성공, 반려동물 목록 페이지로 이동하세요");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }


}
