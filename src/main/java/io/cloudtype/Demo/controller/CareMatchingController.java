package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.entity.CareMatchingEntity;
import io.cloudtype.Demo.service.CareMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/care")
public class CareMatchingController {
    private final CareMatchingService careMatchingService;
    @Autowired
    public CareMatchingController(CareMatchingService careMatchingService){
        this.careMatchingService = careMatchingService;
    }
    //  /mypage/pet/list에서 펫정보를 받아온 이후 고른 펫을 받음
//    @PostMapping("/create/post")
//    public ResponseEntity<?> createPost(@RequestHeader("Authorization") String accessToken,
//                                        @RequestBody CareMatchingEntity careMatchingEntity
//    ) {
//        try {
//            careMatchingService.createPost(accessToken, careMatchingEntity,0);
//            return ResponseEntity.ok("게시글 작성 완료");
//        } catch (IllegalStateException | IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

}
