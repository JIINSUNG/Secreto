package com.pjg.secreto.board.command.service;

import com.pjg.secreto.alarm.dto.AlarmDataDto;
import com.pjg.secreto.alarm.dto.BoardAlarmDataDto;
import com.pjg.secreto.alarm.service.EmitterService;
import com.pjg.secreto.board.command.dto.UpdateBoardRequestDto;
import com.pjg.secreto.board.command.dto.UpdateReplyRequestDto;
import com.pjg.secreto.board.command.dto.WriteBoardRequestDto;
import com.pjg.secreto.board.command.dto.WriteReplyRequestDto;
import com.pjg.secreto.board.command.repository.BoardCommandRepository;
import com.pjg.secreto.board.command.repository.LikedCommandRepository;
import com.pjg.secreto.board.command.repository.ReplyCommandRepository;
import com.pjg.secreto.board.common.entity.Board;
import com.pjg.secreto.board.common.entity.BoardCategory;
import com.pjg.secreto.board.common.entity.Liked;
import com.pjg.secreto.board.common.entity.Reply;
import com.pjg.secreto.board.common.exception.BoardException;
import com.pjg.secreto.board.query.repository.BoardQueryRepository;
import com.pjg.secreto.board.query.repository.LikedQueryRepository;
import com.pjg.secreto.board.query.repository.ReplyQueryRepository;
import com.pjg.secreto.mission.common.entity.UserMission;
import com.pjg.secreto.mission.query.repository.UserMissionQueryRepository;
import com.pjg.secreto.room.common.entity.Room;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.room.query.repository.RoomUserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@RequiredArgsConstructor
@Transactional
@Service
public class BoardCommandServiceImpl implements BoardCommandService {
    private RoomUserQueryRepository roomUserQueryRepository;
    private BoardQueryRepository boardQueryRepository;
    private BoardCommandRepository boardCommandRepository;
    private ReplyQueryRepository replyQueryRepository;
    private ReplyCommandRepository replyCommandRepository;
    private LikedQueryRepository likedQueryRepository;
    private LikedCommandRepository likedCommandRepository;
    private UserMissionQueryRepository userMissionQueryRepository;

    private EmitterService emitterService;

    @Autowired
    public BoardCommandServiceImpl(RoomUserQueryRepository roomUserQueryRepository,
                                   BoardQueryRepository boardQueryRepository, BoardCommandRepository boardCommandRepository,
                                   ReplyQueryRepository replyQueryRepository, ReplyCommandRepository replyCommandRepository,
                                   LikedQueryRepository likedQueryRepository, LikedCommandRepository likedCommandRepository,
                                   UserMissionQueryRepository userMissionQueryRepository,
                                   EmitterService emitterService

    ) {
        this.roomUserQueryRepository = roomUserQueryRepository;
        this.boardQueryRepository = boardQueryRepository;
        this.boardCommandRepository = boardCommandRepository;
        this.replyQueryRepository = replyQueryRepository;
        this.replyCommandRepository = replyCommandRepository;
        this.likedQueryRepository = likedQueryRepository;
        this.likedCommandRepository = likedCommandRepository;
        this.userMissionQueryRepository = userMissionQueryRepository;
        this.emitterService = emitterService;
    }

    @Override
    public Long updatePost(Long boardNo, Long roomNo, Long userNo, UpdateBoardRequestDto updateBoardRequestDto) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Room room = roomUser.getRoom();
            String newTitle = updateBoardRequestDto.getTitle();
            String newContent = updateBoardRequestDto.getContent();
            String newImgUrl = updateBoardRequestDto.getImgUrl();
            Boolean newPublicYn = updateBoardRequestDto.getPublicYn();

            Board post = boardQueryRepository.findById(boardNo).orElseThrow(() -> new BoardException("해당 게시글이 존재하지 않습니다. id=" + boardNo));

            if (!post.getRoomUser().equals(roomUser)) {
                throw new BoardException("수정 권한이 없습니다.");
            }

            if (post.getBoardCategory() == BoardCategory.NOTICE) {
                if(!room.getHostNo().equals(roomUser.getUser().getId())){
                    throw new BoardException("공지를 작성할 권한이 없습니다.");
                }
                if(!newPublicYn){
                    throw new BoardException("공지는 공개 설정만 할 수 있습니다.");
                }
                //인증
            } else if(post.getBoardCategory() == BoardCategory.BOAST && !updateBoardRequestDto.getPublicYn()){
                    throw new BoardException("자랑글은 공개 설정만 할 수 있습니다.");
            }

