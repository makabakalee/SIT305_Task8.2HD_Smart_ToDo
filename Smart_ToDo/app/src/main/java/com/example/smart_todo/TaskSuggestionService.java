package com.example.smart_todo;

import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Service for providing intelligent task suggestions and recommendations
 * Feature 3: Suggestions - gives advice about which task to do first
 */
public class TaskSuggestionService {
    private static final String TAG = "TaskSuggestionService";
    
    /**
     * Get the most important task to do next based on priority, deadline, and category
     * @param tasks List of all tasks
     * @return The recommended task to do first, or null if no suitable task found
     */
    public static Task getNextTaskSuggestion(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        
        // Filter only incomplete tasks
        List<Task> incompleteTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                incompleteTasks.add(task);
            }
        }
        
        if (incompleteTasks.isEmpty()) {
            return null;
        }
        
        // Sort tasks by priority algorithm
        Collections.sort(incompleteTasks, new TaskPriorityComparator());
        
        return incompleteTasks.get(0);
    }
    
    /**
     * Get multiple task suggestions with explanations
     * @param tasks List of all tasks
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of task suggestions with reasoning
     */
    public static List<TaskSuggestion> getTaskSuggestions(List<Task> tasks, int maxSuggestions) {
        List<TaskSuggestion> suggestions = new ArrayList<>();
        
        if (tasks == null || tasks.isEmpty()) {
            return suggestions;
        }
        
        // Filter incomplete tasks
        List<Task> incompleteTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                incompleteTasks.add(task);
            }
        }
        
        if (incompleteTasks.isEmpty()) {
            return suggestions;
        }
        
        // Sort and get top suggestions
        Collections.sort(incompleteTasks, new TaskPriorityComparator());
        
        int count = Math.min(maxSuggestions, incompleteTasks.size());
        for (int i = 0; i < count; i++) {
            Task task = incompleteTasks.get(i);
            String reason = getTaskPriorityReason(task);
            suggestions.add(new TaskSuggestion(task, reason, i + 1));
        }
        
        return suggestions;
    }
    
    /**
     * Get reason why a task is suggested
     * @param task The task to analyze
     * @return Human-readable explanation for the suggestion
     */
    private static String getTaskPriorityReason(Task task) {
        List<String> reasons = new ArrayList<>();
        
        // Check if marked as important
        if (task.isImportant()) {
            reasons.add("marked as important");
        }
        
        // Check priority level
        if (task.getPriority() == 2) {
            reasons.add("high priority");
        } else if (task.getPriority() == 1) {
            reasons.add("medium priority");
        }
        
        // Check time urgency
        String timeReason = getTimeUrgencyReason(task.getTime());
        if (!timeReason.isEmpty()) {
            reasons.add(timeReason);
        }
        
        // Check category importance
        if (task.getCategory().equals("Work")) {
            reasons.add("work-related task");
        } else if (task.getCategory().equals("Health")) {
            reasons.add("health-related task");
        } else if (task.getCategory().equals("Study")) {
            reasons.add("education-related task");
        }
        
        // Combine reasons
        if (reasons.isEmpty()) {
            return "general task completion";
        } else if (reasons.size() == 1) {
            return reasons.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i == reasons.size() - 1) {
                    sb.append(" and ").append(reasons.get(i));
                } else if (i == 0) {
                    sb.append(reasons.get(i));
                } else {
                    sb.append(", ").append(reasons.get(i));
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * Get urgency reason based on time
     * @param time The time string from task
     * @return Urgency explanation
     */
    private static String getTimeUrgencyReason(String time) {
        if (time == null) {
            return "";
        }
        
        String lowerTime = time.toLowerCase();
        
        if (lowerTime.contains("now") || lowerTime.contains("urgent")) {
            return "needs immediate attention";
        } else if (lowerTime.contains("today")) {
            return "due today";
        } else if (lowerTime.contains("tomorrow")) {
            return "due tomorrow";
        } else if (lowerTime.contains("morning")) {
            return "scheduled for morning";
        } else if (lowerTime.contains("afternoon")) {
            return "scheduled for afternoon";
        } else if (lowerTime.contains("evening")) {
            return "scheduled for evening";
        }
        
        return "";
    }
    
    /**
     * Comparator for sorting tasks by priority
     */
    private static class TaskPriorityComparator implements Comparator<Task> {
        @Override
        public int compare(Task task1, Task task2) {
            // Calculate priority scores
            int score1 = calculatePriorityScore(task1);
            int score2 = calculatePriorityScore(task2);
            
            // Higher score comes first
            return Integer.compare(score2, score1);
        }
        
        /**
         * Calculate numerical priority score for a task
         * Higher score = higher priority
         */
        private int calculatePriorityScore(Task task) {
            int score = 0;
            
            // Base priority points
            score += task.getPriority() * 10;
            
            // Important flag
            if (task.isImportant()) {
                score += 15;
            }
            
            // Time urgency
            String time = task.getTime();
            if (time != null) {
                String lowerTime = time.toLowerCase();
                if (lowerTime.contains("now") || lowerTime.contains("urgent")) {
                    score += 20;
                } else if (lowerTime.contains("today")) {
                    score += 15;
                } else if (lowerTime.contains("tomorrow")) {
                    score += 10;
                } else if (lowerTime.contains("morning") || lowerTime.contains("afternoon") || lowerTime.contains("evening")) {
                    score += 8;
                }
            }
            
            // Category importance
            String category = task.getCategory();
            if (category != null) {
                switch (category) {
                    case "Work":
                        score += 12;
                        break;
                    case "Health":
                        score += 10;
                        break;
                    case "Study":
                        score += 8;
                        break;
                    case "Shopping":
                        score += 5;
                        break;
                    case "Personal":
                        score += 3;
                        break;
                    default:
                        score += 1;
                        break;
                }
            }
            
            // Age of task (older tasks get slight priority boost)
            if (task.getCreatedAt() != null) {
                long ageInHours = (System.currentTimeMillis() - task.getCreatedAt().getTime()) / (1000 * 60 * 60);
                if (ageInHours > 24) {
                    score += Math.min(5, ageInHours / 24); // Max 5 bonus points for age
                }
            }
            
            return score;
        }
    }
    
    /**
     * Class to hold task suggestion with reasoning
     */
    public static class TaskSuggestion {
        private Task task;
        private String reason;
        private int rank;
        
        public TaskSuggestion(Task task, String reason, int rank) {
            this.task = task;
            this.reason = reason;
            this.rank = rank;
        }
        
        public Task getTask() {
            return task;
        }
        
        public String getReason() {
            return reason;
        }
        
        public int getRank() {
            return rank;
        }
        
        @Override
        public String toString() {
            return "#" + rank + ": " + task.getName() + " (Reason: " + reason + ")";
        }
    }
} 