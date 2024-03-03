package io.cloudtype.Demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserInfoService {

    private final JdbcTemplate jdbcTemplate;

    // 생성자를 통한 주입
    @Autowired
    public UserInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveUserInfo(Map<String, Object> userInfo) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String sql = "INSERT INTO testdb.user_info (user_id, nickname, profile_image, email, name, gender, age_range) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                (Long) userInfo.get("userId"), // userId를 Long으로 캐스팅
                userInfo.get("nickname"),
                userInfo.get("profileImage"),
                userInfo.get("email"),
                userInfo.get("name"),
                userInfo.get("gender"),
                userInfo.get("ageRange")
        );
    }
}
