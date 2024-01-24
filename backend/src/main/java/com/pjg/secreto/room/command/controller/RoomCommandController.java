package com.pjg.secreto.room.command.controller;

import com.pjg.secreto.common.response.SuccessResponse;
import com.pjg.secreto.room.command.dto.*;
import com.pjg.secreto.room.command.service.RoomCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RequestMapping("/room")
@RestController
public class RoomCommandController {

    private final RoomCommandService roomCommandService;

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequestDto createRoomRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        CreateRoomResponseDto result = roomCommandService.createRoom(createRoomRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방이 생성되었습니다.", result));
    }

    @PutMapping
    public ResponseEntity<?> changeRoomName(@RequestBody ChangeRoomNameRequestDto changeRoomNameRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.changeRoomName(changeRoomNameRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방 이름이 변경되었습니다.", null));
    }

    @PutMapping("/{roomNo}")
    public ResponseEntity<?> setRoom(@PathVariable Long roomNo) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        SetRoomResponseDto result = roomCommandService.setRoom(roomNo);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방을 시작하였습니다.", result));
    }

    @PostMapping("/enter")
    public ResponseEntity<?> enterRoom(@RequestBody EnterRoomRequestDto enterRoomRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.enterRoom(enterRoomRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방에 입장하였습니다.", null));
    }

    @PutMapping("/set_nickname")
    public ResponseEntity<?> setNickname(@RequestBody SetNicknameRequestDto setNicknameRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.setNickname(setNicknameRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "닉네임을 설정하였습니다.", null));
    }

    @PutMapping("/accept")
    public ResponseEntity<?> acceptUser(@RequestBody AcceptUserRequestDto acceptUserRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.acceptUser(acceptUserRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "유저를 수락하였습니다.", null));
    }

    @PutMapping("/deligate")
    public ResponseEntity<?> deligateAdmin(@RequestBody DeligateAdminRequestDto deligateAdminRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.deligateAdmin(deligateAdminRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "관리자 권한을 위임하였습니다.", null));
    }

    @PutMapping("/bookmark")
    public ResponseEntity<?> bookmarkRoom(@RequestBody BookmarkRoomRequestDto bookmarkRoomRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.bookmarkRoom(bookmarkRoomRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방을 즐겨찾기 하였습니다.", null));
    }

    @PutMapping("/terminate")
    public ResponseEntity<?> terminateRoom(@RequestBody TerminateRoomRequestDto terminateRoomRequestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        roomCommandService.terminateRoom(terminateRoomRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK, "방을 종료하였습니다.", null));
    }

}
