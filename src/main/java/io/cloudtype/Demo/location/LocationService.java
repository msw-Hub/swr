package io.cloudtype.Demo.location;

import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.cloudtype.Demo.walk.entity.WalkMatchingEntity;
import io.cloudtype.Demo.walk.repository.WalkMatchingRepository;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final WalkMatchingRepository walkMatchingRepository;
    private final UserRepository userRepository;

    public LocationService(WalkMatchingRepository walkMatchingRepository, UserRepository userRepository){
        this.walkMatchingRepository = walkMatchingRepository;
        this.userRepository  = userRepository;
    }
    String checkCondition(String PartnerName){
        UserEntity partner = userRepository.findByUsername(PartnerName);
        WalkMatchingEntity matchingEntity = walkMatchingRepository.findByWalker_Id(partner.getId());
        if(matchingEntity == null){
            throw new IllegalArgumentException("생성된 산책 정보 없음");
        }
        if(matchingEntity.getStatus() == 0){
            throw new IllegalArgumentException("진행중인 산책 정보 없음");
        }
        return matchingEntity.getUser().getUsername();
    }
}