package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CarePostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarePostRepository extends JpaRepository<CarePostEntity, Long> {
    CarePostEntity findByCaregiver_Id(int caregiverId);
    CarePostEntity findById(int id);
}
