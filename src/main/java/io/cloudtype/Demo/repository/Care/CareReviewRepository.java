package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareReviewRepository extends JpaRepository<CareReviewEntity, Long> {
}
