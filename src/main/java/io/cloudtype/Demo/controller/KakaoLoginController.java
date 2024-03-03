package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.KakaoService;
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

    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code, HttpSession session, HttpServletResponse response) throws IOException {
        String accessToken = kakaoService.getAccessTokenFromKakao(client_id, code);
        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", userInfo.get("userId"));
        session.setAttribute("userNickname", userInfo.get("nickname"));
        session.setAttribute("userProfileImage", userInfo.get("profileImage"));
        session.setAttribute("userEmail", userInfo.get("email")); // 추가 정보 저장
        session.setAttribute("userName", userInfo.get("name")); // 추가 정보 저장
        session.setAttribute("userGender", userInfo.get("gender")); // 추가 정보 저장
        session.setAttribute("userAgeRange", userInfo.get("ageRange")); // 추가 정보 저장
        session.setAttribute("userPhoneNumber", userInfo.get("phoneNumber")); // 추가 정보 저장

        log.info("userId : " + userInfo.get("userId"));
        log.info("userNickname : " + userInfo.get("nickname"));
        log.info("userProfileImage : " + userInfo.get("profileImage"));
        log.info("userEmail : " + userInfo.get("email"));
        log.info("userName : " + userInfo.get("name"));
        log.info("userGender : " + userInfo.get("gender"));
        log.info("userAgeRange : " + userInfo.get("ageRange"));

        // 세션 ID를 제외한 URL로 리다이렉션
        response.sendRedirect("/login/success");
    }



}
