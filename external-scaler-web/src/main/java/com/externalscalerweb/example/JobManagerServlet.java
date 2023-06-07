package com.externalscalerweb.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/*
 * The JobManagerServlet is the Tomcat HTTP Servlet for the Tomcat External Scaler Web application
 *
 */
public class JobManagerServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter writer = response.getWriter();
        writer.println("Job Manager\nNumber of Jobs: " + JobContainer.instance().getNumberOfJobs());
        writer.close();
    }
}
