package io.cloudtype.Demo.entity.Care;

import io.cloudtype.Demo.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CarePostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "caregiver_id")  // 외래 키 컬럼 이름 지정
    private UserEntity caregiver;  // 연관된 UserEntity 객체

    private LocalDateTime createDate;
    private String title;
    private String content;

    private String administrativeAddress1;
    private String administrativeAddress2;
    private String administrativeAddress3;
    private String streetNameAddress;
    private String detailAddress;

    private int amount;  // 돌봄비용
}
