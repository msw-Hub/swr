package io.cloudtype.Demo.service;

import io.cloudtype.Demo.entity.Care.CareImgEntity;
import io.cloudtype.Demo.entity.Care.CareMatchingEntity;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.CareImgRepository;
import io.cloudtype.Demo.repository.Care.CarePostRepository;
import io.cloudtype.Demo.repository.PetRepository;
import io.cloudtype.Demo.repository.UserRepository;
import io.cloudtype.Demo.repository.WaiterRepository;
import io.cloudtype.Demo.repository.Walk.WalkMatchingRepository;
import io.cloudtype.Demo.repository.Walk.WalkRecodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CareMatchingService {

    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    private final UserRepository userRepository;
    private final CarePostRepository carePostRepository;
    private final CareImgRepository careImgRepository;
    private final PetRepository petRepository;
    private final JWTUtil jwtUtil;
    private final WalkMatchingRepository walkMatchingRepository;
    private final WaiterRepository waiterRepository;
    private final WalkRecodeRepository walkRecodeRepository;

    @Autowired
    public CareMatchingService(String keyFileName, String projectId, String bucketName,
                               UserRepository userRepository, PetRepository petRepository, JWTUtil jwtUtil,
                               WalkMatchingRepository walkMatchingRepository, WaiterRepository waiterRepository,
                               WalkRecodeRepository walkRecodeRepository,
                                 CarePostRepository carePostRepository,
                                 CareImgRepository careImgRepository
    ) {
        this.keyFileName = keyFileName;
        this.projectId = projectId;
        this.bucketName = bucketName;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.jwtUtil = jwtUtil;
        this.walkMatchingRepository = walkMatchingRepository;
        this.waiterRepository = waiterRepository;
        this.walkRecodeRepository = walkRecodeRepository;
        this.carePostRepository = carePostRepository;
        this.careImgRepository = careImgRepository;
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
}
