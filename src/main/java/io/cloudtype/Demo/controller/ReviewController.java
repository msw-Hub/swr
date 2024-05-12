package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.WalkReviewDTO;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/review")
public class ReviewController {
    private final CommunityBoardService communityBoardService;
    private final ReviewService reviewService;
    @Autowired
    public ReviewController(CommunityBoardService communityBoardService,
                            ReviewService reviewService
    ) {
        this.communityBoardService = communityBoardService;
        this.reviewService = reviewService;
    }
    //유저의 산책리뷰쓰기
    @PostMapping("create/walk")
    public ResponseEntity<?> createWalkReview(@RequestHeader("Authorization") String accessToken,
                                          @ModelAttribute("walk_recode_id") int walkRecodeId,
                                          @ModelAttribute("content") String content,
                                          @ModelAttribute("rating") double rating,
                                          @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 후기 작성
            reviewService.createReview(accessToken, walkRecodeId, content, rating, imageUrl);
            return ResponseEntity.ok("후기 작성 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 산책 리뷰내역 보기
    @GetMapping("user/walk/list")
    public ResponseEntity<?> getUserWalkReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkReviewDTO> walkReviewList = reviewService.getUserWalkReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkReviewList", walkReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 특정 산책 리뷰 삭제
    @GetMapping("delete/walk")
    public ResponseEntity<?> deleteWalkReview(@RequestHeader("Authorization") String accessToken,
                                             @RequestParam("walk_review_id") int walkReviewId
    ) {
        try {
            reviewService.deleteWalkReview(accessToken, walkReviewId);
            return ResponseEntity.ok("후기 삭제 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너의 산책 리뷰 내역보기
    @GetMapping("partner/walk/list")
    public ResponseEntity<?> getPartnerWalkReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkReviewDTO> walkReviewList = reviewService.getPartnerWalkReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkReviewList", walkReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
