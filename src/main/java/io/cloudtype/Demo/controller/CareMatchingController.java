package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.entity.Care.CareMatchingEntity;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.service.CareMatchingService;
import io.cloudtype.Demo.service.CommunityBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/care")
public class CareMatchingController {
    private final CareMatchingService careMatchingService;
    private final CommunityBoardService communityBoardService;
    @Autowired
    public CareMatchingController(CareMatchingService careMatchingService,
                                  CommunityBoardService communityBoardService){
        this.communityBoardService = communityBoardService;
        this.careMatchingService = careMatchingService;
    }
    //  /mypage/pet/list에서 펫정보를 받아온 이후 고른 펫을 받음
    @PostMapping("/create/post")
    public ResponseEntity<?> createPost(@RequestHeader("Authorization") String accessToken,
                                        @ModelAttribute("title") String title,
                                        @ModelAttribute("content") String content,
                                        @ModelAttribute("administrativeAddress1") String administrativeAddress1,
                                        @ModelAttribute("administrativeAddress2") String administrativeAddress2,
                                        @ModelAttribute("administrativeAddress3") String administrativeAddress3,
                                        @ModelAttribute("streetNameAddress") String streetNameAddress,
                                        @ModelAttribute("detailAddress") String detailAddress,
                                        @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                        @RequestPart(value = "unavailableDates", required = false) List<Date> unavailableDates
    ) {
        try {
            int carePostId = careMatchingService.createPost(accessToken, title, content,
                    administrativeAddress1, administrativeAddress2, administrativeAddress3, streetNameAddress, detailAddress);

            if (images != null) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String imageUrl = communityBoardService.uploadImage(image); // 이미지를 저장하고 URL을 반환하는 메서드
                        careMatchingService.addImage(carePostId, imageUrl, accessToken); // 이미지 URL을 CareImgEntity에 저장
                    }
                }
            }
            if (unavailableDates != null) {
                careMatchingService.addUnavailableDates(carePostId, unavailableDates, accessToken);
            }
            return ResponseEntity.ok("돌봄글 작성 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
