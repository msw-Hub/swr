package io.cloudtype.Demo.Dto.Walk;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WalkMatchingDTO {
    private int id;
    private String userNickname;
    private int petId;
    private int walkTime;
    private LocalDateTime createDate;
    private double latitude;
    private double longitude;
    private String detailLocation;
    private String title;
    private String content;
    private double distance;
}