package com.pjg.secreto.mission.command.service;

import com.pjg.secreto.history.command.repository.ManitoExpectLogCommandRepository;
import com.pjg.secreto.history.command.repository.UserMemoCommandRepository;
import com.pjg.secreto.history.common.entity.ManitoExpectLog;
import com.pjg.secreto.history.common.entity.UserMemo;
import com.pjg.secreto.history.query.repository.UserMemoQueryRepository;
import com.pjg.secreto.mission.command.dto.*;
import com.pjg.secreto.mission.command.repository.SuddenMissionCommandRepository;
import com.pjg.secreto.mission.common.entity.SuddenMission;
import com.pjg.secreto.mission.common.exception.MissionException;
import com.pjg.secreto.mission.query.repository.SuddenMissionQueryRepository;
import com.pjg.secreto.room.common.entity.Room;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.room.query.repository.RoomQueryRepository;
import com.pjg.secreto.room.query.repository.RoomUserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public void addSuddenMission(AddSuddenMissionRequestDto addSuddenMissionRequestDto) {

        try {

            Room findRoom = roomQueryRepository.findById(addSuddenMissionRequestDto.getRoomNo())
                    .orElseThrow(() -> new MissionException("해당 방이 없습니다."));

            SuddenMission suddenMission = SuddenMission.builder().room(findRoom)
                    .missionSubmitAt(addSuddenMissionRequestDto.getMissionSubmitAt())
                    .content(addSuddenMissionRequestDto.getContent()).build();

            suddenMissionCommandRepository.save(suddenMission);

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
    public void predictManito(PredictManitoRequestDto predictManitoRequestDto) {

        try {

            Long userNo = predictManitoRequestDto.getUserNo();

            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, predictManitoRequestDto.getRoomNo()).orElseThrow(() -> new MissionException("해당 방 유저가 없습니다."));

            ManitoExpectLog manitoExpectLog = ManitoExpectLog.builder()
                    .roomUser(findRoomUser).expectedUser(predictManitoRequestDto.getExpectedManito())
                    .expectedAt(LocalDateTime.now()).build();

            manitoExpectLogCommandRepository.save(manitoExpectLog);

        } catch (Exception e) {

            throw new MissionException(e.getMessage());
        }
    }

    @Override
    public MemoUserResponseDto memoUser(MemoUserRequestDto memoUserRequestDto) {

        try {

            Long userNo = memoUserRequestDto.getUserNo();

            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, memoUserRequestDto.getRoomNo()).orElseThrow(() -> new MissionException("해당 방 유저가 없습니다."));

            UserMemo userMemo = UserMemo.builder()
                    .roomUser(roomUser).memo(memoUserRequestDto.getMemo())
                    .manitoPredictType(memoUserRequestDto.getManitoPredictType())
                    .memoTo(memoUserRequestDto.getMemoTo()).build();

            userMemoCommandRepository.save(userMemo);

            MemoUserResponseDto result = MemoUserResponseDto.builder().userMemoNo(userMemo.getId()).build();

            return result;

        } catch (Exception e) {

            throw new MissionException(e.getMessage());
        }
    }

    @Override
    public UpdateMemoResponseDto updateMemo(UpdateMemoRequestDto updateMemoRequestDto) {

        try {

            Long userNo = updateMemoRequestDto.getUserNo();

            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, updateMemoRequestDto.getRoomNo()).orElseThrow(() -> new MissionException("해당 방 유저가 없습니다."));

            UserMemo findUserMemo = userMemoQueryRepository.findByRoomUserNo(findRoomUser.getId());

            UpdateMemoResponseDto result = UpdateMemoResponseDto.builder()
                    .userMemoNo(findUserMemo.getId()).build();

            return result;

        } catch (Exception e) {

            throw new MissionException(e.getMessage());
        }
    }
}