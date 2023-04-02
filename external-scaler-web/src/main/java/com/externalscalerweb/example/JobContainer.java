package com.externalscalerweb.example;

import java.util.concurrent.atomic.AtomicInteger;

public class JobContainer {

    private static JobContainer jobContainer;
    private AtomicInteger numberOfJobs = new AtomicInteger(0);

    JobContainer() {
    }

    public static JobContainer instance() {
        if (jobContainer == null) {
            System.out.println("Creating Job Container...");
            jobContainer = new JobContainer();
            System.out.println("Job Container created.");
        }

        return jobContainer;
    }

    public Job getJob(int id) {
        Job job = null;

        if ((id > 0) && (id <= numberOfJobs.get())) {
            job = newJob(id);
        }

        System.out.println("Get Job: " + job);

        return job;
    }

    public Job createJobs(int numberOfJobs) {
        Job job = newJob(this.numberOfJobs.addAndGet(numberOfJobs));
        System.out.println("Create Jobs: " + job);
        return job;
    }

    public Job deleteJob(int id) {
        int numberOfJobsInteger = 0;
        System.out.println("Delete Job: " + id);
        Job job = getJob(id);
        if (job != null) {
            numberOfJobsInteger = numberOfJobs.updateAndGet(numberOfJobsValue -> numberOfJobsValue > 0 ? numberOfJobsValue - 1 : 0); 
        }
        System.out.println("Delete Job: Number Of Jobs: " + numberOfJobsInteger);
        return job;
    }

    public Integer getNumberOfJobs() {
        int numberOfJobsInteger = 0;
        numberOfJobsInteger = numberOfJobs.updateAndGet(numberOfJobsValue -> numberOfJobsValue < 0 ? 0 : numberOfJobsValue); 
        System.out.println("Get Number Of Jobs: " + numberOfJobsInteger);
        return numberOfJobsInteger;
    }

    private Job newJob(int id) {
        return new Job(id);
    }
}
