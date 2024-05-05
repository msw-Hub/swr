package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.CommunityBoardDTO;
import io.cloudtype.Demo.Dto.JoinDetailsDTO;
import io.cloudtype.Demo.Dto.PetDTO;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.MyPageService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final CommunityBoardService communityBoardService;

    public MyPageController(MyPageService myPageService, CommunityBoardService communityBoardService) {
        this.myPageService = myPageService;
        this.communityBoardService = communityBoardService;
    }
    @PostMapping("")
    public ResponseEntity<?> myPage(@RequestHeader("Authorization") String accessToken,
                                    @RequestBody @NotNull Map<String, String> requestBody
    ) {
        try {
            // 프론트로부터 받은 핀 번호
            int pinNumber = Integer.parseInt(requestBody.get("pinNumber"));
            // 핀 번호 검증
            boolean result = myPageService.checkPinNumber(accessToken, pinNumber);
            if (!result) {
                return ResponseEntity.badRequest().body("핀번호가 일치하지 않습니다.");
            }
            // 유저 정보 반환
            return ResponseEntity.ok(myPageService.getUserInfoByUsername(accessToken));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("핀번호는 숫자로 입력해주세요.");
        }
    }
    @PostMapping("/edit/info")
    public ResponseEntity<String> editInfo(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody JoinDetailsDTO joinDetailsDTO
    ) {
        try {
            // 유저 정보 수정
            myPageService.editInfo(accessToken, joinDetailsDTO.getNickname(),
                    joinDetailsDTO.getPhoneNumber(), joinDetailsDTO.getPinNumber());
            return ResponseEntity.ok("정보 수정 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/edit/password")
    public ResponseEntity<String> editPassword(@RequestHeader("Authorization") String accessToken,
                                               @RequestBody Map<String, String> requestBody
    ) {
        try{
            myPageService.editPassword(accessToken, requestBody.get("password"));
            return ResponseEntity.ok("비밀번호 수정 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/add/step1")
    public ResponseEntity<String> petAddStep1(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @RequestPart("pet_profile_image") MultipartFile petProfileImage
    ) {
        try {
            String imageUrl = null;
            if(petProfileImage != null){
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(petProfileImage);
            }
            // 유저의 반려동물 정보 추가
            int petId = myPageService.addPetStep1(accessToken, petName, petAge, species, imageUrl);
            //petId를 반환
            Map<String, Object> response = new HashMap<>();
            response.put("petId", petId);
            return ResponseEntity.ok(response.toString());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/add/step2")
    public ResponseEntity<String> petAddStep2(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody PetDTO petDTO
    ) {
        try {
            //주인인지 체크
            myPageService.checkOwner(accessToken, petDTO.getId());
            // 유저의 반려동물 정보 추가
            myPageService.addPetStep2(accessToken, petDTO);
            return ResponseEntity.ok("반려동물 추가정보 등록 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/edit/step1")
    public ResponseEntity<String> petEditStep1(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_id") int petId,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @ModelAttribute("image_change_check") Boolean imageChangeCheck,
            @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            //주인인지 체크
            myPageService.checkOwner(accessToken, petId);
            String imageUrl = myPageService.findImageUrlById(petId);
            if(imageChangeCheck && image == null){
                throw new IllegalArgumentException("이미지 변경시 반드시 이미지를 첨부해야 합니다.");
            } else if (imageChangeCheck) {  //이미지 삭제후 새로운 이미지 등록후 url받아오기
                if(imageUrl != null) communityBoardService.deleteImageGcs(imageUrl);
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 반려동물 정보 수정
            myPageService.editPetStep1(petId, petName, petAge, species, imageUrl);
            return ResponseEntity.ok("반려동물 기본정보 수정 완료, 추가 정보 입력으로 이동");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/edit/step2")
    public ResponseEntity<String> petEditStep2(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody PetDTO petDTO
    ) {
        try {
            //주인인지 체크
            myPageService.checkOwner(accessToken, petDTO.getId());

            // 유저의 반려동물 정보 수정 (add2와 동일)
            myPageService.addPetStep2(accessToken, petDTO);
            return ResponseEntity.ok("반려동물 추가정보 수정 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/pet/list")
    public ResponseEntity<Map<String, Object>> petList(@RequestHeader("Authorization") String accessToken
    ) {
        try{
            List<PetDTO> petList = myPageService.getPetList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("petList", petList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage()); // 오류 메시지를 응답에 포함
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @PostMapping("/pet/remove")
    public ResponseEntity<String> petRemove(@RequestHeader("Authorization") String accessToken,
                                            @RequestParam("pet_id") int petId
    ) {
        try {
            // 반려동물 삭제
            myPageService.removePet(accessToken, petId);
            return ResponseEntity.ok("반려동물 삭제 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
/*
    @Operation(summary = "자신이 작성한 산책글보기", description = "walk_post값에 따라 반환값이 달라요")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_post",description = " 0 = 매칭전 / 1=진행중 / 2= 진행완료(과거) / Int", required = true)
    @ApiResponse(responseCode = "200", description = "작성한 글목록 반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/walk/waiting/writer")
    public ResponseEntity<List<Map<String, Object>>> myWalkPost(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Integer walkPost = (Integer) requestBody.get("walk_post");

            // 작성한 산책글 가져오기
            List<Map<String, Object>> responseList = new ArrayList<>();

            //0 = 매칭전 / 1=진행중 / 2= 진행완료
            if (walkPost == 0) {
                Map<String, Object> walkPostInfo = userInfoService.getWalkPostByNickname(nickname, walkPost);
                responseList.add(walkPostInfo);
                //매칭전에는 신청자의 닉네임과 평점, 신청현황id 가 같이 보내집니다.
                List<Map<String, Object>> waitingListInfo = userInfoService.getWaitListByNickname(nickname);
                responseList.addAll(waitingListInfo);
            } else if (walkPost == 1) {
                Map<String, Object> walkPostInfo = userInfoService.getWalkPostByNickname(nickname, walkPost);
                responseList.add(walkPostInfo);
            } else if (walkPost == 2) {
                List<Map<String, Object>> walkPostInfo = userInfoService.getWalkPostByNickname2(nickname);
                responseList.addAll(walkPostInfo);
            } else {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(responseList);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "파트너가 신청한 글 보기(매칭전이거나 진행중인글)", description = "status값이 0이면 매칭전, 1이면 진행중인 글을 보여줍니다. 결국 0~1개만반환")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "신청한 글정보 반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/walk/waiting/partner")
    public ResponseEntity<Map<String, Object>> myWalkWaitingPost(
            @RequestHeader("Authorization") String accessToken
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String partnerNickname = (String) dbUserInfo.get("nickname");

            //파트너(본인)닉네임으로 신청한글 정보조회 0~1개뿐
            Map<String, Object> walkPostInfo = userInfoService.getWaitListByWaiterNickname(partnerNickname);
            return ResponseEntity.ok(walkPostInfo);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
*/