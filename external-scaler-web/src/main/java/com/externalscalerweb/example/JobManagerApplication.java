package com.externalscalerweb.example;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/*
 * JobManagerApplication is the Tomcat web application for the External Scaler Web
 * The base URL of its REST API calls is /api
 *
 */
@ApplicationPath("/api")
public class JobManagerApplication extends Application {
}
