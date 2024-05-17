package io.cloudtype.Demo.Dto.Care;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CareApplyDTO {
    private int carePostId;
    private int petId;
    private int amount;
    private LocalDateTime reservationStartDate;
    private LocalDateTime reservationEndDate;
    private String requestMessage;
}
