package com.pjg.secreto.room.query.repository;

import com.pjg.secreto.room.common.entity.Room;
import com.pjg.secreto.room.common.entity.RoomUser;
import com.pjg.secreto.user.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomUserQueryRepository extends JpaRepository<RoomUser, Long>, RoomUserQueryRepositoryCustom{


    RoomUser findRoomUserByRoomAndUser(Room room, User user);

    List<RoomUser> findRoomUserByRoom(Room findRoom);

    @Query("select ru from RoomUser ru where ru.room.id = :roomNo")
    List<RoomUser> findAllByRoomId(Long roomNo);

    @Query("select ru from RoomUser ru join fetch ru.room where ru.user.id = :userNo")
    List<RoomUser> findAllWithRoomByUserNo(Long userNo);

    @Query("select ru from RoomUser ru where ru.user.id = :userNo and ru.room.id = :roomNo")
    RoomUser findByUserNoAndRoomNo(Long userNo, Long roomNo);

    @Query("select ru from RoomUser ru where ru.id in :roomUserNos")
    List<RoomUser> findByRoomUserNos(List<Long> roomUserNos);

    @Query("select ru from RoomUser ru where ru.id in :roomUserNos and ru.room.id = :roomNo")
    List<RoomUser> findAllByRoomUserNosAndRoomNo(List<Long> roomUserNos, Long roomNo);
}