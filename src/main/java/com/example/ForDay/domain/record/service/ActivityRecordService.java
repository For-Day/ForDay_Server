package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.service.TodayRecordRedisService;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.recent.service.RecentRedisService;
import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.entity.ActivityRecordScarp;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final S3Service s3Service;
    private final ActivityRepository activityRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final ActivityRecordReportRepository activityRecordReportRepository;
    private final HobbyRepository hobbyRepository;
    private final ActivityRecordReactionRepository activityRecordReactionRepository;
    private final RecentRedisService recentRedisService;
    private final S3Util s3Util;
    private final ActivityRecordReactionRepository reactionRepository;
    private final ActivityRecordReportRepository reportRepository;
    private final ActivityRecordScrapRepository scrapRepository;
    private final TodayRecordRedisService todayRecordRedisService;

    @Transactional(readOnly = true)
    public GetRecordDetailResDto getRecordDetail(Long recordId, CustomUserDetails user) {
        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if(detail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();
        boolean isRecordOwner = Objects.equals(currentUserId, detail.writerId());

        // 차단 여부와 탈퇴 회원 여부 확인
        checkBlockedAndDeletedUser(currentUserId, detail.writerId(), detail.writerDeleted());

        if (!isRecordOwner) {
            validateRecordAuthority(detail.visibility(), detail.writerId(), currentUserId);
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);

        GetRecordDetailResDto.UserReactionDto userReaction = createUserReactionDto(summaries, currentUserId);
        GetRecordDetailResDto.NewReactionDto newReaction = createNewReactionDto(summaries, isRecordOwner);

        boolean scraped= activityRecordScrapRepository.existsByActivityRecordIdAndUserId(detail.recordId(), currentUserId);

        return buildGetRecordDetailResDtoFromDto(detail, isRecordOwner, newReaction, userReaction, scraped);
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(
            Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size
    ) {
        // 1. 엔티티 전체 대신 권한 확인용 DTO만 조회 (Fetch Join 제거 효과)
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        String currentUserId = userUtil.getCurrentUser(user).getId();

        checkBlockedAndDeletedUser(currentUserId, recordDetail.writerId(), recordDetail.writerDeleted());

        boolean isRecordOwner = Objects.equals(currentUserId, recordDetail.writerId());

        // 2. 이미 가져온 DTO 정보로 권한 검증
        validateRecordAuthority(recordDetail.visibility(), recordDetail.writerId(), currentUserId);

        // 3. 리포지토리에서 DTO(ReactionUserInfo)로 직접 조회하여 N+1 및 오버페칭 방지
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers =
                recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);

        // 4. 다음 페이지 여부 확인
        boolean hasNext = reactionUsers.size() > size;
        if (hasNext) reactionUsers.remove(size.intValue());

        // 5. 게시글 주인인 경우에만 벌크 업데이트 실행
        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        String nextLastUserId = reactionUsers.isEmpty() ? null : reactionUsers.get(reactionUsers.size() - 1).getUserId();

        return new GetRecordReactionUsersResDto(type, reactionUsers, hasNext, nextLastUserId);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);

        checkBlockedAndDeletedUser(currentUser.getId(), activityRecord.getUser().getId(), activityRecord.getUser().isDeleted());

        validateRecordAuthority(activityRecord.getVisibility(), activityRecord.getUser().getId(), currentUser.getId());

        if (recordReactionRepository.existsByActivityRecordAndReactedUserAndReactionType(activityRecord, currentUser, type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }

        recordReactionRepository.save(ActivityRecordReaction.builder()
                .activityRecord(activityRecord)
                .reactedUser(currentUser)
                .reactionType(type)
                .readWriter(false)
                .build());

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", type, recordId);
    }

    @Transactional
    public UpdateRecordVisibilityResDto updateRecordVisibility(Long recordId, UpdateRecordVisibilityReqDto reqDto, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        verifyRecordOwner(activityRecord, userUtil.getCurrentUser(user));

        RecordVisibility previous = activityRecord.getVisibility();
        RecordVisibility next = reqDto.getVisibility();

        if (previous == next) {
            return new UpdateRecordVisibilityResDto("이미 설정된 공개 범위입니다.", previous, next);
        }

        activityRecord.updateVisibility(next);
        return new UpdateRecordVisibilityResDto("공개 범위가 정상적으로 변경되었습니다.", previous, next);
    }

    @Transactional
    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        ActivityRecordReaction reaction = recordReactionRepository
                .findByActivityRecordAndReactedUserAndReactionType(activityRecord, userUtil.getCurrentUser(user), type)
                .orElseThrow(() -> new CustomException(ErrorCode.REACTION_NOT_FOUND));

        recordReactionRepository.delete(reaction);
        return new CancelReactToRecordResDto("리액션이 정상적으로 취소되었습니다.", type, recordId);
    }

    @Transactional
    public UpdateActivityRecordResDto updateActivityRecord(Long recordId, UpdateActivityRecordReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();
        ActivityRecord activityRecord = activityRecordRepository.findByIdAndUserId(recordId, currentUserId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
        Activity activity = activityRepository.findByIdAndUserId(reqDto.getActivityId(), currentUserId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        String newImageUrl = reqDto.getImageUrl();
        if(Strings.hasText(newImageUrl)) {
            if(!activityRecord.getImageUrl().equals(newImageUrl)) {
                // 이미지 수정하는 경우
                String s3Key = s3Service.extractKeyFromFileUrl(newImageUrl);
                if(!s3Service.existsByKey(s3Key)) {
                    throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
                }
            }
            activityRecord.updateRecord(activity, reqDto.getSticker(), reqDto.getMemo(), reqDto.getVisibility(), reqDto.getImageUrl());
            activityRecordRepository.save(activityRecord);
        }
        return new UpdateActivityRecordResDto("활동 기록이 정상적으로 수정되었습니다.", activity.getId(), activity.getContent(), activityRecord.getSticker(), activityRecord.getMemo(), activityRecord.getImageUrl(), activityRecord.getVisibility());
    }

    @Transactional
    public DeleteActivityRecordResDto deleteActivityRecord(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();
        ActivityRecord activityRecord = activityRecordRepository.findByIdAndUserId(recordId, currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        // 이미 삭제된 경우 예외 처리
        if(activityRecord.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_RECORD);
        }

        reactionRepository.deleteByActivityRecord(activityRecord);
        reportRepository.deleteByReportedRecord(activityRecord);
        scrapRepository.deleteByActivityRecord(activityRecord);

        String deleteImageUrl = activityRecord.getImageUrl();

        boolean isToday = activityRecord.getCreatedAt().toLocalDate().equals(LocalDate.now());

        if (isToday) {
            activityRecord.getActivity().deleteRecord();
            activityRecord.getHobby().deleteRecord();
            todayRecordRedisService.deleteTodayRecordKey(currentUserId, activityRecord.getHobby().getId());
            activityRecordRepository.delete(activityRecord);

        } else {
            activityRecord.deleteRecord();
        }

        return new DeleteActivityRecordResDto("활동 기록이 정상적으로 삭제되었습니다.", activityRecord.getId(), deleteImageUrl);
    }

    @Transactional
    public AddActivityRecordScrapResDto addActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecordWithUserDto activityRecordDto = activityRecordRepository.getActivityRecordWithUser(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        checkBlockedAndDeletedUser(currentUser.getId(), activityRecordDto.getWriterId(), activityRecordDto.isWriterDeleted());
        validateRecordAuthority(activityRecordDto.getVisibility(), activityRecordDto.getWriterId(), currentUser.getId());

        if(activityRecordScrapRepository.existsByActivityRecordIdAndUserId(recordId, currentUser.getId())) {
            throw new CustomException(ErrorCode.DUPLICATE_SCRAP);
        }

        ActivityRecord recordProxy = activityRecordRepository.getReferenceById(recordId);

        activityRecordScrapRepository.save(ActivityRecordScarp.builder()
                .activityRecord(recordProxy)
                .user(currentUser)
                .build());

        return new AddActivityRecordScrapResDto("스크랩이 완료되었습니다.", recordId, true);
    }

    @Transactional
    public DeleteActivityRecordScrapResDto deleteActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord activityRecord = getActivityRecord(recordId);

        Optional<ActivityRecordScarp> scarp = activityRecordScrapRepository.findByActivityRecordIdAndUserId(activityRecord.getId(), currentUser.getId());
        if(scarp.isEmpty()) {
            return new DeleteActivityRecordScrapResDto("스크랩이 존재하지 않거나 이미 삭제되었습니다.", activityRecord.getId(), false);
        }
        ActivityRecordScarp activityRecordScarp = scarp.get();

        activityRecordScrapRepository.delete(activityRecordScarp);

        return new DeleteActivityRecordScrapResDto("스크랩 취소가 완료되었습니다.", recordId, false);
    }

    @Transactional
    public ReportActivityRecordResDto reportActivityRecord(Long recordId, ReportActivityRecordReqDto reqDto, CustomUserDetails user) throws CustomException {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto activityRecord = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        checkBlockedAndDeletedUser(currentUser.getId(), activityRecord.getWriterId(), activityRecord.isWriterDeleted());

        // 기록의 공개 범위 고려
        validateRecordAuthority(activityRecord.getVisibility(), activityRecord.getWriterId(), currentUser.getId());

        if(activityRecordReportRepository.existsByReportedRecordIdAndReporterId(activityRecord.getRecordId(), currentUser.getId())) {
            throw new CustomException(ErrorCode.ALREADY_RECORD_REPORTED);
        }

        ActivityRecordReport report = ActivityRecordReport.builder()
                .reporter(currentUser)
                .reportedUser(activityRecord.getWriter())
                .reportedRecord(activityRecord.getActivityRecord())
                .reason(reqDto.getReason())
                .build();
        activityRecordReportRepository.save(report);

        return new ReportActivityRecordResDto(recordId, activityRecord.getWriterId(), activityRecord.getWriterNickname(), "기록이 정상적으로 신고되었습니다.");
    }

    @Transactional(readOnly = true)
    public GetActivityRecordByStoryResDto getActivityRecordByStory(Long hobbyId, Long lastRecordId, Integer size, String keyword, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        if(Strings.hasText(keyword)) {
            // 최근 검색어 저장 로직
            recentRedisService.createRecentKeyword(currentUser.getId(), keyword);
        }

        Hobby targetHobby = (hobbyId != null)
                ?  hobbyRepository.findByIdAndUserIdAndStatus(hobbyId, currentUser.getId(), HobbyStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND)) // 진행 중인 취미 찾도록
                : getLatestInProgressHobby(currentUser);

        if(targetHobby == null) return null;

        Long hobbyInfoId = targetHobby.getHobbyInfoId();
        if(hobbyInfoId == null) return null;

        List<String> myFriendIds = friendRelationRepository.findAllFriendIdsByUserId(currentUser.getId()); // 현재 유저의 친구 목록 (공개 범위가 FRIEND 이면 조회되도록)
        List<String> blockFriendIds = friendRelationRepository.findAllBlockedIdsByUserId(currentUser.getId()); // 차단 유저 목록 (조회시 배제)

        List<GetActivityRecordByStoryResDto.RecordDto> recordDtos = activityRecordRepository.getActivityRecordByStory(hobbyInfoId, lastRecordId, size, keyword, currentUser.getId(), myFriendIds, blockFriendIds);


        boolean hasNext = false;
        if (recordDtos.size() > size) {
            hasNext = true;
            recordDtos.remove(size.intValue());
        }

        // 썸네일용 이미지 url 반환 (기록 이미지 url & 유저 이미지 url)
        recordDtos.forEach(dto -> {
            // 1. 기록 이미지 URL 변환 (Feed Thumbnail용)
            if (dto.getThumbnailUrl() != null) {
                dto.setThumbnailUrl(s3Util.toFeedThumbResizedUrl(dto.getThumbnailUrl()));
            }
            // 2. 유저 프로필 이미지 URL 변환 (Profile List용)
            if (dto.getUserInfo() != null && dto.getUserInfo().getProfileImageUrl() != null) {
                String originalProfileUrl = dto.getUserInfo().getProfileImageUrl();
                dto.getUserInfo().setProfileImageUrl(s3Util.toProfileListResizedUrl(originalProfileUrl));
            }
        });

        Long lastId = recordDtos.isEmpty() ? null : recordDtos.get(recordDtos.size() - 1).getRecordId();
        return new GetActivityRecordByStoryResDto(hobbyInfoId, targetHobby.getId(), targetHobby.getHobbyName(), lastId, recordDtos, hasNext);
    }

    private Hobby getLatestInProgressHobby(User user) {
        return hobbyRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        HobbyStatus.IN_PROGRESS
                )
                .orElse(null);
    }

    private void validateRecordAuthority(RecordVisibility visibility, String writerId, String currentUserId) {
        if (writerId.equals(currentUserId)) return;

        switch (visibility) {
            case FRIEND -> {
                if (!checkFriendship(writerId, currentUserId)) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            default -> {
            } // PUBLIC
        }
    }

    private boolean checkFriendship(String writerId, String currentUserId) {
        return friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(
                currentUserId, writerId, FriendRelationStatus.FOLLOW);
    }

    private void verifyRecordOwner(ActivityRecord record, User user) {
        if (!Objects.equals(record.getUser(), user)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }
    }

    private ActivityRecord getActivityRecord(Long id) {
        return activityRecordRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    private GetRecordDetailResDto.UserReactionDto createUserReactionDto(List<ReactionSummary> summaries, String userId) {
        List<RecordReactionType> myTypes = summaries.stream()
                .filter(s -> s.reactedUserId().equals(userId))
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.UserReactionDto(
                myTypes.contains(RecordReactionType.AWESOME),
                myTypes.contains(RecordReactionType.GREAT),
                myTypes.contains(RecordReactionType.AMAZING),
                myTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto.NewReactionDto createNewReactionDto(List<ReactionSummary> summaries, boolean isOwner) {
        if (!isOwner) return new GetRecordDetailResDto.NewReactionDto(false, false, false, false);
        List<RecordReactionType> unreadTypes = summaries.stream()
                .filter(s -> !s.readWriter())
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.NewReactionDto(
                unreadTypes.contains(RecordReactionType.AWESOME),
                unreadTypes.contains(RecordReactionType.GREAT),
                unreadTypes.contains(RecordReactionType.AMAZING),
                unreadTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto buildGetRecordDetailResDtoFromDto(RecordDetailQueryDto detail,
                                                                    boolean isOwner,
                                                                    GetRecordDetailResDto.NewReactionDto newR,
                                                                    GetRecordDetailResDto.UserReactionDto userR, boolean scraped) {
        return GetRecordDetailResDto.builder()
                .hobbyId(detail.hobbyId())
                .activityId(detail.activityId())
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .imageUrl(detail.imageUrl())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isOwner)
                .scraped(scraped)
                .userInfo(!isOwner ? GetRecordDetailResDto.UserInfoDto.builder()
                        .userId(detail.writerId())
                        .nickname(detail.writerNickname())
                        .profileImageUrl(detail.imageUrl())
                        .build() : null)
                .visibility(detail.visibility())
                .newReaction(newR)
                .userReaction(userR)
                .build();
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if(friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        // 타겟유저가 탈퇴한 회원인 경우
        if(deleted) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
    }
}