package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mypage")
@Tag(name = "mypage", description = "마이페이지 관련 API")
@CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
public class MyPageController {

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "마이페이지 정보 조회", description = "마이페이지 정보를 조회하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "조회 성공_닉네임,이메일 반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = Map.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> myPage(@RequestHeader("Authorization") String accessToken) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 확인된 고유 ID를 가지고 데이터베이스에 해당 사용자가 이미 존재하는지 확인하고 가져오기
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);

            if (dbUserInfo != null) {
                // 클라이언트에게 전달할 Map 객체 생성
                Map<String, Object> responseUserInfo = new HashMap<>();

                // 가져온 사용자 정보 중에서 닉네임과 이메일 정보만을 추출하여 클라이언트로 보냄
                responseUserInfo.put("nickname", dbUserInfo.get("nickname"));
                responseUserInfo.put("email", dbUserInfo.get("email"));

                // JSON 형식으로 반환
                return ResponseEntity.ok().body(responseUserInfo);
            } else {
                // 사용자가 데이터베이스에 존재하지 않는 경우, 에러 응답을 반환하거나 다른 처리를 수행할 수 있음
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "핀 번호 확인", description = "핀 번호를 확인하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pin_number", description = "핀 번호", required = true)
    @ApiResponse(responseCode = "200", description = "핀 번호 일치_메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pin-check")
    public ResponseEntity<String> pinCheck(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody @NotNull Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 프론트로부터 받은 핀 번호
            String pinNumber = requestBody.get("pin_number");

            // 데이터베이스에서 해당 사용자의 핀 번호 가져오기
            String dbPinNumber = userInfoService.getPinNumberByUserId(userId);
            
            // 받은 핀 번호와 데이터베이스의 핀 번호 비교
            Map<String, Object> jsonResponse = new HashMap<>();
            if (pinNumber.equals(dbPinNumber)) {
                jsonResponse.put("success", "핀번호가 일치함");
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(jsonResponse);
                return ResponseEntity.ok().body(jsonString);
            } else {
                // 핀 번호가 일치하지 않는 경우, 400 상태 코드와 메시지 반환
                jsonResponse.put("bad", "핀번호가 불일치함");
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(jsonResponse);
                return ResponseEntity.badRequest().body(jsonString);
            }
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "정보 수정", description = "사용자 정보를 수정하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "nickname", description = "변경할 닉네임", required = false)
    @Parameter(name = "pin_number", description = "변경할 핀 번호", required = false)
    @Parameter(name = "phone_number", description = "변경할 전화번호", required = false)
    @ApiResponse(responseCode = "200", description = "정보 수정 성공_성공메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/edit-info")
    public ResponseEntity<String> editInfo(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody  Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);

            String column = "";

            // 닉네임을 수정할 경우, 중복된 닉네임이 있는지 확인
            String changeNickname = requestBody.get("nickname");
            if (changeNickname != null && !changeNickname.trim().isEmpty()) {
                column = "nickname";

                // 닉네임 중복 확인
                String nowNickname = (String) dbUserInfo.get("nickname");
                boolean isNicknameDuplicate = nowNickname.equalsIgnoreCase(changeNickname);
                if (isNicknameDuplicate) {
                    return ResponseEntity.badRequest().body("이미 사용 중인 닉네임입니다.");
                }
                else {
                    log.info("바뀌기전 nickname : " +dbUserInfo.get("nickname"));
                    userInfoService.updateUserInfo(userId,column,changeNickname);
                    dbUserInfo = userInfoService.getUserInfoById(userId);
                    log.info("바뀐후 nickname : " +dbUserInfo.get("nickname"));
                }

            }

            // 핀 번호를 수정할 경우
            String pinNumber = requestBody.get("pin_number");
            if (pinNumber != null && !pinNumber.trim().isEmpty() && pinNumber.matches("\\d{6}")) {
                column = "pin_number";
                log.info("바뀌기전 pin_number : " +dbUserInfo.get("pin_number"));
                userInfoService.updateUserInfo(userId,column,pinNumber);
                dbUserInfo = userInfoService.getUserInfoById(userId);
                log.info("바뀐후 pin_number : " +dbUserInfo.get("pin_number"));
            }

            // 전화번호를 수정할 경우
            String phoneNumber = requestBody.get("phone_number");
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() && phoneNumber.matches("01[0-9]-\\d{4}-\\d{4}")) {
                column = "phone_number";
                log.info("바뀌기전 phone_number : " +dbUserInfo.get("phone_number"));
                userInfoService.updateUserInfo(userId,column,phoneNumber);
                dbUserInfo = userInfoService.getUserInfoById(userId);
                log.info("바뀐후 phone_number : " +dbUserInfo.get("phone_number"));
            }
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", "정보가 성공적으로 수정되었습니다.");
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(jsonResponse);
            return ResponseEntity.ok().body(jsonString);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
