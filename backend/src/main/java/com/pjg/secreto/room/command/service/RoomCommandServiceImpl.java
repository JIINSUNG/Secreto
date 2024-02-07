package com.pjg.secreto.room.command.service;

import com.pjg.secreto.history.command.repository.MatchingCommandRepository;
import com.pjg.secreto.history.common.entity.Matching;
import com.pjg.secreto.mission.command.repository.MissionScheduleCommandRepository;
import com.pjg.secreto.mission.command.repository.RoomMissionCommandRepository;
import com.pjg.secreto.mission.common.entity.MissionSchedule;
import com.pjg.secreto.mission.common.entity.RoomMission;
import com.pjg.secreto.mission.common.exception.MissionException;
import com.pjg.secreto.room.command.dto.*;
import com.pjg.secreto.room.command.repository.RoomCommandRepository;
import com.pjg.secreto.room.command.repository.RoomUserCommandRepository;
import com.pjg.secreto.room.common.entity.Room;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.room.common.exception.RoomException;
import com.pjg.secreto.room.query.dto.SearchEntryCodesDto;
import com.pjg.secreto.room.query.repository.RoomQueryRepository;
import com.pjg.secreto.room.query.repository.RoomUserQueryRepository;
import com.pjg.secreto.user.common.entity.User;
import com.pjg.secreto.user.common.exception.UserException;
import com.pjg.secreto.user.query.repository.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class RoomCommandServiceImpl implements RoomCommandService {

    private final RoomQueryRepository roomQueryRepository;
    private final RoomCommandRepository roomCommandRepository;
    private final RoomUserCommandRepository roomUserCommandRepository;
    private final MissionScheduleCommandRepository missionScheduleCommandRepository;
    private final UserQueryRepository userQueryRepository;
    private final RoomMissionCommandRepository roomMissionCommandRepository;
    private final RoomUserQueryRepository roomUserQueryRepository;
    private final MatchingCommandRepository matchingCommandRepository;


    // 방 생성 api (user 개발 완료 시 개발 예정)
    @Override
    public CreateRoomResponseDto createRoom(CreateRoomRequestDto createRoomRequestDto) {

        log.info("방 생성 api");

        try {

            Long userNo = createRoomRequestDto.getUserNo();

            List<Room> rooms = roomQueryRepository.findAll();

            List<SearchEntryCodesDto> codeDto = rooms.stream().map(r -> new SearchEntryCodesDto(r.getEntryCode())).toList();

            List<String> existCodes = new ArrayList<>();

            for (SearchEntryCodesDto ec : codeDto) {
                existCodes.add(ec.getEntryCodes());
            }

            // 모든 방의 입장 코드 조회 후 같은 코드가 있으면 다시 돌림
            boolean isExist = false;
            String newToken;
            do {
                // 입장 코드 생성
                newToken = generateRandomCode();
                for (String code : existCodes) {
                    if (newToken.equals(code)) {
                        log.info("중복된 코드");
                        isExist = true;
                        break;
                    }
                }

            } while (isExist);
            log.info("생성된 입장 코드 : " + newToken);


            // 방 생성
            Room room = Room.builder().roomName(createRoomRequestDto.getRoomName())
                    .entryCode(newToken)
                    .roomEndAt(LocalDateTime.now().plusWeeks(1)).build();
            roomCommandRepository.save(room);

            log.info("방 식별키 : " + room.getId());

            // 방 별 유저에 방장 추가
            User user = userQueryRepository.findById(userNo).orElseThrow(() -> new UserException("해당 유저가 없습니다."));

            log.info("룸 유저 생성");
            RoomUser roomUser = RoomUser.builder().user(user).room(room).userEntryAt(LocalDateTime.now())
                    .userLeaveAt(null).standByYn(false).nickname(createRoomRequestDto.getHostNickname())
                    .bookmarkYn(false).build();
            roomUserCommandRepository.save(roomUser);

            log.info("룸 유저 식별키 : " + roomUser.getId());
            // room에 방장 id 추가
            room.changeHost(roomUser.getId());

            // 방 입장 코드 반환
            CreateRoomResponseDto result = CreateRoomResponseDto.builder().entryCode(newToken).build();

            return result;
        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void changeRoomName(ChangeRoomNameRequestDto changeRoomNameRequestDto) {

        log.info("방 이름 변경 api");

        try {
            Room room = roomQueryRepository.findById(changeRoomNameRequestDto.getRoomNo()).orElseThrow(() -> new UserException("해당 유저가 없습니다."));

            room.changeName(changeRoomNameRequestDto.getRoomName());

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }

    }

    @Override
    public SetRoomResponseDto setRoom(SetRoomRequestDto setRoomRequestDto) {

        log.info("방 세팅 api");

        try {

            Room room = roomQueryRepository.findById(setRoomRequestDto.getRoomNo()).orElseThrow(() -> new RoomException("해당 방이 없습니다."));

            RoomUser findRoomUserNo = roomUserQueryRepository.findByUserNoAndRoomNo(setRoomRequestDto.getUserNo(), room.getId())
                    .orElseThrow(() -> new RoomException("해당 유저는 방에 속해있지 않습니다."));

            if(!Objects.equals(room.getHostNo(), findRoomUserNo.getId())) {
                throw new RoomException("해당 유저는 방장이 아닙니다.");
            }

            // 현재 날짜
            LocalDate today = LocalDateTime.now().toLocalDate();
            LocalTime t = LocalDateTime.now().toLocalTime();

            log.info("현재 날짜 : " + today);

            // 미션 일정 생성 (미션 시작일과 방 끝나는 날짜를 기준으로 주기마다 날짜 생성해야 함)
//            LocalDateTime startDT = LocalDateTime.of(2024, 1, 10, 18, 40, 25);
//            LocalDateTime endDT = LocalDateTime.of(2024, 2, 1, 14, 30, 55);
//            int period = 3;
//            LocalDate missionStartDate = startDT.toLocalDate();
//            LocalDate roomEndDate = endDT.toLocalDate();

            // 방 끝나는 일정
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime roomEndDateTime = LocalDateTime.parse(setRoomRequestDto.getRoomEndAt(), formatter);
            log.info("방 끝나는 일정 : " + roomEndDateTime);

            // 미션이 처음으로 주어지는 날짜
            LocalDate missionStartDate = LocalDate.parse(setRoomRequestDto.getMissionStartAt(), DateTimeFormatter.ISO_DATE);
            log.info("미션이 처음으로 주어지는 날짜 : " + missionStartDate);

            // 미션이 주어지는 시간
            LocalTime missionSubmitTime = LocalTime.parse(setRoomRequestDto.getMissionSubmitTime());
            log.info("미션이 주어지는 시간 : " + missionSubmitTime);

            int period = setRoomRequestDto.getPeriod();
//            LocalDate missionStartDate = setRoomRequestDto.getMissionStartAt();
//            LocalDate roomEndDate = setRoomRequestDto.getRoomEndAt().toLocalDate();

            Period diff = Period.between(missionStartDate, roomEndDateTime.toLocalDate());
            int totalDays = diff.getDays();
            log.info("시작일과 종료일의 날짜 차이 : " + totalDays);

            for(int i=0; i<totalDays; i+=period) {

                LocalDateTime missionStartDateTime = LocalDateTime.of(missionStartDate, missionSubmitTime);
                LocalDateTime date = missionStartDateTime.plusDays(i);
                MissionSchedule missionSchedule = MissionSchedule.builder().room(room).missionSubmitAt(date).build();
                missionScheduleCommandRepository.save(missionSchedule);
            }

            log.info("미션 스케쥴 저장 완료");

            // 방 미션에 미션 추가
            List<MissionDto> missionList = setRoomRequestDto.getMissionList();

            for(MissionDto mission : missionList) {
                RoomMission roomMission = RoomMission.builder().room(room).content(mission.getContent()).build();
                roomMissionCommandRepository.save(roomMission);
            }

            /**
             * 매칭 정보 추가를 위한 로직
              */
            List<RoomUser> roomUsers = roomUserQueryRepository.findAllByRoomId(setRoomRequestDto.getRoomNo());

            // key 랜덤으로 섞기
            int keys[] = new int[roomUsers.size()];
            Random r = new Random();
            for(int i=0; i<roomUsers.size(); i++) {
                keys[i] = r.nextInt(roomUsers.size());

                for(int j=0; j<i; j++) {
                    if(keys[i] == keys[j]) {
                        i--;
                    }
                }
            }

            log.info("keys = " + Arrays.toString(keys));

            // 매칭 정보 저장

            for(int i=0; i<keys.length; i++) {

                RoomUser findRoomUser = roomUserQueryRepository.findById(roomUsers.get(keys[i]).getId())
                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));

                Matching matching = Matching.builder()
                        .roomUser(findRoomUser).matchingAt(LocalDateTime.now()).deprecatedAt(null)
                        .manitoNo(null).manitiNo(null).build();

                if(i == 0) {
                    matching.changeMatchingInfo(roomUsers.get(keys[keys.length-1]).getId(), roomUsers.get(keys[i+1]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[keys.length-1]).getId(), roomUsers.get(keys[i+1]).getId());
                }
                else if(i == keys.length-1) {
                    matching.changeMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[0]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[0]).getId());
                }
                else {
                    matching.changeMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[i+1]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[i+1]).getId());
                }

                matchingCommandRepository.save(matching);

            }

