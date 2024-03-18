package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
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
@RequestMapping("")
public class KakaoLoginController {

    @Value("${kakao.client_id}")
    private String client_id;

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private UserInfoService userInfoService; // UserInfoService 주입

    @CrossOrigin(origins = "https://teamswr.store")
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) throws IOException {
        log.info(code);
        Map<String, String> tokens = kakaoService.getTokensFromKakao(client_id, code);
        String accessToken = tokens.get("access_token");
        String expiresInStr = tokens.get("expires_in");
        int expiresIn = Integer.parseInt(expiresInStr);
        String refreshToken = tokens.get("refresh_token");
        String refreshTokenExpiresInStr = tokens.get("refresh_token_expires_in");
        int refreshTokenExpiresIn = Integer.parseInt(refreshTokenExpiresInStr);

        log.info("Access Token : " + accessToken);
        log.info("Expires_In : " + expiresInStr);
        log.info("Refresh Token : " + refreshToken);
        log.info("Refresh Token Expires In : " + refreshTokenExpiresInStr);

        // 현재 시간을 가져옴
        Instant now = Instant.now();

        // 만료 시간을 현재 시간에 만료 기간을 더한 값으로 계산
        Instant expiresAtInstant = now.plusSeconds(expiresIn);
        long expiresAtUnix = expiresAtInstant.getEpochSecond();

        // refreshToken 만료 시간을 계산
        Instant refreshTokenExpiresAtInstant = now.plusSeconds(refreshTokenExpiresIn);
        long refreshTokenExpiresAtUnix = refreshTokenExpiresAtInstant.getEpochSecond();

        log.info(String.valueOf(expiresAtUnix));

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
        String nickName = (String) userInfo.get("nickname");
        String email = (String) userInfo.get("email");

        //데이터베이스에 있는 내용인지 검토
        int count = kakaoService.processUser(userInfo);

        String logMessage = count == 0 ? "회원가입 완료" : "로그인 완료";
        log.info(logMessage);

        // JSON 응답에 포함할 데이터 준비
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("access_token", accessToken);
        jsonResponse.put("expires_at_unix", expiresAtUnix);
        jsonResponse.put("refresh_token", refreshToken);
        jsonResponse.put("refresh_token_expires_in", refreshTokenExpiresAtUnix); // 수정된 부분

        // ObjectMapper를 사용하여 Map 객체를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(jsonResponse);

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().body(jsonString);
    }
}
