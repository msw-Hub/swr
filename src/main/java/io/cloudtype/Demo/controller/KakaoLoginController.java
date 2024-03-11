package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code, HttpSession session, HttpServletResponse response) throws IOException {
        Map<String, String> tokens = kakaoService.getTokensFromKakao(client_id, code);
        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.get("refresh_token");
        String refreshTokenExpiresIn = tokens.get("refresh_token_expires_in");

        log.info("Access Token : " + accessToken);
        log.info("Refresh Token : " + refreshToken);
        log.info("Refresh Token Expires In : " + refreshTokenExpiresIn);

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", userInfo.get("userId"));
        session.setAttribute("userNickname", userInfo.get("nickname"));
        session.setAttribute("userProfileImage", userInfo.get("profileImage"));
        session.setAttribute("userEmail", userInfo.get("email")); // 추가 정보 저장
        session.setAttribute("userName", userInfo.get("name")); // 추가 정보 저장
        session.setAttribute("userGender", userInfo.get("gender")); // 추가 정보 저장
        session.setAttribute("userAgeRange", userInfo.get("ageRange")); // 추가 정보 저장

        log.info("userId : " + userInfo.get("userId"));
        log.info("userNickname : " + userInfo.get("nickname"));
        log.info("userProfileImage : " + userInfo.get("profileImage"));
        log.info("userEmail : " + userInfo.get("email"));
        log.info("userName : " + userInfo.get("name"));
        log.info("userGender : " + userInfo.get("gender"));
        log.info("userAgeRange : " + userInfo.get("ageRange"));

        // 사용자 정보를 데이터베이스에 저장
        userInfoService.saveUserInfo(userInfo); // 수정된 부분

        log.info("디비 저장함수가 켜지긴함.");

        // 세션 ID를 제외한 URL로 리다이렉션
        response.sendRedirect("/login/success");
    }
}
