package com.pjg.secreto.mission.command.service;

import com.pjg.secreto.alarm.common.entity.Alarm;
import com.pjg.secreto.alarm.dto.AlarmDataDto;
import com.pjg.secreto.alarm.repository.AlarmRepository;
import com.pjg.secreto.alarm.service.AlarmService;
import com.pjg.secreto.alarm.service.EmitterService;
import com.pjg.secreto.history.command.repository.ManitoExpectLogCommandRepository;
import com.pjg.secreto.history.command.repository.UserMemoCommandRepository;
import com.pjg.secreto.history.common.entity.ManitoExpectLog;
import com.pjg.secreto.history.common.entity.ManitoPredictType;
import com.pjg.secreto.history.common.entity.UserMemo;
import com.pjg.secreto.history.query.repository.UserMemoQueryRepository;
import com.pjg.secreto.mission.command.dto.*;
import com.pjg.secreto.mission.command.repository.RoomMissionCommandRepository;
import com.pjg.secreto.mission.command.repository.SuddenMissionCommandRepository;
import com.pjg.secreto.mission.command.repository.UserMissionCommandRepository;
import com.pjg.secreto.mission.common.entity.*;
import com.pjg.secreto.mission.common.exception.MissionException;
import com.pjg.secreto.mission.query.repository.RoomMissionQueryRepository;
import com.pjg.secreto.mission.query.repository.SuddenMissionQueryRepository;
import com.pjg.secreto.mission.query.repository.UserMissionQueryRepository;
import com.pjg.secreto.room.common.entity.Room;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.room.common.exception.RoomException;
import com.pjg.secreto.room.query.repository.RoomQueryRepository;
import com.pjg.secreto.room.query.repository.RoomUserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MissionCommandServiceImpl implements MissionCommandService {

    private final RoomQueryRepository roomQueryRepository;
    private final SuddenMissionCommandRepository suddenMissionCommandRepository;
    private final SuddenMissionQueryRepository suddenMissionQueryRepository;
    private final RoomUserQueryRepository roomUserQueryRepository;
    private final ManitoExpectLogCommandRepository manitoExpectLogCommandRepository;
    private final UserMemoCommandRepository userMemoCommandRepository;
    private final UserMemoQueryRepository userMemoQueryRepository;
    private final RoomMissionQueryRepository roomMissionQueryRepository;
    private final UserMissionCommandRepository userMissionCommandRepository;
    private final EmitterService emitterService;
    private final UserMissionQueryRepository userMissionQueryRepository;

    @Override
    public void addSuddenMission(AddSuddenMissionRequestDto addSuddenMissionRequestDto) {

        try {

            Room findRoom = roomQueryRepository.findById(addSuddenMissionRequestDto.getRoomNo())
                    .orElseThrow(() -> new MissionException("해당 방이 없습니다."));

            // 돌발 미션 저장 로직
            SuddenMission suddenMission = SuddenMission.builder().room(findRoom)
                    .missionSubmitAt(LocalDateTime.now())
                    .content(addSuddenMissionRequestDto.getContent()).build();

            suddenMissionCommandRepository.save(suddenMission);

            // 유저 미션 생성 로직
            for(RoomUser ru : findRoom.getRoomUsers()) {

                UserMission userMission = UserMission.builder()
                        .missionReceivedAt(LocalDateTime.now())
                        .missionCertifyYn(false)
                        .missionType(MissionType.SUDDEN)
                        .missionRerollCount(3)
                        .roomUser(ru)
                        .content(suddenMission.getContent())
                        .roomMissionNo(suddenMission.getId()).build();

                userMissionCommandRepository.save(userMission);

                // 유저에게 알림 발송
                AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                        .content("돌발 미션 : " + userMission.getContent())
                        .readYn(false)
                        .generatedAt(LocalDateTime.now())
                        .author("돌발 미션 도착")
                        .roomUserNo(ru.getId()).build();

                emitterService.alarm(ru.getId(), alarmDataDto, "돌발 미션이 생성되었습니다.", "message");

            }

        } catch (Exception e) {

            throw new MissionException(e.getMessage());
        }
    }

    @Override
    public void deleteSuddenMission(DeleteSuddenMissionRequestDto deleteSuddenMissionRequestDto) {

        try {

            SuddenMission findSuddenMission = suddenMissionQueryRepository
                    .findByIdAndRoomNo(deleteSuddenMissionRequestDto.getSuddenMissionNo(),
                            deleteSuddenMissionRequestDto.getRoomNo());

            suddenMissionCommandRepository.delete(findSuddenMission);

        } catch (Exception e) {
            throw new MissionException(e.getMessage());
        }
    }

    @Override
    public MemoUserResponseDto memoUser(MemoUserRequestDto memoUserRequestDto) {

        try {

            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(memoUserRequestDto.getUserNo(),
                    memoUserRequestDto.getRoomNo()).orElseThrow(() -> new MissionException("해당 방 유저가 없습니다."));

            UserMemo findUserMemo = userMemoQueryRepository.findByRoomUserNoAndMemoTo(findRoomUser.getId(), memoUserRequestDto.getMemoTo());

            List<UserMemo> findUserMemos = userMemoQueryRepository.findByRoomUserNo(findRoomUser.getId());

            // 예측 타입이 YES일 경우, 기존의 YES 유저는 UNKNOWN으로 바꾸고, YES로 예측한 유저의 마니또 예측 로그를 생성
            if(memoUserRequestDto.getManitoPredictType().equals(ManitoPredictType.YES)) {

                for(UserMemo um : findUserMemos) {
                    if(um.getManitoPredictType().equals(ManitoPredictType.YES)) {
                        um.updateToUnknown();
                        break;
                    }
                }

                ManitoExpectLog manitoExpectLog = ManitoExpectLog.builder()
                        .roomUser(findRoomUser)
                        .expectedUser(memoUserRequestDto.getMemoTo())
                        .expectedReason(memoUserRequestDto.getMemo())
                        .expectedAt(LocalDateTime.now()).build();

                manitoExpectLogCommandRepository.save(manitoExpectLog);
            }

            MemoUserResponseDto result;

            if(findUserMemo == null) {

                log.info("메모가 없으므로 새로 생성");
                UserMemo userMemo = UserMemo.builder()
                        .roomUser(findRoomUser)
                        .memo(memoUserRequestDto.getMemo())
                        .manitoPredictType(memoUserRequestDto.getManitoPredictType())
                        .memoTo(memoUserRequestDto.getMemoTo()).build();

                userMemoCommandRepository.save(userMemo);

                result = MemoUserResponseDto.builder().userMemoNo(userMemo.getId()).build();
            }

            else {

                log.info("기존의 메모 수정");
                findUserMemo.updateMemo(memoUserRequestDto.getMemo(), memoUserRequestDto.getManitoPredictType());

                result = MemoUserResponseDto.builder().userMemoNo(findUserMemo.getId()).build();
            }

            return result;

        } catch (Exception e) {

            throw new MissionException(e.getMessage());
        }
    }

    @Override
    public void rerollMission(RerollMissionRequestDto rerollMissionRequestDto) {

        try {

            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(rerollMissionRequestDto.getUserNo(),
                    rerollMissionRequestDto.getRoomNo()).orElseThrow(() -> new RoomException("해당 유저는 방에 속해있지 않습니다."));

            UserMission findUserMission = userMissionQueryRepository.findById(rerollMissionRequestDto.getUserMissionNo())
                    .orElseThrow(() -> new MissionException("해당 미션은 존재하지 않습니다."));

            UserMission latestMission = userMissionQueryRepository.findWhereLatestByRoomUserNo(findRoomUser.getId());

            if(!Objects.equals(findUserMission.getId(), latestMission.getId())) {
                throw new MissionException("가장 최근에 받은 미션만 리롤할 수 있습니다.");
            }

            List<RoomMission> roomMissions = roomMissionQueryRepository.findAllByRoomNo(rerollMissionRequestDto.getRoomNo());
            log.info("미션 리스트 찾기");

            Collections.shuffle(roomMissions);
            log.info("셔플 완료");

            RoomMission newRoomMission = roomMissions.get(0);
            log.info("미션 가져오기");

            // 유저 미션 수정
            findUserMission.rerollUserMission(newRoomMission.getContent(), findUserMission.getMissionRerollCount());

            log.info("유저 미션 수정 완료");

        } catch (Exception e) {
            throw new MissionException(e.getMessage());
        }
    }

    // 정기 미션 날리기 기능, 방 끝나는 시각에 맞게 끝내는 기능 수행 메서드
    @Scheduled(cron = "0 0/2 0-23 * * *") // 매일 정각마다 실행
    public void throwRegularMission() {

        log.info("스케쥴러 발동");

        /**
         * 정기 미션 날리기 로직
         */
        LocalDateTime now = LocalDateTime.now();
        now = now.truncatedTo(ChronoUnit.HOURS);

        log.info("현재 시각 : " + now);

        // 미션 제출 일정이 현재 일자와 같은 모든 룸 유저들 조회
        List<Room> findRooms = roomQueryRepository.findAll();

        List<Room> hasMissionRooms = new ArrayList<>();

        for(Room r : findRooms) {

            boolean hasMissionToday = false;
            for(MissionSchedule ms : r.getMissionSchedules()) {

                LocalDateTime missionSubmitTime = ms.getMissionSubmitAt().truncatedTo(ChronoUnit.HOURS);
                log.info("미션 던져지는 날짜 : " + ms.getMissionSubmitAt());

                if(now.isEqual(missionSubmitTime)) {
                    hasMissionToday = true;
                    break;
                }
            }

            // 일자가 같을 경우 방 저장
            if(hasMissionToday) {
                hasMissionRooms.add(r);
            }
        }

        log.info("룸 미션 가지고 있는 룸 조회 완료");
        for(Room r : hasMissionRooms) {

            List<RoomMission> roomMissions = roomMissionQueryRepository.findAllByRoomNo(r.getId());
            log.info("방의 미션들 조회 완료");

            List<RoomUser> findRoomUsers = roomUserQueryRepository.findAllByRoomNo(r.getId());
            log.info("방의 유저들 조회 완료");

            Collections.shuffle(roomMissions);

            // 방의 유저들에게 미션 던지기 로직
            for(RoomUser ru : findRoomUsers) {

                // 개별 미션이면 미션 랜덤으로
                if(!r.getCommonYn()) {
                    Collections.shuffle(roomMissions);
                }

                // 유저 미션 생성
                RoomMission roomMission = roomMissions.get(0);
                log.info("생성된 유저 미션 : " + roomMissions.get(0).getContent());


                UserMission userMission = UserMission.builder()
                        .missionReceivedAt(LocalDateTime.now())
                        .missionCertifyYn(false)
                        .missionType(MissionType.REGULAR)
                        .missionRerollCount(3)
                        .roomUser(ru)
                        .content(roomMission.getContent())
                        .roomMissionNo(roomMission.getId()).build();

                userMissionCommandRepository.save(userMission);
                log.info("유저 별 미션 생성 완료");

                // 유저에게 알림 발송
                AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                        .content("정기 미션 : " + userMission.getContent())
                        .readYn(false)
                        .generatedAt(LocalDateTime.now())
                        .author("정기 미션 도착")
                        .roomUserNo(ru.getId()).build();

                emitterService.alarm(ru.getId(), alarmDataDto, "정기 미션이 생성되었습니다.", "message");

            }

        }

        // 방 끝내는 로직
        List<Room> rooms = roomQueryRepository.findAll();

        for(Room r : rooms) {
            LocalDateTime roomEndAt = r.getRoomEndAt().truncatedTo(ChronoUnit.HOURS);

            if(now.isEqual(roomEndAt)) {

                r.terminateRoom();

                List<RoomUser> findRoomUsers = roomUserQueryRepository.findAllByRoomNo(r.getId());

                for(RoomUser ru : findRoomUsers) {

                    // 유저에게 알림 발송
                    AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                            .content("마니또 게임이 종료되었습니다.")
                            .readYn(false)
                            .generatedAt(LocalDateTime.now())
                            .author("알림 도착")
                            .roomUserNo(ru.getId()).build();

                    emitterService.alarm(ru.getId(), alarmDataDto, "마니또 게임이 종료되었습니다.", "message");
                }
            }
        }

    }
}


