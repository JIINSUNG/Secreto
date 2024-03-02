package com.pjg.secreto.chat.service;

import com.pjg.secreto.alarm.dto.AlarmDataDto;
import com.pjg.secreto.alarm.service.EmitterService;
import com.pjg.secreto.chat.common.exception.ChatException;
import com.pjg.secreto.chat.dto.ChatMessageDto;
import com.pjg.secreto.chat.dto.ChatMessagesResponseDto;
import com.pjg.secreto.chat.dto.ShowChatUserListResponseDto;
import com.pjg.secreto.chat.entity.Chat;
import com.pjg.secreto.chat.entity.ChatMessage;
import com.pjg.secreto.chat.entity.ChatUser;
import com.pjg.secreto.chat.entity.ChattingUserType;
import com.pjg.secreto.chat.repository.ChatMessageCustomRepository;
import com.pjg.secreto.chat.repository.ChatMessageRepository;
import com.pjg.secreto.chat.repository.ChatRepository;
import com.pjg.secreto.chat.repository.ChatUserRepository;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.room.query.repository.RoomUserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatUserRepository chatUserRepository;
    private final RoomUserQueryRepository roomUserQueryRepository;
    private final ChatMessageCustomRepository chatMessageCustomRepository;
    private final EmitterService emitterService;

    public void chatting(ChatMessageDto chatMessageDto) {

        try {

            Chat findChat = chatRepository.findById(chatMessageDto.getChatNo())
                    .orElseThrow(() -> new ChatException("채팅 예외 발생"));

            ChatMessage chatMessage = ChatMessage.builder()
                    .sender(chatMessageDto.getSender())
                    .message(chatMessageDto.getMessage())
                    .readYn(false)
                    .chat(findChat)
                    .senderId(chatMessageDto.getSenderId())
                    .sendAt(LocalDateTime.now()).build();

            chatMessageRepository.save(chatMessage);

            List<ChatUser> chatUsers = chatUserRepository.findAllByChatNo(findChat.getId());

            for(ChatUser cu : chatUsers) {

                // 자기 자신일 경우 SSE 스킵
                if(Objects.equals(cu.getRoomUser().getId(), chatMessageDto.getSenderId())) {
                    continue;
                }

                // 유저에게 알림 발송
                AlarmDataDto alarmDataDto = AlarmDataDto.builder()
                        .content("채팅 발생")
                        .readYn(false)
                        .generatedAt(LocalDateTime.now())
                        .author(cu.getChattingUserType().toString())
                        .roomUserNo(cu.getRoomUser().getId()).build();

                emitterService.alarmWithNoStore(cu.getRoomUser().getId(), alarmDataDto, "채팅 발생", "chat");
            }

        } catch (Exception e) {

            throw new ChatException(e.getMessage());
        }

    }

    @Transactional(readOnly = true)
    public List<ShowChatUserListResponseDto> showChatUserList(Long userNo, Long roomNo) {

        try {
            RoomUser findRoomUser = roomUserQueryRepository.findByUserNoAndRoomNo(userNo, roomNo)
                    .orElseThrow(() -> new ChatException("유저가 해당 방에 속해있지 않습니다."));

            List<ChatUser> chatUsers = chatUserRepository.findAllByRoomUserNo(findRoomUser.getId());

            List<ShowChatUserListResponseDto> result = chatUsers.stream().map(cu ->
                    ShowChatUserListResponseDto.builder()
                            .chattingUserType(cu.getChattingUserType())
                            .chatUserNo(cu.getId())
                            .chatNo(cu.getChat().getId())
                            .roomUserNo(cu.getRoomUser().getId()).build()).toList();

            return result;

        } catch (Exception e) {
            throw new ChatException(e.getMessage());
        }
    }

    public List<ChatMessagesResponseDto> getChatHistory(String type, Long chatRoomNo){
        ChattingUserType roomType = ChattingUserType.valueOf(type.toUpperCase());
        return chatMessageCustomRepository.getChatHistory(roomType, chatRoomNo);
    }
}
