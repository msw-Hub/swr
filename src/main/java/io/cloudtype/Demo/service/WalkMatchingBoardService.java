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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalkMatchingBoardService {
    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService communityBoardService;
    @Autowired
    public UserInfoService userInfoService;

    @Autowired
    public WalkMatchingBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                           @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                           @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                           JdbcTemplate jdbcTemplate) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
        this.projectId = projectId;
        this.jdbcTemplate = jdbcTemplate;
    }

    //받을데이터 nickname=owner_nickname,pet_id=choosing_pet, walk_time, walk_date, latitude, longitude, title,content, status=0
    public void writeWalkPost(String owner_nickname, Long choosing_pet, Integer walk_time, Date walk_date,
                              Double latitude, Double longitude, String title, String content) {
        String sql = "INSERT INTO mydb.walk_matching_board (owner_nickname, choosing_pet, walk_time, walk_date," +
                "latitude, longitude, title, content, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, owner_nickname, choosing_pet, walk_time, walk_date, latitude, longitude, title, content, 0);
    }
    //신청글 목록 반환하기
    public List<Map<String, Object>> getWalkPostList() {
        //현재 날짜 이후의 신청글만 반환 + 신청글이 아직 매칭이 안된 것만 반환
        String sql = "SELECT * FROM mydb.walk_matching_board WHERE walk_date >= CURRENT_DATE() AND status = 0";
        return jdbcTemplate.queryForList(sql);
    }
    //신청글 거리계산(1/3/5km)
    public List<Map<String, Object>> getWalkPostListWithDistance(double clientLatitude, double clientLongitude, double maxDistance) {
        List<Map<String, Object>> walkPostList = getWalkPostList();

        // If the list is empty, return it immediately
        if (walkPostList.isEmpty()) {
            log.info("No walk post found");
            return walkPostList;
        }
        List<Map<String, Object>> filteredWalkPostList = new ArrayList<>();
        for (Map<String, Object> walkPost : walkPostList) {
            double postLatitude = (double) walkPost.get("latitude");
            double postLongitude = (double) walkPost.get("longitude");
            double distance = calculateDistance(clientLatitude, clientLongitude, postLatitude, postLongitude);
            if (distance <= maxDistance) {
                walkPost.put("distance", distance);
                filteredWalkPostList.add(walkPost);
            }
        }
        return filteredWalkPostList;
    }
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to km
    }
}
