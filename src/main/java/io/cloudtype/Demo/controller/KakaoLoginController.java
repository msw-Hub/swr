package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        var count = kakaoService.processUser(userInfo);

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", userInfo.get("userId"));
        session.setAttribute("userNickname", userInfo.get("nickname"));
        session.setAttribute("userProfileImage", userInfo.get("profileImage"));
        session.setAttribute("userEmail", userInfo.get("email")); // 추가 정보 저장
        session.setAttribute("userName", userInfo.get("name")); // 추가 정보 저장
        session.setAttribute("userGender", userInfo.get("gender")); // 추가 정보 저장
        session.setAttribute("userAgeRange", userInfo.get("ageRange")); // 추가 정보 저장



        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", accessToken);
        tokenResponse.put("refresh_token", refreshToken);
        tokenResponse.put("refresh_token_expires_in", refreshTokenExpiresIn);

        log.info("JSON Response: " + new ObjectMapper().writeValueAsString(tokenResponse));

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(tokenResponse));

        response.sendRedirect("/login/successSign");
//        if(count ==0){
//            userInfoService.saveUserInfo(userInfo);
//            response.sendRedirect("/login/successSign");
//        }
//        else response.sendRedirect("/login/successLogin");

    }
}
