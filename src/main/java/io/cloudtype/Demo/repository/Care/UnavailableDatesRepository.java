package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.UnavailableDatesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnavailableDatesRepository extends JpaRepository<UnavailableDatesEntity, Long> {
}
