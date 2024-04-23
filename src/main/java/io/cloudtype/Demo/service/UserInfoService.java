package io.cloudtype.Demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Year;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoService {

    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService communityBoardService;

    // 생성자를 통한 주입
    @Autowired
    public UserInfoService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                JdbcTemplate jdbcTemplate) {
            this.keyFileName = keyFileName;
            this.bucketName = bucketName;
            this.projectId = projectId;
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
    public void saveAdditionalUserInfo(Long userId, String nickName , String phoneNumber, String pinNumber, String birthday) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String updateSql = "UPDATE mydb.user_info SET nickname=?, phone_number = ?, pin_number = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(updateSql, nickName ,phoneNumber, pinNumber, birthday, userId);
    }

    //개인정보 수정시 pin번호를 조회하는 메서드
    public String getPinNumberByUserId(Long userId) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }

        // 데이터베이스에서 userId에 해당하는 사용자의 핀 번호를 조회하는 쿼리 작성
        String sql = "SELECT pin_number FROM mydb.user_info WHERE user_id = ?";

        // 쿼리 실행하여 결과 가져오기
        return jdbcTemplate.queryForObject(sql, String.class, userId);
    }

    //글 삭제시, 게시글아이디로 작성자 확인하는 메서드
    public String getUserIdByCommunityBoardNickname(Long communityBoardId) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }

        // 데이터베이스에서 userId에 해당하는 사용자의 핀 번호를 조회하는 쿼리 작성
        String sql = "SELECT writer_nickname FROM mydb.community_board WHERE community_board_id = ?";

        // 쿼리 실행하여 결과 가져오기
        return jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
    }

    public void updateUserInfo(Long userId, String column, String value, String nowNickname) {
        switch (column) {
            case "nickname" -> {
                String sqlUserInfo = "UPDATE user_info SET nickname = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlUserInfo, value, userId);

                String sqlCommunityBoard = "UPDATE community_board SET writer_nickname = ? WHERE writer_nickname = ?";
                jdbcTemplate.update(sqlCommunityBoard, value, nowNickname);

                String sqlCommunityReply = "UPDATE community_reply SET writer_nickname = ? WHERE writer_nickname = ?";
                jdbcTemplate.update(sqlCommunityReply, value, nowNickname);

                String sqlLike = "UPDATE likes SET likes_nickname = ? WHERE likes_nickname = ?";
                jdbcTemplate.update(sqlLike, value, nowNickname);
            }
            case "pin_number" -> {
                String sqlPinNumber = "UPDATE user_info SET pin_number = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlPinNumber, value, userId);
            }
            case "phone_number" -> {
                String sqlPhoneNumber = "UPDATE user_info SET phone_number = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlPhoneNumber, value, userId);
            }
            default -> throw new IllegalArgumentException("유효하지 않은 column입니다: " + column);
        }
    }
    public List<String> getAllNicknames() {
        String sql = "SELECT nickname FROM mydb.user_info";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    public Long savePetInfo1(String petName, Integer petAge, String species, String imageUrl, String ownerNickname, boolean insertOrUpdate, Long petId){
        int currentYear = Year.now().getValue();
        int birthYear = currentYear - petAge;
        if(insertOrUpdate){
            String sql = "INSERT INTO mydb.pet_info (owner_nickname, pet_profile_image, pet_name, birth_year, species) " + "VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, ownerNickname, imageUrl ,petName, birthYear, species);
            // 삽입된 행의 auto-increment 칼럼의 값을 가져옴
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        else{
            String sql = "UPDATE mydb.pet_info SET pet_profile_image = ?, pet_name = ?, birth_year = ?, species = ? WHERE pet_info_id = ?";
            jdbcTemplate.update(sql, imageUrl, petName, birthYear, species, petId);
            return petId;
        }
    }
    public String savePetInfo2(String ownerNickname, Map<String, Object> petInfo){
        //petInfo로 받은 petId로 ownerNickname을 찾아서 인자로 받은 ownerNickname과 비교
        String sql = "SELECT owner_nickname FROM mydb.pet_info WHERE pet_info_id = ?";
        String ownerNicknameFromDB = jdbcTemplate.queryForObject(sql, String.class, petInfo.get("pet_info_id"));
        //ownerNickname과 ownerNicknameFromDB가 같으면 petInfo를 업데이트
        if(ownerNickname.equals(ownerNicknameFromDB)){
            String updateSql = "UPDATE mydb.pet_info SET weight=?, neutering = ?, animal_hospital=?,vaccination=?,etc=? WHERE pet_info_id = ?";
            jdbcTemplate.update(updateSql, petInfo.get("weight"), petInfo.get("neutering"),
                    petInfo.get("animal_hospital"), petInfo.get("vaccination"), petInfo.get("etc"), petInfo.get("pet_info_id"));
            return "success";
        }else {
            return "해당 pet_info_id의 주인이 아닙니다.";
        }
    }
    public Map<String, Object> petInfoList(String ownerNickname) {
        try {
            String sql = "SELECT * FROM mydb.pet_info WHERE owner_nickname = ?";
            return jdbcTemplate.queryForMap(sql, ownerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 Map 반환
            return new HashMap<>();
        }
    }
    public  Map<String, Object> getPetInfoByPetId(Long petInfoId){
        String sql = "SELECT * FROM mydb.pet_info WHERE pet_info_id = ?";
        return jdbcTemplate.queryForMap(sql, petInfoId);
    }
    public String getImageUrlByPetId(Long petInfoId){
        String sql = "SELECT pet_profile_image FROM mydb.pet_info WHERE pet_info_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, petInfoId);
    }
    public void deletePetInfo(Long petInfoId, String imageUrl){
        communityBoardService.deleteImageGcs(imageUrl);
        String sql = "DELETE FROM mydb.pet_info WHERE pet_info_id = ?";
        jdbcTemplate.update(sql, petInfoId);
    }
    public Map<String, Object> getPetInfoByOwnerNickname(String ownerNickname) {
        try {
            String sql = "SELECT pet_info_id, pet_profile_image, pet_name FROM mydb.pet_info WHERE owner_nickname = ?";
            return jdbcTemplate.queryForMap(sql, ownerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 Map 반환
            return new HashMap<>();
        }
    }
}
