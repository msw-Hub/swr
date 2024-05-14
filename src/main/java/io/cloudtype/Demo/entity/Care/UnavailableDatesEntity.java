package io.cloudtype.Demo.entity.Care;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class UnavailableDatesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "care_post_id")  // 외래 키 컬럼 이름 지정
    private CarePostEntity carePost;

    private Date unavailableDate;
}
