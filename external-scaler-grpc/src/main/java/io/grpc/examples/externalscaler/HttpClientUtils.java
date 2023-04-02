package io.grpc.examples.externalscaler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_CONNECTION_TIMEOUT = 60;
    private static final int DEFAULT_READ_TIMEOUT = 119;

    public static <T, E> T executeGetRequest(String path, boolean trustAnySSLCertificate, Class<T> responseClass, Class<E> errorClass)
            throws URISyntaxException, IOException, CommunicationException {
        return executeGetRequest(path, null, trustAnySSLCertificate, responseClass, errorClass);
    }

    public static <T, U, E> T executePostRequest(String path, Map<String, String> requestParameters, boolean trustAnySSLCertificate, U request, Class<T> responseClass, Class<E> errorClass)
            throws IOException, CommunicationException, URISyntaxException {
        String maskedPath = path;
        logger.debug("Http Post request url: [{}]", maskedPath);
        StringEntity stringEntity = null;
        if (request != null) stringEntity = new StringEntity(objectMapper.writeValueAsString(request));
        HttpRequestBase requestBase = getRequestPost(path, requestParameters, stringEntity);
        return executeRequest(requestBase, trustAnySSLCertificate, responseClass, errorClass);
    }

    public static <T, U, E> T executePostRequest(String path, boolean trustAnySSLCertificate, U request, Class<T> responseClass, Class<E> errorClass)
            throws IOException, CommunicationException, URISyntaxException {
        return executePostRequest(path, null, trustAnySSLCertificate, request, responseClass, errorClass);
    }

    public static <T, U, E> T executePutRequest(String path, Map<String, String> requestParameters, boolean trustAnySSLCertificate, U request, Class<T> responseClass, Class<E> errorClass)
            throws IOException, CommunicationException, URISyntaxException {
        String maskedPath = path;
        logger.debug("Http Put request url: [{}]", maskedPath);
        StringEntity stringEntity = null;
        if (request != null) stringEntity = new StringEntity(objectMapper.writeValueAsString(request));
        HttpRequestBase requestBase = getRequestPut(path, requestParameters, stringEntity);
        return executeRequest(requestBase, trustAnySSLCertificate, responseClass, errorClass);
    }

    public static <T, U, E> T executePutRequest(String path, boolean trustAnySSLCertificate, U request, Class<T> responseClass, Class<E> errorClass)
            throws IOException, CommunicationException, URISyntaxException {
        return executePutRequest(path, null, trustAnySSLCertificate, request, responseClass, errorClass);
    }

    private static <T, E> T executeRequest(HttpRequestBase request, boolean trustAnySSLCertificate, Class<T> responseClass, Class<E> errorClass)
            throws IOException, CommunicationException {

        try (CloseableHttpClient client = getClient(request, trustAnySSLCertificate)) {
            printRequest(request);


            HttpClientContext context = HttpClientContext.create();
            AuthCache authCache = null;
            if (authCache != null) {
                context.setAuthCache(authCache);
            }

            CredentialsProvider credentialsProvider = null;
            if (credentialsProvider != null) {
                context.setCredentialsProvider(credentialsProvider);
            }

            HttpResponse response = client.execute(request, context);

            logger.debug("Response [{}].", response);
            validateResponseCodeIsOk(response, request.getURI(), errorClass);

            InputStream responseContent = response.getEntity().getContent();
            byte[] responseContentBytes = IOUtils.toByteArray(responseContent);
            String responseContentString = new String(responseContentBytes, StandardCharsets.UTF_8);
            logger.debug("Response Content [{}].", responseContentString);
            T responseObject = null;
            if (responseContentString != null && !responseContentString.isEmpty()) {
                responseObject = objectMapper.readValue(responseContentString, responseClass);
            } else {
                logger.debug("Execute Request: Response class is null. Returning null response object...");
            }
            return responseObject;
        }
    }

    private static void printRequest(HttpRequestBase httpRequest) {
        String maskedURI = httpRequest.getURI().toString();
        logger.debug("Request [{}] to [{}].", httpRequest.getMethod(), maskedURI);

        Header[] headers = httpRequest.getAllHeaders();
        for (Header header : headers) {
            logger.debug("Header: [{}]: [{}].", header.getName(), header.getValue());
        }

    }

    private static CloseableHttpClient getClient(HttpRequestBase httpRequest, boolean trustAnySSLCertificate) throws IOException {
        CloseableHttpClient client = null;

        HttpClientBuilder builderClient = HttpClients.custom().useSystemProperties().setDefaultRequestConfig(setTimeoutToAPICall());

        if (trustAnySSLCertificate) {
            builderClient.setSSLSocketFactory(getSSLConnectionSocketWithTrustAnySSLCertificate());
        }

        client = builderClient.setRedirectStrategy(new LaxRedirectStrategy()).build();

        return client;
    }

    private static SSLConnectionSocketFactory getSSLConnectionSocketWithTrustAnySSLCertificate() throws IOException {
        try {
            logger.debug("Setting Trust Any SSL Certificate...");
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            return new SSLConnectionSocketFactory(sslContextBuilder.build(), NoopHostnameVerifier.INSTANCE);

        } catch (Exception e) {
            logger.error("Error while setting trust any ssl certificate. '{}'", e.getLocalizedMessage(), e);
            throw new IOException("Error while setting trust any ssl certificate");
        }
    }

    private static void addCrumbsToRequest(HttpRequestBase request) {
        request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
    }

    private static <E> void validateResponseCodeIsOk(HttpResponse response, URI uri, Class<E> errorClass) throws CommunicationException {
        int responseStatusCode = response.getStatusLine().getStatusCode();
        if (responseStatusCode == HttpStatus.SC_UNAUTHORIZED) {
            logger.error("Invalid Credentials. Status Code:[{}].", responseStatusCode);
            throw new CommunicationException("UNAUTHORIZED" + response.getStatusLine());
        }
        if (!isHttpStatusOK(responseStatusCode)) {
            String detailedErrorMessage = parseErrorResponse(response, uri, errorClass);
            logger.error("Validate Response Code Is Ok: Detailed Error Message [{}].", detailedErrorMessage);
            throw new CommunicationException(detailedErrorMessage);
        }
    }

    private static <E> String parseErrorResponse(HttpResponse response, URI uri, Class<E> errorClass) {
        String detailedErrorMessage = null;
        E errorMessages = null;
        ObjectMapper objectMapper = new ObjectMapper();
        StatusLine statusLine = response.getStatusLine();
        String redirectLocation = null;
        try {
            String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            Header locationHeader = response.getFirstHeader("Location");
            if (locationHeader != null) {
                redirectLocation = locationHeader.getValue();
            }

            if (StringUtils.isNotEmpty(responseContent)) {
                errorMessages = objectMapper.readValue(responseContent, errorClass);
            } else {
                logger.error("Empty Response Content.");
            }

            detailedErrorMessage = String.format("Failed to communicate with server [%s] [%s] [%s] [%s] [%s].",
                    uri.toASCIIString(),
                    "http status [" + statusLine.getStatusCode() + "] " + statusLine.getReasonPhrase(),
                    responseContent,
                    redirectLocation,
                    errorMessages.toString());

            logger.error("Detailed Error Message: [{}].", detailedErrorMessage);

        } catch (IOException ioe) {
            logger.error("Error while parsing Error Message. Reason: [{}] [{}].", ioe.getMessage(), ioe);
        }
        return errorMessages != null && errorMessages.toString() != null && !errorMessages.toString().isEmpty()
                ? errorMessages.toString()
                : detailedErrorMessage;
    }

    private static boolean isHttpStatusOK(int httpStatus) {
        return httpStatus == HttpStatus.SC_OK ||
                httpStatus == HttpStatus.SC_CREATED ||
                httpStatus == HttpStatus.SC_ACCEPTED;
    }

    private static RequestConfig setTimeoutToAPICall() {
        int connectionTimeout = getConnectionTimeout();
        int readTimeout = getReadTimeout();

        logger.debug("Connection timeout set to: [{}] second(s)", connectionTimeout);
        logger.debug("Read timeout set to: [{}] second(s)", readTimeout);
        return RequestConfig.custom().setConnectTimeout(connectionTimeout * 1000)
                .setConnectionRequestTimeout(readTimeout * 1000).build();
    }

    private static int getIntProperty(String propertyName) {
        return 60;
    }

    private static int getReadTimeout() {
        int readTimeOut = getIntProperty("cdd.plugins.servicevirtualization.client.http.read.timeout");
        return readTimeOut != 0 ? readTimeOut : DEFAULT_READ_TIMEOUT;
    }

    private static int getConnectionTimeout() {
        int connectionTimeOut = getIntProperty("cdd.plugins.servicevirtualization.client.http.connection.timeout");
        return connectionTimeOut != 0 ? connectionTimeOut : DEFAULT_CONNECTION_TIMEOUT;
    }

    private static HttpRequestBase getRequestGet(String urlPath, Map<String, String> requestParameters)
            throws URISyntaxException {
        String urlWithParameters = addUrlParameters(urlPath, requestParameters);
        HttpGet httpGet = new HttpGet(urlWithParameters);
        addCrumbsToRequest(httpGet);
        return httpGet;
    }

    private static HttpRequestBase getRequestPost(String urlPath, Map<String, String> requestParameters, HttpEntity entity) throws URISyntaxException {
        String urlWithParameters = addUrlParameters(urlPath, requestParameters);
        HttpPost request = new HttpPost(urlWithParameters);
        if (entity != null) request.setEntity(entity);
        addCrumbsToRequest(request);
        return request;
    }

    private static HttpRequestBase getRequestPut(String urlPath, Map<String, String> requestParameters, HttpEntity entity) throws URISyntaxException {
        String urlWithParameters = addUrlParameters(urlPath, requestParameters);
        HttpPut request = new HttpPut(urlWithParameters);
        if (entity != null) request.setEntity(entity);
        addCrumbsToRequest(request);
        return request;
    }

    private static String addUrlParameters(String url, Map<String, String> urlParameters) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(url);
        if (urlParameters != null) {
            urlParameters.forEach(builder::setParameter);
        }
        return builder.build().toASCIIString();
    }

    private static <T, E> T executeGetRequest(String path,
                                              Map<String, String> requestParameters, boolean trustAnySSLCertificate, Class<T> responseClass, Class<E> errorClass)
            throws URISyntaxException, IOException, CommunicationException {
        logger.debug("Http Get request url: [{}]", path);
        HttpRequestBase request = getRequestGet(path, requestParameters);
        return executeRequest(request, trustAnySSLCertificate, responseClass, errorClass);
    }
}
