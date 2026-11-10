package com.wersiv.datingsite.backend.message.repository;

import com.wersiv.datingsite.backend.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where " +
            "(m.senderId = :userA and m.receiverId = :userB) or " +
            "(m.senderId = :userB and m.receiverId = :userA) " +
            "order by m.sentAt asc")
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);
}
