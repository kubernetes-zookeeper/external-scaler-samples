package com.externalscalerweb.example;

import java.util.Objects;

/* 
 * The Job class is representing a job
 * Its data member is the numeric integer id of the job
 *
 */
public class Job {
    private int id;

    public Job(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Job)) {
            return false;
        }
        return id == ((Job) obj).id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }
}
