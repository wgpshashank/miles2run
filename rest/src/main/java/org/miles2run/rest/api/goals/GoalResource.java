package org.miles2run.rest.api.goals;

import org.apache.commons.lang3.StringUtils;
import org.jug.filters.LoggedIn;
import org.miles2run.core.repositories.jpa.GoalRepository;
import org.miles2run.core.repositories.jpa.ProfileRepository;
import org.miles2run.core.repositories.redis.GoalStatsRepository;
import org.miles2run.domain.entities.*;
import org.miles2run.representations.GoalRepresentation;
import org.miles2run.representations.GoalRepresentationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("goals")
public class GoalResource {

    private Logger logger = LoggerFactory.getLogger(GoalResource.class);

    @Context
    private SecurityContext securityContext;
    @Inject
    private ProfileRepository profileRepository;
    @Inject
    private GoalRepository goalRepository;
    @Inject
    private GoalStatsRepository goalStatsRepository;

    @GET
    @Produces("application/json")
    @LoggedIn
    public Response allGoals(@QueryParam("archived") boolean archived, @QueryParam("groupByType") Boolean groupByType) {
        Profile profile = getProfile();
        List<Goal> goals = goalRepository.findAll(profile, archived);
        if (groupByType != null && groupByType) {
            return groupGoalsByType(goals);
        }
        return Response.status(Response.Status.OK).entity(goals).build();
    }

    private Profile getProfile() {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        return profileRepository.findByUsername(loggedInUser);
    }

    private Response groupGoalsByType(List<Goal> goals) {
        Map<GoalType, List<Object>> goalsByType = new HashMap<>();
        for (Goal goal : goals) {
            GoalType goalType = toGoalType(goal);
            double totalDistanceCoveredForGoal = goalStatsRepository.distanceCovered(goal.getId());
            GoalRepresentation specificGoal = GoalRepresentationFactory.toGoalType(goal, totalDistanceCoveredForGoal);
            if (goalsByType.containsKey(goalType)) {
                List<Object> goalsForType = goalsByType.get(goalType);
                goalsForType.add(specificGoal);
            } else {
                List<Object> goalsForType = new ArrayList<>();
                goalsForType.add(specificGoal);
                goalsByType.put(goalType, goalsForType);
            }
        }
        return Response.status(Response.Status.OK).entity(goalsByType).build();
    }

    private GoalType toGoalType(Goal goal) {
        if (goal instanceof DistanceGoal) {
            return GoalType.DISTANCE_GOAL;
        } else if (goal instanceof DurationGoal) {
            return GoalType.DURATION_GOAL;
        }
        return GoalType.COMMUNITY_RUN_GOAL;
    }

    @Path("{goalId}")
    @GET
    @Produces("application/json")
    @LoggedIn
    public GoalRepresentation goal(@PathParam("goalId") Long goalId) {
        Profile profile = getProfile();
        Goal goal = goalRepository.find(profile, goalId);
        double distanceCovered = goalStatsRepository.distanceCovered(goal.getId());
        return GoalRepresentationFactory.toGoalType(goal, distanceCovered);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @LoggedIn
    public Response createGoal(@Valid GoalRequest goalRequest) {
        try {
            logger.info("Goal : " + goalRequest);
            Goal goal = goalRequest.toGoal(getProfile());
            Goal createdGoal = goalRepository.save(goal);
            return Response.status(Response.Status.CREATED).entity(GoalRepresentationFactory.toGoalType(createdGoal, 0)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Path("{goalId}")
    @PUT
    @Produces("application/json")
    @LoggedIn
    public Response updateGoal(@PathParam("goalId") Long goalId, @Valid GoalRequest goalRequest) {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Goal existingGoal = goalRepository.find(goalId);
        if (existingGoal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }
        if (!StringUtils.equals(loggedInUser, existingGoal.getProfile().getUsername())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // TODO: FIX ME
//        updateExistingGoal(existingGoal, goalRequest);

        goalRepository.update(existingGoal);
        return Response.status(Response.Status.OK).build();
    }


    @Path("/{goalId}/archive")
    @PUT
    @Produces("application/json")
    @LoggedIn
    public Response archiveGoal(@PathParam("goalId") Long goalId, @QueryParam("archived") boolean archived) {
        Profile profile = getProfile();
        Goal goal = goalRepository.find(profile, goalId);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }
        goal.setArchived(true);
        goalRepository.update(goal);
        return Response.status(Response.Status.OK).build();
    }

/*    @Path("/{goalId}/progress")
    @GET
    @Produces("application/json")
    @LoggedIn
    public Response progress(@PathParam("goalId") Long goalId, @CookieParam("timezoneoffset") int timezoneOffset) {
        String username = securityContext.getUserPrincipal().getName();
        Profile loggedInUser = profileRepository.findByUsername(username);
        Goal goal = goalRepository.find(loggedInUser, goalId);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }
        Progress progress = activityRepository.calculateUserProgressForGoal(loggedInUser, goal);
        if (goal.getGoalType() == GoalType.DISTANCE_GOAL) {
            return Response.status(Response.Status.OK).entity(progress).build();
        }
        Map<String, Object> goalProgress = goalStatsRepository.getDurationGoalProgress(username, goalId, new Interval(goal.getStartDate().getTime(), goal.getEndDate().getTime()), timezoneOffset);
        goalProgress.put("activityCount", progress.getActivityCount());
        goalProgress.put("totalDistanceCovered", progress.getTotalDistanceCovered());
        goalProgress.put("goalUnit", progress.getGoalUnit());
        return Response.status(Response.Status.OK).entity(goalProgress).build();
    }*/

}
