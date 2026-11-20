package com.gaeddong.footy.domain.poll.domain.repository;

import com.gaeddong.footy.domain.poll.domain.PollAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollAnswerRepository extends JpaRepository<PollAnswer, Long> {

    // 투표자 닉네임을 함께 보여주므로 회원을 같은 쿼리로 가져온다
    @EntityGraph(attributePaths = "member")
    List<PollAnswer> findAllByPollOptionPollIdOrderByIdAsc(Long pollId);

    // 응답 교체 전에 바로 지운다. 파생 delete는 새 행 insert 뒤에 실행될 수 있다.
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from PollAnswer pa
            where pa.member.id = :memberId
              and pa.pollOption.id in (select o.id from PollOption o where o.poll.id = :pollId)
            """)
    void deleteAllByPollIdAndMemberId(@Param("pollId") Long pollId, @Param("memberId") Long memberId);

    @Query("""
            select o.poll.id as pollId, count(distinct pa.member.id) as voterCount
            from PollAnswer pa join pa.pollOption o
            where o.poll.game.id = :gameId
            group by o.poll.id
            """)
    List<VoterCount> countVotersByGameId(@Param("gameId") Long gameId);

    @Query("""
            select distinct o.poll.id
            from PollAnswer pa join pa.pollOption o
            where o.poll.game.id = :gameId and pa.member.id = :memberId
            """)
    List<Long> findVotedPollIds(@Param("gameId") Long gameId, @Param("memberId") Long memberId);

    interface VoterCount {

        Long getPollId();

        long getVoterCount();
    }
}
