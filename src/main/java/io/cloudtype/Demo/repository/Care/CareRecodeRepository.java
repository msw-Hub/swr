package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareRecodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareRecodeRepository extends JpaRepository<CareRecodeEntity, Long> {
}
