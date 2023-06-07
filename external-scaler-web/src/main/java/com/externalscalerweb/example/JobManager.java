package com.externalscalerweb.example;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/*
 * The JobManager class is the REST API controller for the Tomcat External Web application
 * It is supporting 4 REST API calls that are called by the External Scaler gRPC Server and the Worker nodes
 * - GET /jobs
 * - GET /jobs/{id}
 * - POST /jobs
 * - DELETE /jobs/{id}
 *
 */
@Path("jobs")
public class JobManager {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getJobs() {
        return JobContainer.instance().getNumberOfJobs();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(@PathParam("id") int id) {
        return JobContainer.instance().getJob(id);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Job createJobs(@QueryParam("number_of_jobs") int numberOfJobs) {
        return JobContainer.instance().createJobs(numberOfJobs);
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job deleteJob(@PathParam("id") int id) {
        return JobContainer.instance().deleteJob(id);
    }
}
