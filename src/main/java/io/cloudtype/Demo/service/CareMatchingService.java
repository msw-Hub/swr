package io.cloudtype.Demo.service;

import io.cloudtype.Demo.entity.Care.CareImgEntity;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.UnavailableDatesEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.CareImgRepository;
import io.cloudtype.Demo.repository.Care.CarePostRepository;
import io.cloudtype.Demo.repository.Care.UnavailableDatesRepository;
import io.cloudtype.Demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;




@Service
public class CareMatchingService {

    private final UserRepository userRepository;
    private final CarePostRepository carePostRepository;
    private final CareImgRepository careImgRepository;
    private final JWTUtil jwtUtil;
    private final UnavailableDatesRepository unavailableDatesRepository;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public CareMatchingService(UserRepository userRepository, JWTUtil jwtUtil,
            CarePostRepository carePostRepository,
            CareImgRepository careImgRepository,
            UnavailableDatesRepository unavailableDatesRepository
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.carePostRepository = carePostRepository;
        this.careImgRepository = careImgRepository;
        this.unavailableDatesRepository = unavailableDatesRepository;
    }

    @Transactional
    public int createPost(String accessToken, String title, String content,
                           String administrativeAddress1, String administrativeAddress2, String administrativeAddress3, String streetNameAddress, String detailAddress) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //파트너쉽이 있는지 확인
        if (user.getPartnership() != 1) {
            throw new IllegalStateException("파트너쉽이 없습니다");
        }
        //이미 작성한 글이 있는지 확인
        if (carePostRepository.findByCaregiver_Id(user.getId()) != null) {
            throw new IllegalStateException("이미 작성한 글이 있습니다");
        }
        //글 작성
        CarePostEntity carePost = new CarePostEntity();
        carePost.setCaregiver(user);
        carePost.setTitle(title);
        carePost.setContent(content);
        carePost.setAdministrativeAddress1(administrativeAddress1);
        carePost.setAdministrativeAddress2(administrativeAddress2);
        carePost.setAdministrativeAddress3(administrativeAddress3);
        carePost.setStreetNameAddress(streetNameAddress);
        carePost.setDetailAddress(detailAddress);
        carePostRepository.save(carePost);
        return carePost.getId();
    }
    @Transactional
    public void addImage(int carePostId, String imageUrl, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //carePostId로 CarePostEntity 찾고 이미지 저장
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        CareImgEntity careImg = new CareImgEntity();
        careImg.setCarePost(carePost);
        careImg.setImgUrl(imageUrl);
        careImg.setUploadDate(LocalDateTime.now());
        careImgRepository.save(careImg);
    }
    @Transactional
    public void addUnavailableDates(int carePostId, List<String> unavailableDates, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }

        CarePostEntity carePost = carePostRepository.findById(carePostId);

        List<Date> dates = unavailableDates.stream().map(dateStr -> {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, dateFormatter);
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid date format", e);
            }
        }).toList();

        for (Date unavailableDate : dates) {
            UnavailableDatesEntity unavailableDateEntity = new UnavailableDatesEntity();
            unavailableDateEntity.setCarePost(carePost);
            unavailableDateEntity.setUnavailableDate(unavailableDate);
            unavailableDatesRepository.save(unavailableDateEntity);
        }
    }
}
