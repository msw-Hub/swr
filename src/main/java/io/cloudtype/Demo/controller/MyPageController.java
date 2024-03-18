package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = "https://teamswr.store")
public class MyPageController {

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private UserInfoService userInfoService;

    @CrossOrigin(origins = "https://teamswr.store")
    @GetMapping("/mypage")
    public ResponseEntity<Map<String, Object>> myPage(@RequestHeader("Authorization") String accessToken) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 확인된 고유 ID를 가지고 데이터베이스에 해당 사용자가 이미 존재하는지 확인
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

}
