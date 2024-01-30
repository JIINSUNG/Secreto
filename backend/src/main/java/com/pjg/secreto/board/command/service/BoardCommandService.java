package com.pjg.secreto.board.command.service;

import com.pjg.secreto.board.command.dto.UpdateBoardRequestDto;
import com.pjg.secreto.board.command.dto.WriteReplyRequestDto;
import com.pjg.secreto.board.command.dto.WriteBoardRequestDto;

public interface BoardCommandService {
    public Long updatePost(UpdateBoardRequestDto updateBoardRequestDto);

    public void deletePost(Long boardNo);

    public void writePost(WriteBoardRequestDto writeBoardRequestDto);

    public void updateLike(Long boardNo, Long roomUserNo);

    public void deleteLike(Long boardNo, Long roomUserNo);

    public void writeReply(WriteReplyRequestDto writeReplyRequestDto);

    public void deleteReply(Long replyNo);
}
