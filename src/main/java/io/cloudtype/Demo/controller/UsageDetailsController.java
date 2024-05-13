package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.WalkUsageDetailsDTO;
import io.cloudtype.Demo.Dto.WalkUsageSimpleDTO;
import io.cloudtype.Demo.service.UsageDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/usageDetails")
public class UsageDetailsController {
    private final UsageDetailsService usageDetailsService;
    @Autowired
    public UsageDetailsController(UsageDetailsService usageDetailsService) {
        this.usageDetailsService = usageDetailsService;
    }
    //유저의 산책이용내역보기
    @GetMapping("/walk/list")
    public ResponseEntity<?> getUserWalkList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkUsageSimpleDTO> walkUsageList = usageDetailsService.getUserWalkUsageList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkUsageList", walkUsageList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 산책이용상세내역보기
    @PostMapping("/walk/detail")
    public ResponseEntity<?> getUserWalkDetail(@RequestHeader("Authorization") String accessToken,
                                               @RequestParam("walk_recode_id") int walkRecodeId
    ) {
        try {
            WalkUsageDetailsDTO walkUsage = usageDetailsService.getUserWalkUsageDetail(accessToken, walkRecodeId);
            return ResponseEntity.ok(walkUsage);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
