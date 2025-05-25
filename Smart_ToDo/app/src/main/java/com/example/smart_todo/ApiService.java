package com.example.smart_todo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final String TAG = "ApiService";
    private static final String DEFAULT_API_URL = "http://10.0.2.2:5000/chat"; // 10.0.2.2 is localhost from Android emulator
    private static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain; charset=utf-8");
    
    private OkHttpClient client;
    private String apiUrl;
    
    public ApiService() {
        this(DEFAULT_API_URL);
    }
    
    public ApiService(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            this.apiUrl = DEFAULT_API_URL;
        } else {
            this.apiUrl = serverUrl;
        }
        
        Log.d(TAG, "ApiService initialized with URL: " + this.apiUrl);
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public interface ApiCallback {
        void onSuccess(Task task);
        void onFailure(String errorMessage);
    }
    
    public void processTask(String userInput, ApiCallback callback) {
        // Extract original time information from user input before sending to API
        String timeFromInput = extractTimeFromInput(userInput);
        
        RequestBody body = RequestBody.create(userInput, MEDIA_TYPE_TEXT);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                callback.onFailure("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API response: " + responseBody);
                    
                    // Check if response is JSON
                    if (responseBody.trim().startsWith("{")) {
                        try {
                            Task task = parseJsonResponse(responseBody, userInput);
                            
                            // Override time if needed
                            if (!timeFromInput.isEmpty() && task.getTime().equalsIgnoreCase("Anytime")) {
                                task.setTime(timeFromInput);
                                Log.d(TAG, "Overriding time with: " + timeFromInput);
                            }
                            
                            callback.onSuccess(task);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error", e);
                            Task task = parsePlainTextResponse(responseBody, userInput);
                            
                            // Override time if needed
                            if (!timeFromInput.isEmpty() && task.getTime().equalsIgnoreCase("Anytime")) {
                                task.setTime(timeFromInput);
                                Log.d(TAG, "Overriding time with: " + timeFromInput);
                            }
                            
                            callback.onSuccess(task);
                        }
                    } else {
                        // Treat as plain text
                        Task task = parsePlainTextResponse(responseBody, userInput);
                        
                        // Override time if needed
                        if (!timeFromInput.isEmpty() && task.getTime().equalsIgnoreCase("Anytime")) {
                            task.setTime(timeFromInput);
                            Log.d(TAG, "Overriding time with: " + timeFromInput);
                        }
                        
                        callback.onSuccess(task);
                    }
                } else {
                    callback.onFailure("Server error: " + response.code());
                }
            }
        });
    }
    
    private Task parseJsonResponse(String response, String userInput) throws JSONException {
        // Try to clean up the response if it contains extra text around the JSON
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}') + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                response = response.substring(jsonStart, jsonEnd);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error cleaning JSON: " + e.getMessage());
            // Continue with original response
        }
        
        JSONObject jsonObject = new JSONObject(response);
        
        String taskName = jsonObject.optString("task", userInput);
        String category = jsonObject.optString("category", "Personal");
        String time = jsonObject.optString("time", "Anytime");
        
        // Ensure we have valid values
        if (taskName.isEmpty()) taskName = userInput;
        if (category.isEmpty()) category = "Personal";
        if (time.isEmpty()) time = "Anytime";
        
        // Validate category to ensure it's one of our expected values
        String[] validCategories = {"Work", "Personal", "Study", "Shopping", "Health", "Other"};
        boolean isValidCategory = false;
        for (String validCategory : validCategories) {
            if (validCategory.equalsIgnoreCase(category)) {
                category = validCategory; // Use the proper case
                isValidCategory = true;
                break;
            }
        }
        
        if (!isValidCategory) {
            category = "Personal"; // Default to Personal if category is not valid
        }
        
        Log.d(TAG, "Parsed JSON: Task=" + taskName + ", Category=" + category + ", Time=" + time);
        
        // Create task with extracted information
        Task task = new Task(taskName, category, time);
        
        // Parse priority from category or time
        if (category.equalsIgnoreCase("Work") || 
            time.toLowerCase().contains("urgent") || 
            time.toLowerCase().contains("important")) {
            task.setPriority(2); // High priority
            task.setImportant(true);
        } else if (time.toLowerCase().contains("today") || 
                   time.toLowerCase().contains("tomorrow")) {
            task.setPriority(1); // Medium priority
        }
        
        return task;
    }
    
    private Task parsePlainTextResponse(String response, String userInput) {
        Log.d(TAG, "Parsing plain text response");
        
        // Look for JSON-like patterns in the text
        try {
            if (response.contains("{") && response.contains("}")) {
                String jsonCandidate = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
                return parseJsonResponse(jsonCandidate, userInput);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to extract JSON from text: " + e.getMessage());
        }
        
        // Simple parsing logic for plain text
        String taskName = userInput;
        String category = "Personal";
        String time = "Anytime";
        int priority = 0;
        boolean important = false;
        
        // Try to extract task name if the model attempted to provide one
        if (response.toLowerCase().contains("task:")) {
            int taskIndex = response.toLowerCase().indexOf("task:");
            int endIndex = response.indexOf("\n", taskIndex);
            if (endIndex == -1) {
                endIndex = response.indexOf(".", taskIndex);
            }
            if (endIndex == -1) {
                endIndex = response.length();
            }
            
            if (taskIndex >= 0 && endIndex > taskIndex + 5) {
                String extractedTask = response.substring(taskIndex + 5, endIndex).trim();
                if (!extractedTask.isEmpty()) {
                    taskName = extractedTask;
                }
            }
        }
        
        // Extract category information (case insensitive)
        String lowerResponse = response.toLowerCase();
        
        // Look for category labels
        if (lowerResponse.contains("category:") || lowerResponse.contains("type:")) {
            String[] categoryMarkers = {"category:", "type:"};
            for (String marker : categoryMarkers) {
                if (lowerResponse.contains(marker)) {
                    int categoryIndex = lowerResponse.indexOf(marker);
                    int endIndex = lowerResponse.indexOf("\n", categoryIndex);
                    if (endIndex == -1) {
                        endIndex = lowerResponse.indexOf(".", categoryIndex);
                    }
                    if (endIndex == -1) {
                        endIndex = response.length();
                    }
                    
                    if (categoryIndex >= 0 && endIndex > categoryIndex + marker.length()) {
                        String extractedCategory = response.substring(categoryIndex + marker.length(), endIndex).trim().toLowerCase();
                        
                        if (extractedCategory.contains("work")) {
                            category = "Work";
                            priority = Math.max(priority, 1);
                        } else if (extractedCategory.contains("study")) {
                            category = "Study";
                            priority = Math.max(priority, 1);
                        } else if (extractedCategory.contains("shop")) {
                            category = "Shopping";
                        } else if (extractedCategory.contains("health")) {
                            category = "Health";
                            priority = Math.max(priority, 1);
                        } else if (extractedCategory.contains("personal")) {
                            category = "Personal";
                        } else if (extractedCategory.contains("other")) {
                            category = "Other";
                        }
                    }
                }
            }
        } else {
            // Fallback category detection
            if (lowerResponse.contains("work")) {
                category = "Work";
                priority = Math.max(priority, 1);
            } else if (lowerResponse.contains("study") || lowerResponse.contains("homework") || 
                      lowerResponse.contains("assignment") || lowerResponse.contains("class")) {
                category = "Study";
                priority = Math.max(priority, 1);
            } else if (lowerResponse.contains("shop") || lowerResponse.contains("buy") || 
                      lowerResponse.contains("purchase") || lowerResponse.contains("store")) {
                category = "Shopping";
            } else if (lowerResponse.contains("health") || lowerResponse.contains("doctor") || 
                      lowerResponse.contains("exercise") || lowerResponse.contains("medicine")) {
                category = "Health";
                priority = Math.max(priority, 1);
            }
        }
        
        // Extract time information (case insensitive)
        if (lowerResponse.contains("time:") || lowerResponse.contains("when:")) {
            String[] timeMarkers = {"time:", "when:"};
            for (String marker : timeMarkers) {
                if (lowerResponse.contains(marker)) {
                    int timeIndex = lowerResponse.indexOf(marker);
                    int endIndex = lowerResponse.indexOf("\n", timeIndex);
                    if (endIndex == -1) {
                        endIndex = lowerResponse.indexOf(".", timeIndex);
                    }
                    if (endIndex == -1) {
                        endIndex = response.length();
                    }
                    
                    if (timeIndex >= 0 && endIndex > timeIndex + marker.length()) {
                        String extractedTime = response.substring(timeIndex + marker.length(), endIndex).trim();
                        if (!extractedTime.isEmpty()) {
                            time = extractedTime;
                            
                            // Adjust priority based on extracted time
                            String lowerTime = time.toLowerCase();
                            if (lowerTime.contains("today") || lowerTime.contains("now")) {
                                priority = Math.max(priority, 1);
                            } else if (lowerTime.contains("tomorrow")) {
                                priority = Math.max(priority, 1);
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback time detection
            if (lowerResponse.contains("morning")) {
                time = "Morning";
            } else if (lowerResponse.contains("afternoon")) {
                time = "Afternoon";
            } else if (lowerResponse.contains("evening")) {
                time = "Evening";
            } else if (lowerResponse.contains("night")) {
                time = "Night";
            } else if (lowerResponse.contains("tomorrow")) {
                time = "Tomorrow";
                priority = Math.max(priority, 1);
            } else if (lowerResponse.contains("today")) {
                time = "Today";
                priority = Math.max(priority, 1);
            }
        }
        
        // Check for important/urgent markers
        if (lowerResponse.contains("important") || lowerResponse.contains("urgent") || 
            lowerResponse.contains("priority") || lowerResponse.contains("critical")) {
            priority = 2;
            important = true;
        }
        
        Log.d(TAG, "Parsed plain text: Task=" + taskName + ", Category=" + category + 
                   ", Time=" + time + ", Priority=" + priority);
        
        // Create and configure task
        Task task = new Task(taskName, category, time);
        task.setPriority(priority);
        task.setImportant(important);
        
        return task;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    // Extract time information directly from user input
    private String extractTimeFromInput(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // Check for explicit time indicators
        if (lowerInput.contains("tomorrow")) {
            return "Tomorrow";
        } else if (lowerInput.contains("today")) {
            return "Today";
        } else if (lowerInput.contains("morning")) {
            return "Morning";
        } else if (lowerInput.contains("afternoon")) {
            return "Afternoon";
        } else if (lowerInput.contains("evening")) {
            return "Evening";
        } else if (lowerInput.contains("night")) {
            return "Night";
        } else if (lowerInput.matches(".*\\d{1,2}(:|am|pm).*")) {
            // Extract time with regex for patterns like "3pm", "10:30am"
            // This is a simplified version - could be enhanced further
            return extractSpecificTime(lowerInput);
        }
        
        return ""; // No time found
    }
    
    // Extract specific time formats like "3pm", "10:30", etc.
    private String extractSpecificTime(String input) {
        // Very simple regex matching - could be improved with more comprehensive regex
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
                "\\d{1,2}(:\\d{1,2})?(\\s)?(am|pm)?", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = timePattern.matcher(input);
        
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return "";
    }
} 