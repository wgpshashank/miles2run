package org.miles2run.business.domain.jpa;

import org.hibernate.validator.constraints.URL;
import org.miles2run.business.bean_validation.CommunityRunDateRange;
import org.miles2run.business.bean_validation.ImageUrl;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

/**
 * Created by shekhargulati on 08/07/14.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "community_run", indexes = {
        @Index(columnList = "name", unique = true),
        @Index(columnList = "slug", unique = true)
})
@NamedQueries({
        @NamedQuery(name = "CommunityRun.findAllActiveRaces", query = "SELECT new CommunityRun(cr) FROM CommunityRun cr WHERE cr.active IS TRUE"),
        @NamedQuery(name = "CommunityRun.findBySlug", query = "SELECT new CommunityRun(cr) FROM CommunityRun cr WHERE cr.slug =:slug"),
        @NamedQuery(name = "CommunityRun.findBySlugWithProfiles", query = "SELECT cr FROM CommunityRun cr WHERE cr.slug =:slug"),
        @NamedQuery(name = "CommunityRun.findAllActiviRunsByNameLike", query = "SELECT new CommunityRun(cr) from CommunityRun cr WHERE LOWER(cr.name) LIKE :name and cr.active IS TRUE")
})
@CommunityRunDateRange
public class CommunityRun extends BaseEntity {

    @NotNull
    private String name;

    @NotNull
    @ImageUrl
    private String bannerImg;

    private String slug;

    @NotNull
    @Size(max = 4000)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date endDate;

    @URL
    private String website;

    @NotNull
    private String twitterHandle;

    @NotNull
    @Size(max = 10)
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "communityrun_hashtags", joinColumns = {
            @JoinColumn(name = "communityRun_Id")
    })
    private Set<String> hashtags = new HashSet<>();

    @ManyToMany
    private List<Profile> profiles = new ArrayList<>();


    private boolean active = true;

    public CommunityRun() {
    }

    public CommunityRun(CommunityRun communityRun) {
        this.name = communityRun.name;
        this.bannerImg = communityRun.bannerImg;
        this.slug = communityRun.slug;
        this.description = communityRun.description;
        this.startDate = communityRun.startDate;
        this.endDate = communityRun.endDate;
        this.website = communityRun.website;
        this.twitterHandle = communityRun.twitterHandle;
        this.hashtags = communityRun.hashtags;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    public String getTwitterHandle() {
        return twitterHandle;
    }

    public void setTwitterHandle(String twitterHandle) {
        this.twitterHandle = twitterHandle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBannerImg() {
        return bannerImg;
    }

    public void setBannerImg(String bannerImg) {
        this.bannerImg = bannerImg;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public CommunityRun(String name, String bannerImg, String slug, String description, Date startDate, Date endDate, String website, String twitterHandle) {
        this.name = name;
        this.bannerImg = bannerImg;
        this.slug = slug;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.website = website;
        this.twitterHandle = twitterHandle;
    }
}