            post.updateBoard(boardNo, newTitle, newContent, newImgUrl, newPublicYn);

            return boardNo;
        }catch (Exception e) {
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public void deletePost(Long roomNo, Long userNo, Long boardNo) {
        try {

            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Board board = boardQueryRepository.findById(boardNo)
                    .orElseThrow(() -> new BoardException("해당 게시글이 없습니다. id=" + boardNo));

            if (!roomUser.equals(board.getRoomUser())) {
                throw new BoardException("해당 게시글을 삭제할 권한이 없습니다.");
            }

            if(board.getBoardCategory()==BoardCategory.CERTIFICATE) {
                board.getUserMission().updateUserMission();
            }

            boardCommandRepository.delete(board);
        } catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public Long writePost(Long roomNo, Long userNo, WriteBoardRequestDto writeBoardRequestDto) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));

            Room room = roomUser.getRoom();

            BoardCategory boardCategory = writeBoardRequestDto.getBoardCategory();
            UserMission userMission = null;

            //공지
            if (boardCategory == BoardCategory.NOTICE) {
                if(!room.getHostNo().equals(roomUser.getId())){
                    throw new BoardException("공지를 작성할 권한이 없습니다.");
                }
                if(!writeBoardRequestDto.getPublicYn()){
                    throw new BoardException("공지는 공개 설정만 할 수 있습니다.");
                }

                List<RoomUser> findRoomUsers = roomUserQueryRepository.findAllByRoomNo(roomNo);

                for(RoomUser ru : findRoomUsers) {

                    // 유저에게 알림 발송
                    AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                            .content("방장이 공지를 등록하였습니다.")
                            .readYn(false)
                            .generatedAt(LocalDateTime.now())
                            .author("방장")
                            .roomUserNo(ru.getId()).build();

                    emitterService.alarm(ru.getId(), alarmDataDto, "방장이 공지를 등록하였습니다.", "message");
                }

                //인증
            } else if (boardCategory == BoardCategory.CERTIFICATE) {
                    if(writeBoardRequestDto.getUserMissionNo()==null){
                        throw new BoardException("미션을 선택해주세요.");
                    }
                    userMission = userMissionQueryRepository.findById(writeBoardRequestDto.getUserMissionNo()).orElseThrow(()->new BoardException("해당 미션이 존재하지 않습니다."));
                    if(userMission.getMissionCertifyYn()){
                        throw new BoardException("이미 인증한 미션입니다.");
                    }
                    userMission.updateUserMission();
                //자랑
            } else {
                if(!writeBoardRequestDto.getPublicYn()){
                    throw new BoardException("자랑글은 공개 설정만 할 수 있습니다.");
                }
            }

            Board board = Board.builder()
                    .roomUser(roomUser)
                    .title(writeBoardRequestDto.getTitle())
                    .content(writeBoardRequestDto.getContent())
                    .imgUrl(writeBoardRequestDto.getImgUrl())
                    .registerAt(LocalDateTime.now())
                    .boardCategory(writeBoardRequestDto.getBoardCategory())
                    .publicYn(writeBoardRequestDto.getPublicYn())
                    .userMission(userMission)
                    .hit(0L)
                    .likedCount(0L)
                    .build();
            boardCommandRepository.save(board);
            return board.getId();
        }catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public void updateLike(Long roomNo, Long userNo, Long boardNo) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Board board = boardQueryRepository.findById(boardNo)
                    .orElseThrow(() -> new BoardException("해당 게시글이 없습니다. id=" + boardNo));

            likedQueryRepository.findLikedByBoardAndRoomUser(board, roomUser)
                    .ifPresent(a -> {
                        throw new BoardException("이미 좋아요를 눌렀습니다.");
                    });

            likedCommandRepository.save(new Liked(board, roomUser));
            board.updateLikedCount(board.getLikedCount() + 1);
        } catch (Exception e) {
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public void deleteLike(Long roomNo, Long userNo, Long boardNo) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Board board = boardQueryRepository.findById(boardNo)
                    .orElseThrow(() -> new BoardException("해당 게시글이 없습니다. id=" + boardNo));

            Liked liked = likedQueryRepository.findLikedByBoardAndRoomUser(board, roomUser)
                    .orElseThrow(() -> new BoardException("이미 처리된 요청입니다."));

            likedCommandRepository.delete(liked);

            board.updateLikedCount(board.getLikedCount() - 1);
        }catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public void writeReply(Long boardNo, Long roomNo, Long userNo, WriteReplyRequestDto writeReplyRequestDto) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Board board = boardQueryRepository.findById(boardNo)
                    .orElseThrow(() -> new BoardException("해당 게시글이 없습니다. id=" + boardNo));

            Long parentReplyNo = writeReplyRequestDto.getParentReplyNo();
            Long tagUserNo = null;

            if(writeReplyRequestDto.getParentReplyNo()!=null) {
                Reply parentReply = replyQueryRepository.findById(writeReplyRequestDto.getParentReplyNo()).orElseThrow(() -> new BoardException("부모 댓글이 존재하지 않습니다."));
                if(parentReply.getParentReplyNo()!=null){
                    parentReplyNo = parentReply.getParentReplyNo();
                    tagUserNo = parentReply.getRoomUser().getId();
                }
            }

            Reply reply = Reply.builder()
                    .roomUser(roomUser)
                    .board(board)
                    .content(writeReplyRequestDto.getContent())
                    .registerAt(LocalDateTime.now())
                    .parentReplyNo(parentReplyNo)
                    .tagUserNo(tagUserNo)
                    .anonymityYn(writeReplyRequestDto.isAnonymityYn())
                    .deleteYn(false)
                    .build();

            // 게시판 작성자에게 알림 발송
            if(reply.getParentReplyNo() == null) {

                // 유저에게 알림 발송
                AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                        .content("당신의 게시글에 댓글이 달렸습니다.")
                        .readYn(false)
                        .generatedAt(LocalDateTime.now())
                        .author(board.getId().toString())
                        .roomUserNo(board.getRoomUser().getId()).build();

                emitterService.alarm(board.getRoomUser().getId(), alarmDataDto, "게시글에 댓글이 달렸습니다.", "board");
            }

            // 댓글 작성자에게 대댓글 알림 발송
            else {

                Reply findParentReply = replyQueryRepository.findById(reply.getParentReplyNo())
                        .orElseThrow(() -> new BoardException("해당 댓글이 존재하지 않습니다."));

                // 유저에게 알림 발송
                AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                        .content("당신의 댓글에 답글이 달렸습니다.")
                        .readYn(false)
                        .generatedAt(LocalDateTime.now())
                        .author(board.getId().toString())
                        .roomUserNo(findParentReply.getRoomUser().getId()).build();

                emitterService.alarm(findParentReply.getRoomUser().getId(), alarmDataDto, "대댓글이 달렸습니다.", "board");
            }

            replyCommandRepository.save(reply);
        }catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public void deleteReply(Long roomNo, Long userNo, Long replyNo) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            Reply reply = replyQueryRepository.findById(replyNo)
                    .orElseThrow(() -> new BoardException("해당 댓글이 없습니다. id=" + replyNo));
            if (reply.getRoomUser().equals(roomUser))
                reply.deleteReply();
            else
                throw new BoardException("댓글을 삭제할 권한이 없습니다.");
        } catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }

    @Override
    public Long updateReply(Long roomNo, Long userNo, Long replyNo, UpdateReplyRequestDto updateReplyRequestDto) {
        try {
            RoomUser roomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo).orElseThrow(()->new BoardException("방에 참여한 유저가 아닙니다."));
            String newContent = updateReplyRequestDto.getContent();
            Boolean newAnnonymityYn = updateReplyRequestDto.getAnonymityYn();

            Reply reply = replyQueryRepository.findById(replyNo).orElseThrow(
                    () -> new BoardException("해당 댓글이 없습니다. id=" + replyNo));

            if(reply.getRoomUser().equals(roomUser)) {
                reply.updateReply(newContent, newAnnonymityYn);
                if(updateReplyRequestDto.getAnonymityYn() && !reply.getRoomUser().equals(reply.getBoard().getRoomUser())){
                    throw new BoardException("게시글 작성자만 댓글 익명 체크를 할 수 있습니다.");
                }
            }
            else
                throw new BoardException("해당 댓글을 수정할 권할이 없습니다.");
            return replyNo;
        } catch (Exception e){
            throw new BoardException(e.getMessage());
        }
    }
}
