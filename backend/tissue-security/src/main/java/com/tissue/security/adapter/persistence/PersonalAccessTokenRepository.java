package com.tissue.security.adapter.persistence;

import com.tissue.security.domain.PersonalAccessToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PersonalAccessTokenRepository extends Repository<PersonalAccessToken, Long> {

    PersonalAccessToken save(PersonalAccessToken token);

    @Query("SELECT p FROM PersonalAccessToken p JOIN FETCH p.member WHERE p.tokenHash = :tokenHash")
    Optional<PersonalAccessToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    List<PersonalAccessToken> findAllByMember_Id(Long memberId);

    Optional<PersonalAccessToken> findByIdAndMember_Id(Long id, Long memberId);
}
