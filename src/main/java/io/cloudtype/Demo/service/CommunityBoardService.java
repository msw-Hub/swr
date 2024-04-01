package io.cloudtype.Demo.service;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CommunityBoardService {

    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                                 @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                                 JdbcTemplate jdbcTemplate) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
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

    public void writePost(Long userId, String nickname, String title, String content, String imgUrl){
        // 현재 시간 가져오기
        Instant now = Instant.now();

        // 게시글 작성 SQL 쿼리 실행
        String sql = "INSERT INTO community_board (writer_id, writer_nickname, title, content, created_date, img_url) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, nickname, title, content, now, imgUrl);
    }

    public String uploadImage(MultipartFile image) {
        try {
            // 이미지가 비어 있는지 확인
            if (image.isEmpty()) {
                log.warn("Empty image provided for upload.");
                return null;
            }

            // Google Cloud Storage에 이미지 업로드
            InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
            String uuid = UUID.randomUUID().toString();
            String ext = StringUtils.getFilenameExtension(image.getOriginalFilename());
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build()
                    .getService();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, uuid)
                    .setContentType(ext)
                    .build();
            Blob blob = storage.create(blobInfo, image.getBytes());

            // 업로드된 이미지의 URL 반환
            String imgUrl = "https://storage.googleapis.com/" + bucketName + "/" + uuid;

            // 성공 시 정보 로그 작성
            log.info("Image uploaded successfully. URL: {}", imgUrl);
            return imgUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Google Cloud Storage.", e);
            return null;
        }
    }

    public List<Map<String, Object>> getCommentsForPost(Long postId) {
        String sql = "SELECT * FROM community_reply WHERE community_board_id = ?";
        return jdbcTemplate.queryForList(sql, postId);
    }
}
