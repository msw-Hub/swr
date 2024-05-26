package io.cloudtype.Demo.walk.repository;

import io.cloudtype.Demo.walk.entity.WalkMatchingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkMatchingRepository extends JpaRepository<WalkMatchingEntity, Long> {
    List<WalkMatchingEntity> findByStatus(int status);
    WalkMatchingEntity findById(int id);

    WalkMatchingEntity findByUserId(int userId);
    int countByWalkerIdAndStatus(int walkerId, int status);
}
