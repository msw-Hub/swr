package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.WalkMatchingBoardService;
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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/walk")
@Tag(name = "matching-walk", description = "매칭 게시판 관련 API")
public class WalkMatchingBoardController {
    private final WalkMatchingBoardService walkMatchingBoardService;
    @Autowired
    private KakaoService kakaoService;
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    public WalkMatchingBoardController(WalkMatchingBoardService walkMatchingBoardService) {
        this.walkMatchingBoardService = walkMatchingBoardService;
    }


    @Operation(summary = "매칭-산책 글 작성전, 반려동물 id, name, profile 전달하며 반려동물 고르기", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "반려동물 id, name, profile 반환성공")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/post1")
    public ResponseEntity<String> walkPost1(
            @RequestHeader("Authorization") String accessToken
    ) {
        try {
            // 가입된 사람만 사용하도록 확인
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");

            //등록한 반려동물 리스트 반환
            Map<String, Object> response = userInfoService.getPetInfoByOwnerNickname(nickname);
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "매칭-산책 글 작성", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_time", description = "산책시간 / INT(30/60/90/120)", required = true)
    @Parameter(name = "walk_date", description = "산책예약날짜 / DATE(yyyy-mm-dd hh:mm:00)", required = true)
    @Parameter(name = "pet_info_id",description = "서비스 이용할 반려동물 고유id / Long", required = true)
    @Parameter(name = "latitude",description = "신청위치의 위도 / Double", required = true)
    @Parameter(name = "longitude",description = "신청위치의 경도 / Double", required = true)
    @Parameter(name = "title", description = "신청글제목 / String", required = true)
    @Parameter(name = "content", description = "산책전 만날 상세주소, 요청사항, 주의사항 / String", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/post2")
    public ResponseEntity<String> walkPost2(
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
            Long petInfoId = Long.valueOf(requestBody.get("pet_info_id").toString());
            Map<String,Object> petInfo = userInfoService.getPetInfoByPetId(petInfoId);
            String ownerNickname = (String) petInfo.get("owner_nickname");
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            //산책글 작성 서비스 호출
            //넘길데이터 nickname=owner_nickname,pet_info_id, walk_time, walk_date, latitude, longitude, title,content, status=0
            //petId가 유효한지 검색

            Integer walkTime = (Integer) requestBody.get("walk_time");
            Timestamp walkDate = (Timestamp) requestBody.get("walk_date");
            double latitude = ((Number) requestBody.get("latitude")).doubleValue();
            double longitude = ((Number) requestBody.get("longitude")).doubleValue();
            String title = (String) requestBody.get("title");
            String content = (String) requestBody.get("content");
            walkMatchingBoardService.writeWalkPost(nickname, petInfoId, walkTime, walkDate, latitude, longitude, title, content);

            Map<String, Object> response = new HashMap<>();
            response.put("success", "산책글 작성 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 목록반환, 파트너만 볼 수 있도록", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "page", description = "페이지 번호 / Integer", required = true)
    @Parameter(name = "now_latitude", description = "현재 위도 / Double", required = true)
    @Parameter(name = "now_longitude",description = "현재 경도 / Double", required = true)
    @Parameter(name = "max_distance",description = "최대 거리 / Double(1/3/5)", required = true)
    @ApiResponse(responseCode = "200", description = "산책 신청글 반환성공")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("")
    public ResponseEntity<String> walkList(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 가입된 사람만 사용하도록 확인
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            // 가져온 유저 정보에서 고유 ID를 확인. 해당 ID를 가진 유저가 파트너권한이 있는지를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            if ((int) dbUserInfo.get("partnership") == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "파트너 권한이 없습니다.");
                return ResponseEntity.ok(response.toString());
            }
            //산책 신청글 목록 반환 / 위치 계산
            double now_latitude = ((Number) requestBody.get("now_latitude")).doubleValue();
            double now_longitude = ((Number) requestBody.get("now_longitude")).doubleValue();
            double max_distance = ((Number) requestBody.get("max_distance")).doubleValue();
            List<Map<String, Object>> response = walkMatchingBoardService.getWalkPostListWithDistance((int) requestBody.get("page"),
                    now_latitude, now_longitude, max_distance);
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
