package com.example.smart_todo;

import android.util.Log;
import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB Atlas Data API configuration and connection manager
 * Uses REST API instead of native MongoDB driver for better Android compatibility
 */
public class MongoDBConfig {
    private static final String TAG = "MongoDBConfig";
    
    // MongoDB Atlas Data API configuration
    private static final String CLUSTER_NAME = "Cluster0";
    private static final String DATABASE_NAME = "smart_todo_db";
    private static final String COLLECTION_NAME = "tasks";
    
    // IMPORTANT: Configure these values from your MongoDB Atlas console
    // 1. Go to https://cloud.mongodb.com/
    // 2. Select your project → Data API
    // 3. Enable Data API and copy the App ID
    // 4. Create an API Key and copy it
    private static final String APP_ID = ""; // Leave empty to disable MongoDB temporarily
    private static final String API_KEY = ""; // Your MongoDB Atlas API Key
    
    private static final String BASE_URL = "https://data.mongodb-api.com/app/" + APP_ID + "/endpoint/data/v1";
    
    private static OkHttpClient httpClient;
    private static Gson gson;
    
    static {
        initialize();
    }
    
    /**
     * Check if MongoDB is properly configured
     */
    public static boolean isConfigured() {
        return !APP_ID.isEmpty() && !API_KEY.isEmpty();
    }
    
    /**
     * Initialize HTTP client and JSON parser
     */
    public static synchronized void initialize() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            gson = new Gson();
            
            if (isConfigured()) {
                Log.d(TAG, "MongoDB Atlas Data API client initialized");
            } else {
                Log.w(TAG, "MongoDB Atlas not configured - APP_ID and API_KEY are empty");
            }
        }
    }
    
    /**
     * Create base request builder with common headers
     */
    private static Request.Builder createBaseRequest() {
        return new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Access-Control-Request-Headers", "*")
                .addHeader("api-key", API_KEY);
    }
    
    /**
     * Create base request body with database and collection info
     */
    private static JsonObject createBaseRequestBody() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("cluster", CLUSTER_NAME);
        requestBody.addProperty("database", DATABASE_NAME);
        requestBody.addProperty("collection", COLLECTION_NAME);
        return requestBody;
    }
    
    /**
     * Execute HTTP request synchronously
     */
    public static Response executeRequest(Request request) throws IOException {
        if (!isConfigured()) {
            throw new IOException("MongoDB Atlas not configured. Please set APP_ID and API_KEY in MongoDBConfig.java");
        }
        return httpClient.newCall(request).execute();
    }
    
    /**
     * Execute HTTP request asynchronously
     */
    public static void executeRequestAsync(Request request, Callback callback) {
        if (!isConfigured()) {
            callback.onFailure(null, new IOException("MongoDB Atlas not configured"));
            return;
        }
        httpClient.newCall(request).enqueue(callback);
    }
    
    /**
     * Get HTTP client instance
     */
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            initialize();
        }
        return httpClient;
    }
    
    /**
     * Get Gson instance
     */
    public static Gson getGson() {
        if (gson == null) {
            initialize();
        }
        return gson;
    }
    
    /**
     * Get base URL for MongoDB Atlas Data API
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    /**
     * Get database name
     */
    public static String getDatabaseName() {
        return DATABASE_NAME;
    }
    
    /**
     * Get collection name
     */
    public static String getCollectionName() {
        return COLLECTION_NAME;
    }
    
    /**
     * Create request for finding documents
     */
    public static Request createFindRequest(JsonObject filter) {
        JsonObject requestBody = createBaseRequestBody();
        if (filter != null) {
            requestBody.add("filter", filter);
        }
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );
        
        return createBaseRequest()
                .url(BASE_URL + "/action/find")
                .post(body)
                .build();
    }
    
    /**
     * Create request for inserting a document
     */
    public static Request createInsertRequest(JsonObject document) {
        JsonObject requestBody = createBaseRequestBody();
        requestBody.add("document", document);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );
        
        return createBaseRequest()
                .url(BASE_URL + "/action/insertOne")
                .post(body)
                .build();
    }
    
    /**
     * Create request for updating a document
     */
    public static Request createUpdateRequest(JsonObject filter, JsonObject update) {
        JsonObject requestBody = createBaseRequestBody();
        requestBody.add("filter", filter);
        requestBody.add("update", update);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );
        
        return createBaseRequest()
                .url(BASE_URL + "/action/updateOne")
                .post(body)
                .build();
    }
    
    /**
     * Create request for deleting a document
     */
    public static Request createDeleteRequest(JsonObject filter) {
        JsonObject requestBody = createBaseRequestBody();
        requestBody.add("filter", filter);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );
        
        return createBaseRequest()
                .url(BASE_URL + "/action/deleteOne")
                .post(body)
                .build();
    }
    
    /**
     * Test the connection to MongoDB Atlas Data API
     */
    public static boolean testConnection() {
        if (!isConfigured()) {
            Log.w(TAG, "MongoDB Atlas not configured - skipping connection test");
            return false;
        }
        
        try {
            JsonObject filter = new JsonObject();
            Request request = createFindRequest(filter);
            
            Response response = executeRequest(request);
            boolean isSuccess = response.isSuccessful();
            
            if (isSuccess) {
                Log.d(TAG, "MongoDB Atlas Data API connection test successful");
            } else {
                Log.e(TAG, "MongoDB Atlas Data API connection test failed with code: " + response.code());
                if (response.body() != null) {
                    Log.e(TAG, "Response body: " + response.body().string());
                }
            }
            
            response.close();
            return isSuccess;
        } catch (Exception e) {
            Log.e(TAG, "MongoDB Atlas Data API connection test failed", e);
            return false;
        }
    }
    
    /**
     * Close resources (if needed)
     */
    public static void close() {
        // OkHttpClient will clean up automatically
        Log.d(TAG, "MongoDB Atlas Data API client resources released");
    }
    
    /**
     * Log configuration status for debugging
     */
    public static void logConfigurationStatus() {
        Log.d(TAG, "=== MongoDB Atlas Configuration Status ===");
        Log.d(TAG, "APP_ID configured: " + (!APP_ID.isEmpty()));
        Log.d(TAG, "API_KEY configured: " + (!API_KEY.isEmpty()));
        Log.d(TAG, "Base URL: " + BASE_URL);
        Log.d(TAG, "Database: " + DATABASE_NAME);
        Log.d(TAG, "Collection: " + COLLECTION_NAME);
        Log.d(TAG, "Is properly configured: " + isConfigured());
        Log.d(TAG, "==========================================");
    }
} 