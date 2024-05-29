package io.cloudtype.Demo.mypage;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.BlacklistEntity;
import io.cloudtype.Demo.mypage.user.BlacklistRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class LogoutService {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final BlacklistRepository blacklistRepository;

    @Autowired
    public LogoutService(JWTUtil jwtUtil, UserRepository userRepository,
                         BlacklistRepository blacklistRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.blacklistRepository = blacklistRepository;
    }
    //블랙 리스트에 추가하고 나중에 비교함
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        user.setRefreshToken(null);
        userRepository.save(user);

        BlacklistEntity blacklistEntity = new BlacklistEntity();
        blacklistEntity.setAccessToken(accessToken);
        blacklistEntity.setRefreshToken(refreshToken);
        blacklistRepository.save(blacklistEntity);
    }
}
