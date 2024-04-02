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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityBoardService {

    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                                 @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                                 @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                                 JdbcTemplate jdbcTemplate) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
        this.projectId = projectId;
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
    public void writePost(String nickname, String title, String content, String imgUrl){
        // 현재 시간 가져오기
        Instant now = Instant.now();
        // 게시글 작성 SQL 쿼리 실행
        String sql = "INSERT INTO community_board (writer_nickname, title, content, created_date, img_url) " +
                "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,nickname, title, content, now, imgUrl);
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
    public void deleteCommunityBoard(Long communityBoardId) {
        // 게시글 id를 토대로 이미지 URL을 가져옴
        String sql = "SELECT img_url FROM community_board WHERE community_board_id = ?";
        String imgUrl = jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
        if (imgUrl != null && !imgUrl.isEmpty()) {
            deleteImageGcs(imgUrl);
        }

        // 게시글에 연결된 모든 댓글의 ID를 찾음
        String findRepliesSql = "SELECT community_reply_id FROM community_reply WHERE community_board_id = ?";
        List<Long> replyIds = jdbcTemplate.queryForList(findRepliesSql, Long.class, communityBoardId);

        // 모든 댓글의 ID를 쉼표로 구분된 문자열로 변환
        String replyIdsStr = replyIds.stream().map(Object::toString).collect(Collectors.joining(","));

        // 모든 댓글과 관련된 좋아요를 한 번의 쿼리로 삭제
        String deleteLikesSql = "DELETE FROM likes WHERE likes_reply_id IN (" + replyIdsStr + ")";
        jdbcTemplate.update(deleteLikesSql);

        // 모든 댓글을 한 번의 쿼리로 삭제
        String deleteRepliesSql = "DELETE FROM community_reply WHERE community_reply_id IN (" + replyIdsStr + ")";
        jdbcTemplate.update(deleteRepliesSql);

        //게시글의 좋아요 삭제
        String deleteLikesSql2 = "DELETE FROM likes WHERE likes_board_id = ?";
        jdbcTemplate.update(deleteLikesSql2, communityBoardId);

        // 게시글 삭제
        String deleteSql = "DELETE FROM community_board WHERE community_board_id = ?";
        jdbcTemplate.update(deleteSql, communityBoardId);
    }
    public void deleteImageGcs(String imgUrl){
        // imgUrl에서 파일 이름(객체 이름)을 추출
        String objectName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
        log.info("Object name: {}", objectName);
        try {
            // 인증 정보를 사용하여 Storage 객체 생성
            InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
            Storage storage = StorageOptions.newBuilder().setCredentials(GoogleCredentials.fromStream(keyFile)).build().getService();

            // GCS에서 이미지 파일(객체) 삭제
            Blob blob = storage.get(bucketName, objectName);
            if (blob != null) {
                blob.delete();
                log.info("Object {} was deleted from {}", objectName, bucketName);
            } else {
                log.info("The object {} wasn't found in {}", objectName, bucketName);
            }
        } catch (FileNotFoundException e) {
            log.error("Key file for GCP credentials not found: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Error occurred while accessing GCP credentials: {}", e.getMessage());
        }
    }
    public void deleteReply(Long communityReplyId) {    //댓글과 관련된 좋아요 삭제 후 해당 댓글 하나 삭제.
        String likesSql = "DELETE FROM likes WHERE likes_reply_id = ?";
        jdbcTemplate.update(likesSql, communityReplyId);
        String sql = "DELETE FROM community_reply WHERE community_reply_id = ?";
        jdbcTemplate.update(sql, communityReplyId);
    }
    public void editCommunityBoard(Long communityBoardId,  String title, String content, MultipartFile image, Boolean imageChangeCheck) {
        String sql = "SELECT img_url FROM community_board WHERE community_board_id = ?";
        String imgUrl = jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
        if( imageChangeCheck && image==null ){  //게시글의 이미지 삭제
            if(imgUrl == null) log.error("Failed to fetch image URL from community board post"); //기존 url이 없었는데 삭제한다니 오류
            else deleteImageGcs(imgUrl);
            String updateSql = "UPDATE community_board SET title = ?, content = ?, img_url = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, null, communityBoardId);
        } else if (imageChangeCheck) {  //게시글의 이미지 변경 (1.gcs이미지 삭제 후 변경이미지 업로드)
            if(imgUrl != null) deleteImageGcs(imgUrl);
            String newImgUrl = uploadImage(image);
            String updateSql = "UPDATE community_board SET title = ?, content = ?, img_url = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, newImgUrl, communityBoardId);
        } else {   //게시글의 이미지 변경 없이 수정
            String updateSql = "UPDATE community_board SET title = ?, content = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, communityBoardId);
        }
    }
    public void writeReply(Long communityBoardId, String nickname, String content) {
        Instant now = Instant.now();
        String sql = "INSERT INTO community_reply (community_board_id,reply_content,reply_date,writer_nickname) " +
                "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, communityBoardId,content, now, nickname);
        //해당글의 댓글수 증가
        String sql2 = "UPDATE community_board SET views = views + 1 WHERE community_board_id = ?";
        jdbcTemplate.update(sql2, communityBoardId);
    }
    public void plusViews(Long communityBoardId) {
        String sql = "UPDATE community_board SET replies = replies + 1 WHERE community_board_id = ?";
        jdbcTemplate.update(sql, communityBoardId);
    }
    public void plusBoardLikes(Long communityBoardId, String nickname) {
        String sql = "SELECT likes_nickname FROM likes WHERE likes_board_id = ?";
        List<String> nicknames = jdbcTemplate.queryForList(sql, String.class, communityBoardId);

        if (!nicknames.contains(nickname)) {
            String updateSql = "UPDATE community_board SET likes = likes + 1 WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, communityBoardId);
        }
    }
    public void plusReplyLikes(Long communityReplyId, String nickname) {
        String sql = "SELECT likes_nickname FROM likes WHERE likes_reply_id = ?";
        List<String> nicknames = jdbcTemplate.queryForList(sql, String.class, communityReplyId);

        if (!nicknames.contains(nickname)) {
            String updateSql = "UPDATE community_reply SET likes = likes + 1 WHERE community_reply_id = ?";
            jdbcTemplate.update(updateSql, communityReplyId);
        }
    }
    public void deleteReplyBeforeCheck(Long communityReplyId, String nickname) {
        String sql = "SELECT writer_nickname FROM community_reply WHERE community_reply_id = ?";
        String writerNickname = jdbcTemplate.queryForObject(sql, String.class, communityReplyId);

        if (nickname.equals(writerNickname)) {
            deleteReply(communityReplyId);
        }
    }
}