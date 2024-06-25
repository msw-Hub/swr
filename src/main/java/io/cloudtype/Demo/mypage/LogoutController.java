package io.cloudtype.Demo.mypage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/logout")
public class LogoutController {
    private final LogoutService logoutService;

    @Autowired
    public LogoutController(LogoutService logoutService){
    this.logoutService = logoutService;
    }

    //로그아웃
    @GetMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken,
                                         @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        try {
            log.info("accessToken: {}", accessToken);
            log.info("refreshToken: {}", refreshToken);
            logoutService.logout(accessToken, refreshToken);
            return ResponseEntity.ok("로그아웃 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //회원탈퇴
    @GetMapping("/signOut")
    public ResponseEntity<?> signOut(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            logoutService.signOut(accessToken);
            return ResponseEntity.ok("회원 탈퇴 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
