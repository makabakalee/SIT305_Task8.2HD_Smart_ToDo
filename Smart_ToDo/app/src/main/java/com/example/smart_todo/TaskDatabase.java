package com.example.smart_todo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDatabase {
    private static final String TAG = "TaskDatabase";
    private static final String PREF_NAME = "smart_todo_prefs";
    private static final String KEY_TASKS = "tasks";
    
    private final SharedPreferences sharedPreferences;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public TaskDatabase(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveTasks(List<Task> tasks) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Task task : tasks) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", task.getId());
                jsonObject.put("name", task.getName());
                jsonObject.put("description", task.getDescription());
                jsonObject.put("category", task.getCategory());
                jsonObject.put("time", task.getTime());
                jsonObject.put("createdAt", dateFormat.format(task.getCreatedAt()));
                jsonObject.put("completed", task.isCompleted());
                jsonObject.put("important", task.isImportant());
                jsonObject.put("priority", task.getPriority());
                
                jsonArray.put(jsonObject);
            }
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_TASKS, jsonArray.toString());
            editor.apply();
            
            Log.d(TAG, "Saved " + tasks.size() + " tasks to local storage");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving tasks", e);
        }
    }
    
    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        String tasksJson = sharedPreferences.getString(KEY_TASKS, "[]");
        
        try {
            JSONArray jsonArray = new JSONArray(tasksJson);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                Task task = new Task();
                if (jsonObject.has("id")) {
                    try {
                        java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(task, jsonObject.getString("id"));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to set task ID", e);
                    }
                }
                
                task.setName(jsonObject.getString("name"));
                task.setCategory(jsonObject.getString("category"));
                task.setTime(jsonObject.getString("time"));
                task.setCompleted(jsonObject.getBoolean("completed"));
                
                if (jsonObject.has("description")) {
                    task.setDescription(jsonObject.getString("description"));
                }
                if (jsonObject.has("important")) {
                    task.setImportant(jsonObject.getBoolean("important"));
                }
                if (jsonObject.has("priority")) {
                    task.setPriority(jsonObject.getInt("priority"));
                }
                
                tasks.add(task);
            }
            
            Log.d(TAG, "Loaded " + tasks.size() + " tasks from local storage");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading tasks", e);
        }
        
        return tasks;
    }
    
    public void clearTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TASKS);
        editor.apply();
        Log.d(TAG, "Cleared all tasks from local storage");
    }
} 