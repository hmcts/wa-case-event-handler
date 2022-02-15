package uk.gov.hmcts.reform.wacaseeventhandler.entities.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@ToString
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
    
    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getName() {
        return name;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

}
