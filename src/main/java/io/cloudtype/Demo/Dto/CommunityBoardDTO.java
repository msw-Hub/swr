package io.cloudtype.Demo.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CommunityBoardDTO {
    private int id;
    private String title;
    private String content;
    private LocalDateTime createDate;
    private String imgUrl;
    private int likeCount;
    private int commentCount;
    private int viewCount;
}