package org.miles2run.jaxrs.api.v1;

import org.apache.commons.lang3.StringUtils;
import org.jug.filters.InjectPrincipal;
import org.jug.filters.LoggedIn;
import org.jug.view.View;
import org.miles2run.business.domain.jpa.CommunityRun;
import org.miles2run.business.domain.jpa.Goal;
import org.miles2run.business.domain.jpa.Profile;
import org.miles2run.business.domain.jpa.Role;
import org.miles2run.business.services.GoalService;
import org.miles2run.business.services.ProfileMongoService;
import org.miles2run.business.services.jpa.CommunityRunJPAService;
import org.miles2run.business.services.redis.CommunityRunRedisService;
import org.miles2run.business.services.ProfileService;
import org.miles2run.business.utils.SlugUtils;
import org.miles2run.business.vo.ProfileGroupDetails;
import org.miles2run.jaxrs.vo.CommunityRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shekhargulati on 10/07/14.
 */
@Path("/api/v1/community_runs")
public class CommunityRunResource {

    private Logger logger = LoggerFactory.getLogger(CommunityRunResource.class);

    @Inject
    private CommunityRunRedisService communityRunRedisService;
    @Inject
    private CommunityRunJPAService communityRunJPAService;
    @Context
    private SecurityContext securityContext;
    @Inject
    private ProfileService profileService;
    @Inject
    private ProfileMongoService profileMongoService;
    @Inject
    private GoalService goalService;

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @LoggedIn
    public Response createCommunityRun(@Valid CommunityRun communityRun) {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Profile profile = profileService.findProfileByUsername(loggedInUser);
        if (profile.getRole() == Role.ADMIN || profile.getRole() == Role.ORGANIZER) {
            communityRun.setSlug(SlugUtils.toSlug(communityRun.getName()));
            Long id = communityRunJPAService.save(communityRun);
            communityRunRedisService.addCommunityRunToSet(communityRun.getSlug());
            return Response.status(Response.Status.CREATED).entity(id).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED).build();

    }


    @GET
    @Produces("application/json")
    @InjectPrincipal
    public List<CommunityRunDetails> allCommunityRuns(@QueryParam("name") String name, @QueryParam("include_stats") boolean includeStats, @QueryParam("include_participation_detail") boolean includeParticipationDetail) {
        if (StringUtils.isNotBlank(name)) {
            return toCommunityRunDetailsList(communityRunJPAService.findAllActiveCommunityRunsWithNameLike(name));
        }
        List<CommunityRun> activeCommunityRuns = communityRunJPAService.findAllActiveCommunityRuns();
        List<CommunityRunDetails> communityRunDetailsList = new ArrayList<>();
        for (CommunityRun activeCommunityRun : activeCommunityRuns) {
            String slug = activeCommunityRun.getSlug();
            CommunityRunDetails communityRunDetails = CommunityRunDetails.fromCommunityRun(activeCommunityRun);
            if (includeStats) {
                communityRunDetails.addStats(communityRunRedisService.getCurrentStatsForCommunityRun(slug));
            }
            if (includeParticipationDetail && securityContext.getUserPrincipal() != null) {
                String principal = securityContext.getUserPrincipal().getName();
                if (communityRunRedisService.isUserAlreadyPartOfRun(slug, principal)) {
                    boolean participating = true;
                    communityRunDetails.addParticipationDetails(participating);
                }
            }
            communityRunDetailsList.add(communityRunDetails);
        }
        return communityRunDetailsList;
    }

    private List<CommunityRunDetails> toCommunityRunDetailsList(List<CommunityRun> allActiveCommunityRunsWithNameLike) {
        List<CommunityRunDetails> communityRunDetailsList = new ArrayList<>();
        for (CommunityRun communityRun : allActiveCommunityRunsWithNameLike) {
            communityRunDetailsList.add(CommunityRunDetails.fromCommunityRun(communityRun));
        }
        return communityRunDetailsList;
    }

    @Path("/{slug}")
    @GET
    @Produces("application/json")
    public CommunityRun findCommunityRun(@NotNull @PathParam("slug") String slug) {
        return communityRunJPAService.findBySlug(slug);
    }

    @Path("/{slug}/profiles_group_city")
    @GET
    @Produces("application/json")
    public List<ProfileGroupDetails> groupAllUsersInACommunityRunByCity(@NotNull @PathParam("slug") String slug) {
        List<ProfileGroupDetails> profileGroupDetailsList = communityRunJPAService.groupAllUserInACommunityRunByCity(slug);
        List<ProfileGroupDetails> profileGroupDetailsListWithLatLng = new ArrayList<>();
        for (ProfileGroupDetails profileGroupDetails : profileGroupDetailsList) {
            profileGroupDetailsListWithLatLng.add(ProfileGroupDetails.newWitLatLng(profileGroupDetails, profileMongoService.findLatLngForACity(profileGroupDetails.getCity())));
        }
        return profileGroupDetailsListWithLatLng;
    }


    // TODO : CODE COPIED FROM CommunityRunView
    @Path("/{slug}/join")
    @POST
    @Produces("text/html")
    @LoggedIn
    public Response joinCommunityRun(@NotNull @PathParam("slug") final String slug) {
        if (!communityRunRedisService.communityRunExists(slug)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String principal = securityContext.getUserPrincipal().getName();
        if (communityRunRedisService.isUserAlreadyPartOfRun(slug, principal)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("You are already part of this community run").build();
        }
        Profile profile = profileService.findProfile(principal);
        logger.info("Adding profile {} to community run ", principal, slug);
        CommunityRun communityRun = communityRunJPAService.addRunnerToCommunityRun(slug, profile);

        Goal goal = Goal.newCommunityRunGoal(communityRun);
        logger.info("Creating a CommunityRun goal for profile {}", principal);
        Goal savedGoal = goalService.save(goal, profile);
        logger.info("Created a new goal with id {}", savedGoal.getId());

        communityRunRedisService.addGoalToCommunityRun(slug, savedGoal.getId());
        communityRunRedisService.addRunnerToCommunityRun(slug, profile);
        return Response.status(Response.Status.CREATED).entity("Successfully joined community run").build();
    }
}