//            for(int i=0; i<keys.length; i++) {
//
//                RoomUser findRoomUser = roomUserQueryRepository.findById(keys[i])
//                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));
//
//                Matching matching = Matching.builder()
//                        .roomUser(findRoomUser).matchingAt(LocalDateTime.now()).deprecatedAt(null)
//                        .manitoNo(null).manitiNo(null).build();
//                if(i == 0) {
//                    matching.changeMatchingInfo(keys[keys.length-1], keys[i+1]);
//                    findRoomUser.setMatchingInfo(keys[keys.length-1], keys[i+1]);
//                }
//                else if(i == keys.length-1) {
//                    matching.changeMatchingInfo(keys[i-1], keys[0]);
//                    findRoomUser.setMatchingInfo(keys[i-1], keys[0]);
//                }
//                else {
//                    matching.changeMatchingInfo(keys[i-1], keys[i+1]);
//                    findRoomUser.setMatchingInfo(keys[i-1], keys[i+1]);
//                }
//
//                matchingCommandRepository.save(matching);
//
//            }

            // 방 정보 수정
            room.startRoom(LocalDateTime.now(), roomEndDateTime,
                    setRoomRequestDto.getHostParticipantYn(), setRoomRequestDto.getCommonYn(),
                    missionSubmitTime, missionStartDate, true);

            SetRoomResponseDto result = SetRoomResponseDto.builder().roomNo(setRoomRequestDto.getRoomNo()).build();
            return result;

        } catch (Exception e) {
            throw new RoomException("방 생성 중 오류 발생");
        }

    }

    @Override
    public Long enterRoom(EnterRoomRequestDto enterRoomRequestDto) {

        log.info("방 입장 api");

        try {

            Long userNo = enterRoomRequestDto.getUserNo();

            // 사용할 닉네임 입력
            Room findRoom = roomQueryRepository.findByEntryCode(enterRoomRequestDto.getEntryCode());
            User findUser = userQueryRepository.findById(userNo).orElseThrow(() -> new UserException("유저가 존재하지 않습니다."));

            List<RoomUser> roomUsers = roomUserQueryRepository.findAllByUserNoAndRoomNo(findUser.getId(), findRoom.getId());

            if(!roomUsers.isEmpty()) {
                throw new RoomException("해당 유저는 이미 방에 속해있습니다.");
            }
            else {

                RoomUser roomUser = RoomUser.builder()
                        .nickname(enterRoomRequestDto.getNickname())
                        .room(findRoom)
                        .user(findUser)
                        .userEntryAt(null)
                        .userLeaveAt(null)
                        .standByYn(true)
                        .bookmarkYn(false)
                        .build();

                roomUserCommandRepository.save(roomUser);
            }

            return findRoom.getId();

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void exitRoom(ExitRoomRequestDto exitRoomRequestDto) {

        log.info("방 나가기 api");

        try {

            // 방 생성 유저 id 꺼내기 (security 세팅 완료 시 수정)
            Long userNo = exitRoomRequestDto.getUserNo();

            // 방 유저 조회
            Room findRoom = roomQueryRepository.findById(exitRoomRequestDto.getRoomNo())
                    .orElseThrow(() -> new RoomException("해당 방이 존재하지 않습니다."));
            User findUser = userQueryRepository.findById(userNo)
                    .orElseThrow(() -> new UserException("해당 유저가 존재하지 않습니다."));
            RoomUser roomUser = roomUserQueryRepository.findRoomUserByRoomAndUser(findRoom, findUser);
            log.info("방 유저 식별키 : " + roomUser);

            // 방 유저 정보 변경
            roomUser.leave();

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public List<Long> acceptUser(AcceptUserRequestDto acceptUserRequestDto) {

        log.info("유저 수락 api");
        try {

            List<RoomUser> findRoomUsers = roomUserQueryRepository.findByRoomUserNos(acceptUserRequestDto.getRoomUserNos());

            List<Long> roomUserNos = new ArrayList<>();

            // 방 유저 정보 변경
            for(RoomUser ru : findRoomUsers) {
                ru.accepted();
                roomUserNos.add(ru.getId());
            }


            return roomUserNos;

        } catch (Exception e) {

            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void denyUser(DenyUserRequestDto denyUserRequestDto) {

        log.info("유저 거절 api");
        try {
            roomUserCommandRepository.deleteAllByIds(denyUserRequestDto.getRoomUserNos());

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void deligateAdmin(DeligateAdminRequestDto deligateAdminRequestDto) {

        log.info("방장 위임 api");
        try {

            Room findRoom = roomQueryRepository.findById(deligateAdminRequestDto.getRoomNo())
                    .orElseThrow(() -> new RoomException("해당 방이 존재하지 않습니다."));

            findRoom.changeHost(deligateAdminRequestDto.getNewHost());

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }

    }

    @Override
    public Boolean bookmarkRoom(BookmarkRoomRequestDto bookmarkRoomRequestDto) {

        log.info("즐겨찾기 api");

        try {

            Long userNo = bookmarkRoomRequestDto.getUserNo();

            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, bookmarkRoomRequestDto.getRoomNo()).orElseThrow(() -> new MissionException("해당 방 유저가 없습니다."));

            return findRoomUser.bookmark();

        } catch (Exception e) {
            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void terminateRoom(TerminateRoomRequestDto terminateRoomRequestDto) {

        log.info("방 종료 api");

        try {

            Room findRoom = roomQueryRepository.findById(terminateRoomRequestDto.getRoomNo())
                    .orElseThrow(() -> new RoomException("방이 존재하지 않습니다."));

            findRoom.terminateRoom();

        } catch (Exception e) {

            throw new RoomException(e.getMessage());
        }
    }

    @Override
    public void initMatching(InitMatchingRequestDto initMatchingRequestDto) {

        log.info("모두 랜덤 매칭 api");

        try {

            /**
             * 매칭 정보 추가를 위한 로직
             */
            List<RoomUser> roomUsers = roomUserQueryRepository.findAllByRoomId(initMatchingRequestDto.getRoomNo());

            // key 랜덤으로 섞기
            int keys[] = new int[roomUsers.size()];
            Random r = new Random();
            for(int i=0; i<roomUsers.size(); i++) {
                keys[i] = r.nextInt(roomUsers.size()) + 1;

                for(int j=0; j<i; j++) {
                    if(keys[i] == keys[j]) {
                        i--;
                    }
                }
            }

            log.info("keys = " + Arrays.toString(keys));

            // 매칭 정보 저장
            for(int i=0; i<keys.length; i++) {

                RoomUser findRoomUser = roomUserQueryRepository.findById(roomUsers.get(keys[i]).getId())
                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));

                Matching matching = Matching.builder()
                        .roomUser(findRoomUser).matchingAt(LocalDateTime.now()).deprecatedAt(null)
                        .manitoNo(null).manitiNo(null).build();

                if(i == 0) {
                    matching.changeMatchingInfo(roomUsers.get(keys[keys.length-1]).getId(), roomUsers.get(keys[i+1]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[keys.length-1]).getId(), roomUsers.get(keys[i+1]).getId());
                }
                else if(i == keys.length-1) {
                    matching.changeMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[0]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[0]).getId());
                }
                else {
                    matching.changeMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[i+1]).getId());
                    findRoomUser.setMatchingInfo(roomUsers.get(keys[i-1]).getId(), roomUsers.get(keys[i+1]).getId());
                }

                matchingCommandRepository.save(matching);

            }

        } catch (Exception e) {

            throw new RoomException(e.getMessage());
        }

    }

    @Override
    public void insertMatching(InsertMatchingRequestDto insertMatchingRequestDto) {

        log.info("사이 매칭 api");

        try {

            // 방 입장이 수락된 유저 리스트 조회
            List<RoomUser> acceptedUsers = roomUserQueryRepository
                    .findAllByRoomUserNosAndRoomNo(insertMatchingRequestDto.getRoomUserNos(), insertMatchingRequestDto.getRoomNo());

            // 방 유저 전체 리스트 조회
            List<RoomUser> roomUsers = roomUserQueryRepository.findAllByRoomId(insertMatchingRequestDto.getRoomNo());

            // 순서대로(원형으로)

            Random random = new Random();
            for (RoomUser acceptedUser: acceptedUsers) {
                int randomIdx = random.nextInt(roomUsers.size());
                roomUsers.add(randomIdx, acceptedUser);
            }

            for (int i = 0, end = roomUsers.size(); i < end; ++i) {
                RoomUser roomUser = roomUsers.get(i);
                RoomUser manito = roomUsers.get((i - 1 + end) % end);
                RoomUser maniti = roomUsers.get((i + 1) % end);
                roomUser.setManito(manito.getId());
                roomUser.setManiti(maniti.getId());
            }

            
//
//            for(int matchingCnt=0; matchingCnt < acceptedUsers.size(); matchingCnt++) {
//
//                log.info("모든 방 유저 : ");
//                for(RoomUser ru : roomUsers) {
//                    log.info(ru.getId() + " ");
//                }
//
//                // 새로 들어온 유저를 제외한 기존 유저 리스트를 구하는 로직
//                List<Long> existsUserNos = new ArrayList<>();
//                for(int i=0; i< roomUsers.size(); i++) {
//
//                    boolean isDuplicate = false;
//                    for(int j=0; j<acceptedUsers.size(); j++) {
//                        if(roomUsers.get(i).getId() == acceptedUsers.get(j).getId()) {
//                            isDuplicate = true;
//                        }
//                    }
//
//                    if(!isDuplicate) {
//                        existsUserNos.add(roomUsers.get(i).getId());
//                    }
//                }
//
//                log.info("기존 방 유저 : ");
//                for(Long nos : existsUserNos) {
//                    log.info(nos + " ");
//                }
//
//                // 랜덤 유저 픽을 위한 로직(기존 유저들을 랜덤으로 돌린 뒤 인덱스 0번재 유저의 마니또 관계를 파괴할 예정
//                int indexs[] = new int[existsUserNos.size()];
//                Random r = new Random();
//                for(int i=0; i<existsUserNos.size(); i++) {
//                    indexs[i] = r.nextInt(existsUserNos.size()) + 1;
//
//                    for(int j=0; j<i; j++) {
//                        if(indexs[i] == indexs[j]) {
//                            i--;
//                        }
//                    }
//                }
//
//                log.info("indexs : ");
//                for(int i=0; i<indexs.length; i++) {
//                    log.info(indexs[i] + " ");
//                }
//
//                // 랜덤으로 고른 한명의 유저
//                RoomUser findRoomUser = roomUserQueryRepository.findById(existsUserNos.get(indexs[0]))
//                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));
//
//                // 랜덤 유저의 마니또
//                RoomUser findUsersManito = roomUserQueryRepository.findById(findRoomUser.getUsersManito())
//                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));
//
//                // 새로 매칭될 유저
//                RoomUser newUser = roomUserQueryRepository.findById(acceptedUsers.get(matchingCnt).getId())
//                        .orElseThrow(() -> new RoomException("해당 유저가 존재하지 않습니다."));
//
//                findUsersManito.setManiti(newUser.getId());
//                findRoomUser.setManito(newUser.getId());
//
//                newUser.setManito(findUsersManito.getId());
//                newUser.setManiti(findRoomUser.getId());
//
//                // 새로운 매칭 기록 저장
//                Matching matching1 = Matching.builder()
//                        .roomUser(findRoomUser)
//                        .matchingAt(LocalDateTime.now())
//                        .manitoNo(findRoomUser.getUsersManito())
//                        .manitiNo(findRoomUser.getUsersManiti())
//                        .build();
//
//                Matching matching2 = Matching.builder()
//                        .roomUser(findUsersManito)
//                        .matchingAt(LocalDateTime.now())
//                        .manitoNo(findUsersManito.getUsersManito())
//                        .manitiNo(findUsersManito.getUsersManiti())
//                        .build();
//
//                Matching matching3 = Matching.builder()
//                        .roomUser(newUser)
//                        .matchingAt(LocalDateTime.now())
//                        .manitoNo(newUser.getUsersManito())
//                        .manitiNo(newUser.getUsersManiti())
//                        .build();
//
//                matchingCommandRepository.save(matching1);
//                matchingCommandRepository.save(matching2);
//                matchingCommandRepository.save(matching3);
//
//            }

        } catch (Exception e) {

            throw new RoomException(e.getMessage());
        }
    }

    /**
     * 방 입장 코드 생성 메서드
     */
    public String generateRandomCode() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 6;
        Random random = new Random();
        String generatedCode = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedCode;
    }

}

