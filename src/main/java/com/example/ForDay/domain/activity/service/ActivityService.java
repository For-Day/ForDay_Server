package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.activity.dto.FastAPIHobbyCardReqDto;
import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.activity.dto.response.GetAiRecommendItemsResDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.dto.request.FastAPIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.CollectActivityResDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {
    private static final Integer STICKER_COMPLETE_COUNT = 66;
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final S3Service s3Service;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final HobbyCardRepository hobbyCardRepository;
    private final HobbyRepository hobbyRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final RestTemplate restTemplate;
    private final ActivityRecommendItemRepository recommendItemRepository;

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;

    @Transactional
    public RecordActivityResDto recordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {

        User currentUser = userUtil.getCurrentUser(user);
        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();
        Long hobbyId = hobby.getId();

        checkHobbyInProgressStatus(hobby); // 진행 중인 취미에 대해서만 활동 기록 가능

        // 기간 설정 66일 이고 이미 스티커를 다 채운 상황이면 기록 불가
        if (isCheckStickerFull(hobby)) {
            log.warn("[RecordActivity] 기록 불가 - 이미 스티커를 모두 채움. HobbyId: {}", hobbyId);
            throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        }

        String redisKey = todayRecordRedisService.createRecordKey(currentUser.getId(), hobby.getId());
        if (todayRecordRedisService.hasKey(redisKey)) { // 해당 취미에 대해 오늘 기록한 활동이 있는지 확인
            log.warn("[RecordActivity] 중복 기록 시도 - UserId: {}, HobbyId: {}",
                    currentUser.getId(), hobby.getId());
            throw new CustomException(ErrorCode.ALREADY_RECORDED_TODAY);
        }

        if (StringUtils.hasText(reqDto.getImageUrl())) {  // 이미지를 등록하고자 한다면 해당 이미지가 s3상에 잘 업로드 되었는지 확인
            String s3Key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl()); // 이미지url에서 key를 추출
            if (!s3Service.existsByKey(s3Key)) { // 해당 key를 가진 객체가 존재하는지 확인
                log.error("[RecordActivity] S3 이미지 부재 - Key: {}", s3Key);
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND); // 존재하지 않으면 예외 발생
            }
        }

        ActivityRecord activityRecord = ActivityRecord.builder()
                .hobby(hobby)
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .imageUrl(reqDto.getImageUrl())
                .build();

        activity.record(); // 해당 취미와 활동에 대해 스티커 + 1
        currentUser.obtainSticker(); // 해당 유저가 모은 스티커 + 1
        activityRecordRepository.save(activityRecord);

        log.info("[RecordActivity] 기록 저장 성공 - RecordId: {}, 현재 스티커 수: {}",
                activityRecord.getId(), hobby.getCurrentStickerNum());

        todayRecordRedisService.setDataExpire(redisKey, "recorded");

        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            log.info("[RecordActivity] 취미 완주 달성! 취미 카드 생성을 시작합니다. HobbyId: {}", hobbyId);
            createHobbyCard(hobby, currentUser);
        }

        boolean extensionCheckRequired = isCheckStickerFull(hobby);

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                hobby.getId(),
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                reqDto.getSticker(),
                activityRecord.getMemo(),
                extensionCheckRequired
        );
    }

    @Transactional
    public RecordActivityResDto testRecordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();

        if (isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        checkHobbyInProgressStatus(hobby); // 진행 중인 취미에 대해서만 활동 기록 가능

        if (StringUtils.hasText(reqDto.getImageUrl())) {  // 이미지를 등록하고자 한다면 해당 이미지가 s3상에 잘 업로드 되었는지 확인
            String s3Key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl()); // 이미지url에서 key를 추출
            if (!s3Service.existsByKey(s3Key)) { // 해당 key를 가진 객체가 존재하는지 확인
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND); // 존재하지 않으면 예외 발생
            }
        }

        ActivityRecord activityRecord = ActivityRecord.builder()
                .hobby(hobby)
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .imageUrl(reqDto.getImageUrl())
                .build();

        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);


        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            createHobbyCard(hobby, currentUser);
        }
        boolean extensionCheckRequired = isCheckStickerFull(hobby);

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                hobby.getId(),
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                reqDto.getSticker(),
                activityRecord.getMemo(),
                extensionCheckRequired
        );
    }

    private void createHobbyCard(Hobby hobby, User currentUser) {
        Long hobbyId = hobby.getId();
        String userId = currentUser.getId();

        log.info("[HobbyCard] 생성 프로세스 시작 - 사용자: {}, 취미: {}", userId, hobbyId);

        // fast api 서버와 통신하여 취미 카드 content 생성하기
        FastAPIHobbyCardReqDto requestDto = FastAPIHobbyCardReqDto.builder()
                .userHobbyId(hobbyId)
                .build();

        // 3. FastAPI 호출
        String url = fastApiBaseUrl + "/ai/hobby-card/content";
        try {
            log.info("[HobbyCard] AI 콘텐츠 생성 요청 - URL: {}", url);
            FastAPIHobbyCardResDto response = restTemplate.postForObject(url, requestDto, FastAPIHobbyCardResDto.class);

            if (response == null || response.getContent().isEmpty()) {
                log.error("[HobbyCard] AI 응답 데이터가 유효하지 않음 - 사용자: {}", userId);
                throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
            }

            String hobbyCardContent = response.getContent();

            // 취미 카드 전용 url 생성
            String coverImageUrl =
                    (hobby.getCoverImageUrl() == null)
                            ? activityRecordRepository.findLatestImageRecord(hobby.getId())
                            .map(ActivityRecord::getImageUrl)
                            .orElse("https://your-bucket.s3.../default-hobby-image.png") // 여기 수정 예정
                            : hobby.getCoverImageUrl();

            String hobbyCardImageUrl = null;
            if(StringUtils.hasText(coverImageUrl)) {
                try {
                    String coverImageKey = s3Service.extractKeyFromFileUrl(coverImageUrl);
                    String hobbyCardImageKey = coverImageKey.replace("cover_image/temp/", "hobby_card/temp/");

                    s3Service.copyObject(coverImageKey, hobbyCardImageKey);
                    hobbyCardImageUrl = s3Service.createFileUrl(hobbyCardImageKey);

                    log.info("[HobbyCard] S3 이미지 복사 완료 - {} -> {}", coverImageKey, hobbyCardImageKey);
                } catch (Exception s3Ex) {
                    log.warn("[HobbyCard] S3 이미지 처리 중 오류 발생 (프로세스는 계속됨) - {}", s3Ex.getMessage());
                }
            }

            HobbyCard hobbyCard = HobbyCard.builder()
                    .user(currentUser)
                    .hobby(hobby)
                    .content(hobbyCardContent)
                    .imageUrl(hobbyCardImageUrl)
                    .build();
            hobbyCardRepository.save(hobbyCard);
            currentUser.obtainHobbyCard();

            log.info("[HobbyCard] 생성 완료 - 카드ID: {}, 사용자: {}", hobbyCard.getId(), userId);

            currentUser.obtainHobbyCard();
        } catch (Exception e) {
            todayRecordRedisService.deleteTodayRecordKey(currentUser.getId(), hobby.getId());
            log.error("[AI-HOBBY-CARD][ERROR] FastAPI 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private static boolean isCheckStickerFull(Hobby hobby) {
        if (hobby.getCurrentStickerNum() == null || hobby.getGoalDays() == null) {
            return false;
        }
        return Objects.equals(hobby.getCurrentStickerNum().intValue(), STICKER_COMPLETE_COUNT)
                && Objects.equals(hobby.getGoalDays().intValue(), STICKER_COMPLETE_COUNT);
    }

    @Transactional
    public MessageResDto updateActivity(
            Long activityId,
            UpdateActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        log.info("[ActivityService] 활동 수정 요청 - activityId={}, content={}",
                activityId, reqDto.getContent());
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = getActivityByUserId(activityId, currentUser.getId());

        // 진행 중인 취미가 아니면 활동 수정 불가
        checkHobbyInProgressStatus(activity.getHobby());

        String beforeContent = activity.getContent();
        activity.updateContent(reqDto.getContent());

        log.info("[ActivityService] 활동 수정 완료 - activityId={}, userId={}, before='{}', after='{}'",
                activityId,
                currentUser.getId(),
                beforeContent,
                reqDto.getContent()
        );

        return new MessageResDto("활동이 정상적으로 수정되었습니다.");
    }

    @Transactional
    public MessageResDto deleteActivity(Long activityId, CustomUserDetails user) {
        log.info("[ActivityService] 활동 삭제 요청 - activityId={}", activityId);
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = getActivityByUserId(activityId, currentUser.getId());

        // 삭제 가능 여부 (해당 활동으로 획득한 스티커가 없을 때)
        if (!activity.isDeletable()) {
            log.warn("[ActivityService] 활동 삭제 불가 (deletable=false) - activityId={}, userId={}",
                    activityId, currentUser.getId());
            throw new CustomException(ErrorCode.ACTIVITY_NOT_DELETABLE);
        }

        // 진행 중인 취미가 아니면 활동 삭제 불가
        checkHobbyInProgressStatus(activity.getHobby());
        activityRepository.delete(activity);

        log.info("[ActivityService] 활동 삭제 완료 - activityId={}, userId={}",
                activityId, currentUser.getId()
        );

        return new MessageResDto("활동이 정상적으로 삭제되었습니다.");
    }

    @Transactional
    public CollectActivityResDto collectActivity(Long hobbyId, Long activityId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();

        // [시작] 활동 담기 요청 로그
        log.info("[Activity Collect] 시작 - 사용자: {}, 취미ID: {}, 활동ID: {}",
                currentUserId, hobbyId, activityId);

        Hobby hobby = hobbyRepository.findByIdAndUserId(hobbyId, currentUserId)
                .orElseThrow(() -> {
                    log.warn("[Activity Collect] 실패 - 취미를 찾을 수 없음. 취미ID: {}, 사용자ID: {}", hobbyId, currentUserId);
                    return new CustomException(ErrorCode.HOBBY_NOT_FOUND);
                });

        ActivityRecordCollectInfo activity = activityRepository.getCollectActivityInfo(activityId)
                .orElseThrow(() -> {
                    log.warn("[Activity Collect] 실패 - 원본 활동을 찾을 수 없음. 활동ID: {}", activityId);
                    return new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
                });

        checkBlockedAndDeletedUser(currentUserId, activity.getUserId(), activity.isUserDeleted());
        log.info("[Activity Collect] 검증 완료 - 활동 소유자ID: {}", activity.getUserId());

        Activity build = Activity.builder()
                .user(currentUser)
                .hobby(hobby)
                .content(activity.getContent())
                .aiRecommended(false)
                .build();

        Activity savedActivity = activityRepository.save(build);


        log.info("[Activity Collect] 완료 - 생성된 활동ID: {}, 저장된 취미: {}",
                savedActivity.getId(), hobby.getHobbyName());

        return new CollectActivityResDto(hobby.getId(), hobby.getHobbyName(), build.getId(), build.getContent(), "활동이 정상적으로 담겼습니다.");
    }

    @Transactional(readOnly = true)
    public GetAiRecommendItemsResDto getAiRecommendItems(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();

        // 메세지 조회 -> 논의 한 후 수정 예정

        // 1. 현재 유저의 현재 진행 중인 취미 조회
        List<Hobby> progressHobbies = hobbyRepository.findAllByUserIdAndStatusOrderByIdDesc(
                currentUserId,
                HobbyStatus.IN_PROGRESS
        );

        if (progressHobbies.isEmpty()) {
            return new GetAiRecommendItemsResDto(new GetAiRecommendItemsResDto.MessageDto(), Collections.emptyList());
        }

        // 2. 오늘 날짜 범위 설정
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        // 3. 오늘 생성된 추천 아이템 조회
        List<ActivityRecommendItem> items = recommendItemRepository.findAllByHobbiesAndDate(
                progressHobbies, startOfToday, endOfToday
        );

        // 4. DTO 변환
        List<GetAiRecommendItemsResDto.ItemDto> itemDtos = items.stream()
                .map(item -> new GetAiRecommendItemsResDto.ItemDto(
                        item.getId(),
                        item.getHobby().getId(),
                        item.getHobby().getHobbyName(),
                        item.getContent(),
                        item.getDescription()
                ))
                .toList();

        return new GetAiRecommendItemsResDto(new GetAiRecommendItemsResDto.MessageDto(), itemDtos);
    }

    // 유틸 클래스
    private Activity getActivityByUserId(Long activityId, String userId) {
        return activityRepository.findByIdAndUserId(activityId, userId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void checkHobbyInProgressStatus(Hobby hobby) {
        if (!hobby.getStatus().equals(HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if (friendRelationRepository.existsByFriendship(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByFriendship(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        // 타겟유저가 탈퇴한 회원인 경우
        if (deleted) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
    }
}
