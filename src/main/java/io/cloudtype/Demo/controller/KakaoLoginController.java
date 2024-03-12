package io.cloudtype.Demo.controller;

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
        String expiresIn = tokens.get("expires_in");
        String refreshToken = tokens.get("refresh_token");
        String refreshTokenExpiresIn = tokens.get("refresh_token_expires_in");

        log.info("Access Token : " + accessToken);
        log.info("Expires_In : " + expiresIn);
        log.info("Refresh Token : " + refreshToken);
        log.info("Refresh Token Expires In : " + refreshTokenExpiresIn);

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

        var count = kakaoService.processUser(userInfo);

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", userInfo.get("userId"));
        session.setAttribute("userNickname", userInfo.get("nickname"));
        session.setAttribute("userProfileImage", userInfo.get("profileImage"));
        session.setAttribute("userEmail", userInfo.get("email")); // 추가 정보 저장
        session.setAttribute("userName", userInfo.get("name")); // 추가 정보 저장
        session.setAttribute("userGender", userInfo.get("gender")); // 추가 정보 저장
        session.setAttribute("userAgeRange", userInfo.get("ageRange")); // 추가 정보 저장

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().body(tokens.toString());
    }
}
