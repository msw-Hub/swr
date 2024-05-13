package io.cloudtype.Demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CareMatchingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")  // 외래 키 컬럼 이름 지정
    private UserEntity caregiver;  // 연관된 UserEntity 객체

    @ManyToOne
    @JoinColumn(name = "pet_entity_id")  // 외래 키 컬럼 이름 지정
    private PetEntity pet;  // 연관된 PetEntity 객체

    private LocalDateTime createDate;
    private String title;
    private String content;
    private int status;  // 0: 신청중(매칭전), 1: 매칭완료 2:돌봄시작 3:돌봄정상종료 4:문제발생(비정상종료)

    private String administrativeAddress1;
    private String administrativeAddress2;
    private String administrativeAddress3;
    private String streetNameAddress;
    private String detailAddress;

    @ManyToOne
    @JoinColumn(name = "owner_id")    // 외래 키 컬럼 이름 지정
    private UserEntity owner;  // 산책 동행자

    private String requestMassage;

}
