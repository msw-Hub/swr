package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareMatchingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareMatchingRepository extends JpaRepository<CareMatchingEntity, Long> {
}
