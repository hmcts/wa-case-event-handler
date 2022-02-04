package uk.gov.hmcts.reform.wacaseeventhandler.entities.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@EqualsAndHashCode
@ToString
@Getter
public class UserInfo {

    @JsonProperty("sub")
    private String email;
    private String uid;
    private List<String> roles;
    private String name;
    private String givenName;
    private String familyName;

    public UserInfo() {
        //No-op constructor for deserialization
    }

    public UserInfo(String email, String uid, List<String> roles, String name, String givenName, String familyName) {
        this.email = email;
        this.uid = uid;
        this.roles = roles;
        this.name = name;
        this.givenName = givenName;
        this.familyName = familyName;
    }

}
