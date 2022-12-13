package uk.gov.hmcts.reform.wacaseeventhandler.entity;

import java.time.LocalDateTime;
import java.util.Map;

public class CaseEventMessageEntityTest {

    private CaseEventMessageEntityTest() {
    }

    public static CaseEventMessageEntity buildMessageEntity(Map<String, Object> map, MessageState state) {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();

        for (Map.Entry<String, Object> val : map.entrySet()) {
            Object value = val.getValue();
            switch (val.getKey()) {
                case "messageId":
                    entity.setMessageId((String) value);
                    break;
                case "eventTimeStamp":
                    entity.setEventTimestamp(LocalDateTime.parse((String) value));
                    break;
                default:
                    break;
            }
        }

        entity.setState(state);

        return entity;
    }

}
