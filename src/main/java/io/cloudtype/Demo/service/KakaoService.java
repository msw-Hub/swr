package io.cloudtype.Demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class KakaoService {
    private final JdbcTemplate jdbcTemplate;
    private final UserInfoService userInfoService;

    // 생성자를 통한 주입
    @Autowired
    public KakaoService(JdbcTemplate jdbcTemplate, UserInfoService userInfoService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userInfoService = userInfoService;
    }


    public Map<String, String> getTokensFromKakao(String client_id, String code) throws IOException {
        //------kakao POST 요청------
        String reqURL = "https://kauth.kakao.com/oauth/token?grant_type=authorization_code&client_id="+client_id+"&code=" + code;
        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        String result = "";

        while ((line = br.readLine()) != null) {
            result += line;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> tokens = objectMapper.readValue(result, new TypeReference<Map<String, String>>() {});

        log.info("Response Body : " + result);

        return tokens;
    }

    public Map<String, Object> getUserInfo(String access_Token) throws IOException {
        // 클라이언트 요청 정보
        Map<String, Object> userInfo = new HashMap<>();

        //------kakao GET 요청------
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + access_Token);

        int responseCode = conn.getResponseCode();
        System.out.println("responseCode : " + responseCode);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            result.append(line);
        }

        // jackson objectmapper 객체 생성
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON String -> Map
        Map<String, Object> jsonMap = objectMapper.readValue(result.toString(), new TypeReference<Map<String, Object>>() {});

        // 프로필 정보 가져오기
        Map<String, Object> properties = (Map<String, Object>) jsonMap.get("properties");
        String nickname = (String) properties.get("nickname");
        String profileImage = (String) properties.get("profile_image");

        // 카카오 계정 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) jsonMap.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String name = (String) profile.get("nickname");
        String gender = (String) kakaoAccount.get("gender");
        String ageRange = (String) kakaoAccount.get("age_range");
        Long userId = (Long) jsonMap.get("id");

        // userInfo에 넣기
        userInfo.put("userId", userId);
        userInfo.put("nickname", nickname);
        userInfo.put("profileImage", profileImage);
        userInfo.put("email", email);
        userInfo.put("name", name);
        userInfo.put("gender", gender);
        userInfo.put("ageRange", ageRange);

        return userInfo;
    }
    // 회원가입 또는 로그인 처리 메서드
    public int processUser(Map<String, Object> userInfo) {
        Long userId = (Long) userInfo.get("userId");

        // 사용자 ID가 데이터베이스에 이미 존재하는지 확인
        String sql = "SELECT COUNT(*) FROM testdb.user_info WHERE user_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, userId);

        if (count == 0) {
            // 사용자 정보가 데이터베이스에 존재하지 않는 경우, 사용자 정보를 저장
            userInfoService.saveUserInfo(userInfo);
        }

        return count;
    }
}