package com.example.smart_todo;

import android.util.Log;
import okhttp3.*;
import com.google.gson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;

/**
 * MongoDB Task Manager - handles all database operations for tasks using Atlas Data API
 */
public class MongoDBTaskManager {
    private static final String TAG = "MongoDBTaskManager";
    private static MongoDBTaskManager instance;
    private ExecutorService executorService;
    private Gson gson;
    
    private MongoDBTaskManager() {
        executorService = Executors.newFixedThreadPool(3);
        gson = MongoDBConfig.getGson();
    }
    
    public static synchronized MongoDBTaskManager getInstance() {
        if (instance == null) {
            instance = new MongoDBTaskManager();
        }
        return instance;
    }
    
    /**
     * Convert Task object to JsonObject for MongoDB Atlas API
     */
    private JsonObject taskToJsonObject(Task task) {
        JsonObject doc = new JsonObject();
        doc.addProperty("id", task.getId());
        doc.addProperty("name", task.getName());
        doc.addProperty("description", task.getDescription());
        doc.addProperty("category", task.getCategory());
        doc.addProperty("time", task.getTime());
        
        // Convert dates to ISO string format
        if (task.getCreatedAt() != null) {
            doc.addProperty("createdAt", task.getCreatedAt().getTime());
        }
        if (task.getDueDate() != null) {
            doc.addProperty("dueDate", task.getDueDate().getTime());
        }
        
        doc.addProperty("completed", task.isCompleted());
        doc.addProperty("important", task.isImportant());
        doc.addProperty("priority", task.getPriority());
        
        return doc;
    }
    
    /**
     * Convert JsonObject from MongoDB to Task object
     */
    private Task jsonObjectToTask(JsonObject doc) {
        Task task = new Task();
        
        if (doc.has("name") && !doc.get("name").isJsonNull()) {
            task.setName(doc.get("name").getAsString());
        }
        if (doc.has("description") && !doc.get("description").isJsonNull()) {
            task.setDescription(doc.get("description").getAsString());
        }
        if (doc.has("category") && !doc.get("category").isJsonNull()) {
            task.setCategory(doc.get("category").getAsString());
        }
        if (doc.has("time") && !doc.get("time").isJsonNull()) {
            task.setTime(doc.get("time").getAsString());
        }
        
        task.setCompleted(doc.has("completed") && doc.get("completed").getAsBoolean());
        task.setImportant(doc.has("important") && doc.get("important").getAsBoolean());
        
        if (doc.has("priority") && !doc.get("priority").isJsonNull()) {
            task.setPriority(doc.get("priority").getAsInt());
        }
        
        // Handle dates
        if (doc.has("dueDate") && !doc.get("dueDate").isJsonNull()) {
            long timestamp = doc.get("dueDate").getAsLong();
            task.setDueDate(new Date(timestamp));
        }
        
        // Set ID and createdAt using reflection (as before)
        try {
            if (doc.has("id") && !doc.get("id").isJsonNull()) {
                java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(task, doc.get("id").getAsString());
            }
            
            if (doc.has("createdAt") && !doc.get("createdAt").isJsonNull()) {
                java.lang.reflect.Field createdAtField = Task.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                long timestamp = doc.get("createdAt").getAsLong();
                createdAtField.set(task, new Date(timestamp));
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set ID or createdAt fields via reflection", e);
        }
        
        return task;
    }
    
    /**
     * Save a task to MongoDB Atlas
     */
    public void saveTask(Task task, TaskOperationCallback callback) {
        if (!MongoDBConfig.isConfigured()) {
            Log.w(TAG, "MongoDB Atlas not configured - skipping save operation");
            if (callback != null) callback.onError("MongoDB not configured");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JsonObject taskDoc = taskToJsonObject(task);
                Request request = MongoDBConfig.createInsertRequest(taskDoc);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (result.has("insertedId")) {
                        Log.d(TAG, "Task saved successfully: " + task.getName());
                        if (callback != null) callback.onSuccess("Task saved successfully");
                    } else {
                        Log.e(TAG, "Failed to save task: " + task.getName());
                        if (callback != null) callback.onError("Failed to save task");
                    }
                } else {
                    Log.e(TAG, "HTTP error saving task: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    Log.e(TAG, "Error response: " + errorBody);
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error saving task", e);
                if (callback != null) callback.onError("Error saving task: " + e.getMessage());
            }
        });
    }
    
