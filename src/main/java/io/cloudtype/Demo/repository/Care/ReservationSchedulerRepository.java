package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.ReservationSchedulerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservationSchedulerRepository extends JpaRepository<ReservationSchedulerEntity, Long> {
    List<ReservationSchedulerEntity> findByUnavailableDateBetween(Date start, Date end);
    void deleteByCarePostAndUnavailableDate(CarePostEntity carePost, Date date);

    boolean existsByCarePostAndReservationDate(CarePostEntity carePost, Date reservationDate); // 특정 carePostId와 날짜 기준으로 예약 기록이 존재하는지 확인
    ReservationSchedulerEntity findByCarePostAndReservationDate(CarePostEntity carePost, Date reservationDate); // 특정 carePostId와 날짜 기준으로 예약 기록을 가져옴
}
