package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.WalkUsageDTO;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.entity.WalkRecodeEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.UserRepository;
import io.cloudtype.Demo.repository.WalkRecodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsageDetailsService {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final WalkRecodeRepository walkRecodeRepository;
    @Autowired
    public UsageDetailsService(UserRepository userRepository, JWTUtil jwtUtil, WalkRecodeRepository walkRecodeRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.walkRecodeRepository = walkRecodeRepository;
    }
    //유저의 산책이용내역보기
    public List<WalkUsageDTO> getUserWalkUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<WalkRecodeEntity> walkRecodeList = walkRecodeRepository.findByUser_IdOrderByCreateDateDesc(user.getId());
        return walkRecodeList.stream()
                .map(this::getWalkUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    private WalkUsageDTO getWalkUsageDetailSimplified(WalkRecodeEntity walkRecodeEntity) {
        WalkUsageDTO walkUsageDTO = new WalkUsageDTO();
        walkUsageDTO.setId(walkRecodeEntity.getId());
        walkUsageDTO.setUserNickname(walkRecodeEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecodeEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecodeEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecodeEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecodeEntity.getPet().getPetName());
        walkUsageDTO.setWalkTime(walkRecodeEntity.getWalkTime());
        walkUsageDTO.setEndTime(walkRecodeEntity.getEndTime());
        walkUsageDTO.setAmount(walkRecodeEntity.getAmount());
        return walkUsageDTO;
    }

    public WalkUsageDTO getUserWalkUsageDetail(String accessToken, int walkRecodeId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        WalkRecodeEntity walkRecode = walkRecodeRepository.findById(walkRecodeId);
        if (walkRecode == null) {
            throw new IllegalArgumentException("해당 산책기록을 찾을 수 없습니다");
        }
        if (walkRecode.getUser().getId() != user.getId() || walkRecode.getWalker().getId() != user.getId()){
            throw new IllegalArgumentException("해당 산책기록의 작성자가 혹은 산책러가 아닙니다");
        }
        return getWalkUsageDetail(walkRecode);
    }
    private WalkUsageDTO getWalkUsageDetail(WalkRecodeEntity walkRecodeEntity) {
        WalkUsageDTO walkUsageDTO = new WalkUsageDTO();
        walkUsageDTO.setId(walkRecodeEntity.getId());
        walkUsageDTO.setUserNickname(walkRecodeEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecodeEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecodeEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecodeEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecodeEntity.getPet().getPetName());
        walkUsageDTO.setPetImage(walkRecodeEntity.getPet().getPetImage());
        walkUsageDTO.setPetGender(walkRecodeEntity.getPet().getGender());
        walkUsageDTO.setPetSpecies(walkRecodeEntity.getPet().getSpecies());
        walkUsageDTO.setPetBirthYear(walkRecodeEntity.getPet().getBirthYear());
        walkUsageDTO.setWalkTime(walkRecodeEntity.getWalkTime());
        walkUsageDTO.setStartTime(walkRecodeEntity.getStartTime());
        walkUsageDTO.setEndTime(walkRecodeEntity.getEndTime());
        walkUsageDTO.setLatitude(walkRecodeEntity.getLatitude());
        walkUsageDTO.setLongitude(walkRecodeEntity.getLongitude());
        walkUsageDTO.setDetailLocation(walkRecodeEntity.getDetailLocation());
        walkUsageDTO.setTitle(walkRecodeEntity.getTitle());
        walkUsageDTO.setContent(walkRecodeEntity.getContent());
        walkUsageDTO.setStatus(walkRecodeEntity.getStatus());
        walkUsageDTO.setReason(walkRecodeEntity.getReason());
        walkUsageDTO.setAmount(walkRecodeEntity.getAmount());
        return walkUsageDTO;
    }
}
