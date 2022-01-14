package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Repository
public class CaseEventMessageCustomCriteriaRepository {

    @PersistenceContext
    private EntityManager em;

    public Long countAll() {
        String jpql = "SELECT count(ceme) FROM CaseEventMessageEntity ceme";
        Query queryCount = em.createQuery(jpql);
        return ((Number) queryCount.getSingleResult()).longValue();
    }

    public List<CaseEventMessageEntity> getMessages(List<MessageState> states,
                                                    String caseId,
                                                    LocalDateTime eventTimestamp,
                                                    Boolean fromDlq) {
        CriteriaBuilder criteriaBuilder = criteriaBuilder();
        CriteriaQuery<CaseEventMessageEntity> criteriaQuery = criteriaBuilder.createQuery(CaseEventMessageEntity.class);
        Root<CaseEventMessageEntity> itemRoot = criteriaQuery.from(CaseEventMessageEntity.class);

        List<Predicate> mainAndOperatorPredicates = new ArrayList<>();

        if (caseId != null) {
            Predicate predicateForCaseId
                = criteriaBuilder.equal(itemRoot.get("caseId"), caseId);
            mainAndOperatorPredicates.add(predicateForCaseId);
        }

        if (!states.isEmpty()) {
            Predicate[] statesPredicateArray = states.stream()
                .map(state -> criteriaBuilder.equal(itemRoot.get("state"), state)).toArray(Predicate[]::new);

            mainAndOperatorPredicates.add(criteriaBuilder.or(statesPredicateArray));
        }

        if (eventTimestamp != null) {
            Predicate predicateForEventTimestamp
                = criteriaBuilder.between(itemRoot.get("eventTimestamp"),
                                          eventTimestamp.minus(1, ChronoUnit.MILLIS),
                                          eventTimestamp.plus(1, ChronoUnit.MILLIS));
            mainAndOperatorPredicates.add(predicateForEventTimestamp);
        }

        if (fromDlq != null) {
            if (fromDlq) {
                mainAndOperatorPredicates.add(criteriaBuilder.isTrue(itemRoot.get("fromDlq")));
            } else {
                mainAndOperatorPredicates.add(criteriaBuilder.isFalse(itemRoot.get("fromDlq")));
            }
        }

        Predicate finalPredicate = criteriaBuilder.and(mainAndOperatorPredicates.toArray(Predicate[]::new));
        criteriaQuery.where(finalPredicate);

        return em.createQuery(criteriaQuery).getResultList();
    }

    private CriteriaBuilder criteriaBuilder() {
        return em.getCriteriaBuilder();
    }
}
