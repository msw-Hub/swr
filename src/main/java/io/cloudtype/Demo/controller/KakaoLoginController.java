package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    public ResponseEntity<String> callback(@RequestParam("code") String code, HttpSession session, HttpServletResponse response) throws IOException {
        log.info(code);
        Map<String, String> tokens = kakaoService.getTokensFromKakao(client_id, code);
        String accessToken = tokens.get("access_token");
        String expiresInStr = tokens.get("expires_in");
        int expiresIn = Integer.parseInt(expiresInStr);
        String refreshToken = tokens.get("refresh_token");
        String refreshTokenExpiresIn = tokens.get("refresh_token_expires_in");

        log.info("Access Token : " + accessToken);
        log.info("Expires_In : " + expiresInStr);
        log.info("Refresh Token : " + refreshToken);
        log.info("Refresh Token Expires In : " + refreshTokenExpiresIn);

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
        String nickName = (String) userInfo.get("nickname");

        var count = kakaoService.processUser(userInfo);

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", userInfo.get("userId"));
        session.setAttribute("userNickname", userInfo.get("nickname"));
        session.setAttribute("userProfileImage", userInfo.get("profileImage"));
        session.setAttribute("userEmail", userInfo.get("email")); // 추가 정보 저장
        session.setAttribute("userName", userInfo.get("name")); // 추가 정보 저장
        session.setAttribute("userGender", userInfo.get("gender")); // 추가 정보 저장
        session.setAttribute("userAgeRange", userInfo.get("ageRange")); // 추가 정보 저장

        // 현재 시간을 milliseconds로 변환하여 expiresIn(초)만큼 더하여 유효한 만료 시간 계산
        long now = System.currentTimeMillis();
        long expiresInMillis = now + (expiresIn * 1000L);
        Date expiryDate = new Date(expiresInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 날짜 형식으로 변환한 만료 시간을 JSON에 추가
        String expiryDateString = sdf.format(expiryDate);

        Map<String, String> jsonResponse = new HashMap<>();
        jsonResponse.put("access_token", accessToken);
        jsonResponse.put("nick_name", nickName);
        jsonResponse.put("expires_at", expiryDateString);

        // ObjectMapper를 사용하여 Map 객체를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(jsonResponse);

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().body(jsonString);
    }
}
