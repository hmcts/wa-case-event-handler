package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;

import java.util.List;

@Repository
public interface CaseEventMessageRepository extends CrudRepository<CaseEventMessageEntity, Long> {

    @Query("FROM CaseEventMessageEntity cem")
    List<CaseEventMessageEntity> findAllMessages();

    @Query("FROM CaseEventMessageEntity cem WHERE cem.messageId=:messageId")
    List<CaseEventMessageEntity> findByMessageId(String messageId);
}
