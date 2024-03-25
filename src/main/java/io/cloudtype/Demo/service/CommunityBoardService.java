package io.cloudtype.Demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommunityBoardService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CommunityBoardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getCommunityBoardPosts(int page) {
        // 한 페이지에 표시할 게시글 수
        int pageSize = 10;

        // 페이지 번호에 따라 시작 게시글의 인덱스 계산
        int start = (page - 1) * pageSize;

        // SQL 쿼리 생성
        String sql = "SELECT * FROM community_board ORDER BY created_date DESC LIMIT ?, ?";

        // SQL 쿼리 실행
        try {
            List<Map<String, Object>> lists = jdbcTemplate.queryForList(sql, start, pageSize);

            // 결과 맵 생성
            Map<String, Object> result = new HashMap<>();
            result.put("posts", lists);
            result.put("page", page);
            result.put("pageSize", pageSize);

            return result;
        } catch (Exception e) {
            log.error("Failed to fetch community board posts", e);
            return null;
        }
    }

    public void writePost(Long userId, String nickname, String title, String content) {
        // 현재 시간 가져오기
        Instant now = Instant.now();

        // 게시글 작성 SQL 쿼리 실행
        String sql = "INSERT INTO community_board (writer_id, writer_nickname, title, content, created_date) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, nickname, title, content, now);
    }
}
