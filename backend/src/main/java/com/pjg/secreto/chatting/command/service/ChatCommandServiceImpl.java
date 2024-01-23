package com.pjg.secreto.chatting.command.service;

import com.pjg.secreto.chatting.command.repository.ChatCommandRepository;
import com.pjg.secreto.chatting.common.entity.ChatMessage;
import com.pjg.secreto.chatting.common.entity.GroupChatMessage;
import com.pjg.secreto.chatting.query.repository.ChatQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatCommandServiceImpl implements ChatCommandService {

    private ChatQueryRepository chatQueryRepository;
    private ChatCommandRepository chatCommandRepository;
    @Override
    public void saveMessage(ChatMessage chatMessage) throws Exception {

    }

    @Override
    public void saveGroupMessage(GroupChatMessage groupChatMessage) throws Exception {

    }
}
