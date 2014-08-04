package org.miles2run.business.domain.jpa;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Created by shekhargulati on 04/03/14.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "SocialConnection.findByConnectionId",query = "SELECT s from SocialConnection s WHERE s.connectionId =:connectionId")
})
@Access(AccessType.FIELD)
@Table(name="social_connection")
public class SocialConnection extends BaseEntity{

    @NotNull
    private String accessToken;

    private String accessSecret;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

    private String handle;

    @NotNull
    private String connectionId;

    @ManyToOne
    private Profile profile;

    public SocialConnection() {

    }

    public SocialConnection(String accessToken, String accessSecret, SocialProvider provider, String handle, String connectionId) {
        this.accessToken = accessToken;
        this.accessSecret = accessSecret;
        this.provider = provider;
        this.handle = handle;
        this.connectionId = connectionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAccessSecret() {
        return accessSecret;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getHandle() {
        return handle;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public String toString() {
        return "SocialConnection{" +
                "id=" + id +
                ", accessToken='" + accessToken + '\'' +
                ", accessSecret='" + accessSecret + '\'' +
                ", provider=" + provider +
                ", handle='" + handle + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", createdAt=" + createdAt +
                ", profile=" + profile +
                '}';
    }
}