    /**
     * Update an existing task in MongoDB Atlas
     */
    public void updateTask(Task task, TaskOperationCallback callback) {
        if (!MongoDBConfig.isConfigured()) {
            Log.w(TAG, "MongoDB Atlas not configured - skipping update operation");
            if (callback != null) callback.onError("MongoDB not configured");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JsonObject filter = new JsonObject();
                filter.addProperty("id", task.getId());
                
                JsonObject updateDoc = new JsonObject();
                JsonObject setDoc = taskToJsonObject(task);
                updateDoc.add("$set", setDoc);
                
                Request request = MongoDBConfig.createUpdateRequest(filter, updateDoc);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (result.has("modifiedCount") && result.get("modifiedCount").getAsInt() > 0) {
                        Log.d(TAG, "Task updated successfully: " + task.getName());
                        if (callback != null) callback.onSuccess("Task updated successfully");
                    } else {
                        Log.w(TAG, "No task found to update with ID: " + task.getId());
                        if (callback != null) callback.onError("Task not found");
                    }
                } else {
                    Log.e(TAG, "HTTP error updating task: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    Log.e(TAG, "Error response: " + errorBody);
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error updating task", e);
                if (callback != null) callback.onError("Error updating task: " + e.getMessage());
            }
        });
    }
    
    /**
     * Delete a task from MongoDB Atlas
     */
    public void deleteTask(String taskId, TaskOperationCallback callback) {
        if (!MongoDBConfig.isConfigured()) {
            Log.w(TAG, "MongoDB Atlas not configured - skipping delete operation");
            if (callback != null) callback.onError("MongoDB not configured");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JsonObject filter = new JsonObject();
                filter.addProperty("id", taskId);
                
                Request request = MongoDBConfig.createDeleteRequest(filter);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (result.has("deletedCount") && result.get("deletedCount").getAsInt() > 0) {
                        Log.d(TAG, "Task deleted successfully: " + taskId);
                        if (callback != null) callback.onSuccess("Task deleted successfully");
                    } else {
                        Log.w(TAG, "No task found to delete with ID: " + taskId);
                        if (callback != null) callback.onError("Task not found");
                    }
                } else {
                    Log.e(TAG, "HTTP error deleting task: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    Log.e(TAG, "Error response: " + errorBody);
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting task", e);
                if (callback != null) callback.onError("Error deleting task: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get all tasks from MongoDB Atlas
     */
    public void getAllTasks(TaskListCallback callback) {
        if (!MongoDBConfig.isConfigured()) {
            Log.w(TAG, "MongoDB Atlas not configured - returning empty list");
            if (callback != null) callback.onSuccess(new ArrayList<>());
            return;
        }
        
        executorService.execute(() -> {
            try {
                JsonObject filter = new JsonObject(); // Empty filter to get all documents
                Request request = MongoDBConfig.createFindRequest(filter);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    List<Task> tasks = new ArrayList<>();
                    
                    if (result.has("documents")) {
                        JsonArray documents = result.getAsJsonArray("documents");
                        for (JsonElement element : documents) {
                            JsonObject doc = element.getAsJsonObject();
                            Task task = jsonObjectToTask(doc);
                            tasks.add(task);
                        }
                    }
                    
                    Log.d(TAG, "Retrieved " + tasks.size() + " tasks from MongoDB Atlas");
                    if (callback != null) callback.onSuccess(tasks);
                } else {
                    Log.e(TAG, "HTTP error retrieving tasks: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    Log.e(TAG, "Error response: " + errorBody);
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving tasks", e);
                if (callback != null) callback.onError("Error retrieving tasks: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get tasks by completion status
     */
    public void getTasksByStatus(boolean completed, TaskListCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject filter = new JsonObject();
                filter.addProperty("completed", completed);
                
                Request request = MongoDBConfig.createFindRequest(filter);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    List<Task> tasks = new ArrayList<>();
                    
                    if (result.has("documents")) {
                        JsonArray documents = result.getAsJsonArray("documents");
                        for (JsonElement element : documents) {
                            JsonObject doc = element.getAsJsonObject();
                            Task task = jsonObjectToTask(doc);
                            tasks.add(task);
                        }
                    }
                    
                    Log.d(TAG, "Retrieved " + tasks.size() + " " + (completed ? "completed" : "pending") + " tasks");
                    if (callback != null) callback.onSuccess(tasks);
                } else {
                    Log.e(TAG, "HTTP error retrieving tasks by status: " + response.code());
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving tasks by status", e);
                if (callback != null) callback.onError("Error retrieving tasks: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get important tasks
     */
    public void getImportantTasks(TaskListCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject filter = new JsonObject();
                filter.addProperty("important", true);
                
                Request request = MongoDBConfig.createFindRequest(filter);
                
                Response response = MongoDBConfig.executeRequest(request);
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    List<Task> tasks = new ArrayList<>();
                    
                    if (result.has("documents")) {
                        JsonArray documents = result.getAsJsonArray("documents");
                        for (JsonElement element : documents) {
                            JsonObject doc = element.getAsJsonObject();
                            Task task = jsonObjectToTask(doc);
                            tasks.add(task);
                        }
                    }
                    
                    Log.d(TAG, "Retrieved " + tasks.size() + " important tasks");
                    if (callback != null) callback.onSuccess(tasks);
                } else {
                    Log.e(TAG, "HTTP error retrieving important tasks: " + response.code());
                    if (callback != null) callback.onError("HTTP error: " + response.code());
                }
                
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving important tasks", e);
                if (callback != null) callback.onError("Error retrieving important tasks: " + e.getMessage());
            }
        });
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Callback interface for single task operations
     */
    public interface TaskOperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    /**
     * Callback interface for task list operations
     */
    public interface TaskListCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }
} 