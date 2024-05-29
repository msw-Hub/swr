package io.cloudtype.Demo.mypage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/logout")
public class LogoutController {
    private final LogoutService myPageService;

    @Autowired
    public LogoutController(LogoutService myPageService) {
        this.myPageService = myPageService;
    }

    //로그아웃
    @PostMapping("")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken,
                                         @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        try {
            myPageService.logout(accessToken, refreshToken);
            return ResponseEntity.ok("로그아웃 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
