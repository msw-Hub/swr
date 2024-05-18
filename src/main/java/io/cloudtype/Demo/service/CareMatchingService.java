package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.Care.CareApplyDTO;
import io.cloudtype.Demo.entity.Care.CareImgEntity;
import io.cloudtype.Demo.entity.Care.CareMatchingEntity;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.ReservationSchedulerEntity;
import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.CareImgRepository;
import io.cloudtype.Demo.repository.Care.CareMatchingRepository;
import io.cloudtype.Demo.repository.Care.CarePostRepository;
import io.cloudtype.Demo.repository.Care.ReservationSchedulerRepository;
import io.cloudtype.Demo.repository.PetRepository;
import io.cloudtype.Demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class CareMatchingService {

    private final UserRepository userRepository;
    private final CarePostRepository carePostRepository;
    private final CareImgRepository careImgRepository;
    private final JWTUtil jwtUtil;
    private final ReservationSchedulerRepository reservationSchedulerRepository;
    private final PetRepository petRepository;
    private final CareMatchingRepository careMatchingRepository;
    private final CommunityBoardService communityBoardService;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public CareMatchingService(UserRepository userRepository, JWTUtil jwtUtil,
            CarePostRepository carePostRepository,
            CareImgRepository careImgRepository,
            ReservationSchedulerRepository reservationSchedulerRepository,
            PetRepository petRepository,
            CareMatchingRepository careMatchingRepository,
            CommunityBoardService communityBoardService
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.carePostRepository = carePostRepository;
        this.careImgRepository = careImgRepository;
        this.petRepository = petRepository;
        this.careMatchingRepository = careMatchingRepository;
        this.communityBoardService = communityBoardService;
        this.reservationSchedulerRepository = reservationSchedulerRepository;
    }

    @Transactional
    public int createPost(String accessToken, String title, String content,
                           String administrativeAddress1, String administrativeAddress2, String streetNameAddress, String detailAddress) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //파트너쉽이 있는지 확인
        if (user.getPartnership() != 1) {
            throw new IllegalStateException("파트너쉽이 없습니다");
        }
        //이미 작성한 글이 있는지 확인
        if (carePostRepository.findByCaregiver_Id(user.getId()) != null) {
            throw new IllegalStateException("이미 작성한 글이 있습니다");
        }
        //글 작성
        CarePostEntity carePost = new CarePostEntity();
        carePost.setCaregiver(user);
        carePost.setTitle(title);
        carePost.setContent(content);
        carePost.setAdministrativeAddress1(administrativeAddress1);
        carePost.setAdministrativeAddress2(administrativeAddress2);
        carePost.setStreetNameAddress(streetNameAddress);
        carePost.setDetailAddress(detailAddress);
        carePostRepository.save(carePost);
        return carePost.getId();
    }
    @Transactional
    public void addImage(int carePostId, String imageUrl, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        //carePostId로 CarePostEntity 찾고 이미지 저장
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        CareImgEntity careImg = new CareImgEntity();
        careImg.setCarePost(carePost);
        careImg.setImgUrl(imageUrl);
        careImg.setUploadDate(LocalDateTime.now());
        careImgRepository.save(careImg);
    }
    //돌봄글 첫 작성시
    @Transactional
    public void addUnavailableDates(int carePostId, List<String> unavailableDates) {
        CarePostEntity carePost = carePostRepository.findById(carePostId);

        List<Date> dates = unavailableDates.stream().map(dateStr -> {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, dateFormatter);
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid date format", e);
            }
        }).toList();

        for (Date unavailableDate : dates) {
            ReservationSchedulerEntity unavailableDateEntity = new ReservationSchedulerEntity();
            unavailableDateEntity.setCarePost(carePost);
            unavailableDateEntity.setUnavailableDate(unavailableDate);
            reservationSchedulerRepository.save(unavailableDateEntity);
        }
    }

    @Transactional
    public void applyCare(String accessToken, CareApplyDTO careApplyDTO) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(careApplyDTO.getCarePostId());
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() == user.getId()) {
            throw new IllegalStateException("자신의 글에는 신청할 수 없습니다");
        }

        LocalDateTime reservationStartDate = careApplyDTO.getReservationStartDate();
        LocalDateTime reservationEndDate = careApplyDTO.getReservationEndDate();
        if (isDateInUnavailablePeriod(reservationStartDate, reservationEndDate)) {
            throw new IllegalStateException("예약 불가 날짜가 포함되어 있습니다");
        }

        PetEntity pet = petRepository.findById(careApplyDTO.getPetId());
        if(pet == null) {
            throw new IllegalArgumentException("펫을 찾을 수 없습니다");
        }
        int dtoPetOwnerId = pet.getOwner().getId();
        if(dtoPetOwnerId != user.getId()) {
            throw new IllegalArgumentException("자신의 펫이 아닙니다");
        }

        CareMatchingEntity careMatching = new CareMatchingEntity();
        careMatching.setCarePost(carePost);
        careMatching.setPet(pet);
        careMatching.setOwner(user);
        careMatching.setAmount(careApplyDTO.getAmount());
        careMatching.setApplyDate(LocalDateTime.now());
        careMatching.setReservationStartDate(careApplyDTO.getReservationStartDate());
        careMatching.setReservationEndDate(careApplyDTO.getReservationEndDate());
        careMatching.setRequestMessage(careApplyDTO.getRequestMessage());
        careMatchingRepository.save(careMatching);
    }
    private boolean isDateInUnavailablePeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 예약 불가 날짜 확인
        List<ReservationSchedulerEntity> unavailableDates = reservationSchedulerRepository.findByUnavailableDateBetween(start, end);
        return !unavailableDates.isEmpty();
    }
    @Transactional
    public void deleteImages(int carePostId, String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글만 수정할 수 있습니다");
        }
        List<CareImgEntity> careImages = careImgRepository.findByCarePost_Id(carePostId);
        for (CareImgEntity careImage : careImages) {
            communityBoardService.deleteImageGcs(careImage.getImgUrl());
            careImgRepository.delete(careImage);
        }
    }


    @Transactional
    public void updateUnavailableDates(int carePostId, List<String> newUnavailableDates) {
        // 새로운 unavailableDates를 LocalDate 타입으로 변환
        List<LocalDate> newDates = newUnavailableDates.stream()
                .map(dateStr -> LocalDate.parse(dateStr, dateFormatter))
                .toList();

        // 기존 unavailableDates를 조회
        CarePostEntity carePost = carePostRepository.findById(carePostId);

        Set<LocalDate> existingDates = carePost.getUnavailableDates().stream()
                .map(reservation -> reservation.getUnavailableDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toSet());

        Set<LocalDate> newDatesSet = new HashSet<>(newDates);

        // 추가해야 할 날짜 (newDatesSet - existingDates)
        Set<LocalDate> datesToAdd = newDatesSet.stream()
                .filter(date -> !existingDates.contains(date))
                .collect(Collectors.toSet());

        // 삭제해야 할 날짜 (existingDates - newDatesSet)
        Set<LocalDate> datesToRemove = existingDates.stream()
                .filter(date -> !newDatesSet.contains(date))
                .collect(Collectors.toSet());

        // 이용불가 날짜 추가
        for (LocalDate date : datesToAdd) {
            Date dateToAdd = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            boolean isExist = reservationSchedulerRepository.existsByCarePostAndReservationDate(carePost, dateToAdd);
            if(isExist){
                throw new IllegalArgumentException("해당 날짜에 이미 예약이 있어서 이용불가날짜로의 변경이 불가능합니다");
            }else{
                ReservationSchedulerEntity reservation = new ReservationSchedulerEntity();
                reservation.setCarePost(carePost);
                reservation.setUnavailableDate(dateToAdd);
                reservationSchedulerRepository.save(reservation);
            }
        }

        // 이용불가 날짜 - > 이용가능 날짜 변경 (이용불가날짜 리스트에서 삭제)
        for (LocalDate date : datesToRemove) {
            Date dateToRemove = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            int reservations = reservationSchedulerRepository.findByCarePostAndReservationDate(carePost,dateToRemove).getReservations();
            if(reservations == 2){
                throw new IllegalArgumentException("해당 날짜에 이미 예약이 2건 있어서 이용가능날짜로의 변경이 불가능합니다");
            }else{
                reservationSchedulerRepository.deleteByCarePostAndUnavailableDate(carePost, dateToRemove);
            }
        }
    }

    @Transactional
    public void editPost(String accessToken, int carePostId, String title, String content) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CarePostEntity carePost = carePostRepository.findById(carePostId);
        if (carePost == null) {
            throw new IllegalArgumentException("돌봄글을 찾을 수 없습니다");
        }
        if (carePost.getCaregiver().getId() != user.getId()) {
            throw new IllegalStateException("자신의 글만 수정할 수 있습니다");
        }
        carePost.setTitle(title);
        carePost.setContent(content);
        carePostRepository.save(carePost);
    }
    public boolean checkReservation(int carePostId) {
        return careMatchingRepository.existsByCarePost_IdAndStatus(carePostId,0);
    }
}
