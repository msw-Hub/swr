package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.CareMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/create/post")

}
