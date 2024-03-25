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

    //유저 고유id를 통해서 유저정보를 가져오는 메서드
    public Map<String, Object> getUserInfoById(Long userId) {
        String sql = "SELECT * FROM mydb.user_info WHERE user_id = ?";
        return jdbcTemplate.queryForMap(sql, userId);
    }

    //db에 유저정보 저장하는 메서드
    public void saveUserInfo(Map<String, Object> userInfo) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String sql = "INSERT INTO mydb.user_info (user_id, nickname, profile_image, email, name, gender, age_range) "
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

    // 회원가입 시 추가 정보를 저장하는 메서드
    public void saveAdditionalUserInfo(String phoneNumber, String pinNumber, String birthday) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String sql = "INSERT INTO mydb.user_info (phone_number, pin_number, birthday) "
                + "VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE phone_number = VALUES(phone_number), pin_number = VALUES(pin_number), birthday = VALUES(birthday)";
        jdbcTemplate.update(sql, phoneNumber, pinNumber, birthday);
    }

    //개인정보 수정시 pin번호를 조회하는 메서드
    public String getPinNumberByUserId(Long userId) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }

        // 데이터베이스에서 userId에 해당하는 사용자의 핀 번호를 조회하는 쿼리 작성
        String sql = "SELECT pin_number FROM mydb.user_info WHERE user_id = ?";

        // 쿼리 실행하여 결과 가져오기
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    public void updateUserInfo(Long userId, String column, String value) {
        // 수정할 column에 따라 SQL 쿼리를 동적으로 생성하여 실행
        String sql = switch (column) {
            case "nickname" -> "UPDATE user_info SET nickname = ? WHERE user_id = ?";
            case "pin_number" -> "UPDATE user_info SET pin_number = ? WHERE user_id = ?";
            case "phone_number" -> "UPDATE user_info SET phone_number = ? WHERE user_id = ?";
            default ->
                // 유효하지 않은 column인 경우 예외 처리
                    throw new IllegalArgumentException("유효하지 않은 column입니다: " + column);
        };

        // SQL 쿼리 실행
        jdbcTemplate.update(sql, value, userId);
    }
}
