package com.example.smart_todo;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Service for handling natural language queries about tasks
 * Feature 4: Ask questions - Users can ask questions and get clear answers
 */
public class TaskQueryService {
    private static final String TAG = "TaskQueryService";
    
    /**
     * Process natural language query and return appropriate response
     * @param query User's natural language question
     * @param tasks List of all tasks
     * @return Human-readable response to the query
     */
    public static String processQuery(String query, List<Task> tasks) {
        if (query == null || query.trim().isEmpty()) {
            return "Please ask me a question about your tasks.";
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Remove question words for easier parsing
        lowerQuery = lowerQuery.replaceAll("^(what|how|when|where|why|which)\\s+(are|is|do|did|will|can|should)\\s+", "");
        lowerQuery = lowerQuery.replaceAll("^(show|tell|list)\\s+(me)?\\s*", "");
        
        Log.d(TAG, "Processing query: " + lowerQuery);
        
        // Identify query type and respond accordingly
        if (isTaskCountQuery(lowerQuery)) {
            return handleTaskCountQuery(lowerQuery, tasks);
        } else if (isTodayTasksQuery(lowerQuery)) {
            return handleTodayTasksQuery(tasks);
        } else if (isTomorrowTasksQuery(lowerQuery)) {
            return handleTomorrowTasksQuery(tasks);
        } else if (isImportantTasksQuery(lowerQuery)) {
            return handleImportantTasksQuery(tasks);
        } else if (isCompletedTasksQuery(lowerQuery)) {
            return handleCompletedTasksQuery(tasks);
        } else if (isWorkTasksQuery(lowerQuery)) {
            return handleCategoryTasksQuery(tasks, "Work");
        } else if (isStudyTasksQuery(lowerQuery)) {
            return handleCategoryTasksQuery(tasks, "Study");
        } else if (isShoppingTasksQuery(lowerQuery)) {
            return handleCategoryTasksQuery(tasks, "Shopping");
        } else if (isSuggestionQuery(lowerQuery)) {
            return handleSuggestionQuery(tasks);
        } else if (isNextTaskQuery(lowerQuery)) {
            return handleNextTaskQuery(tasks);
        } else if (isStatusQuery(lowerQuery)) {
            return handleStatusQuery(tasks);
        } else {
            return handleGenericQuery(lowerQuery, tasks);
        }
    }
    
    // Query type detection methods
    private static boolean isTaskCountQuery(String query) {
        return query.contains("how many") || query.contains("count") || query.contains("number of");
    }
    
    private static boolean isTodayTasksQuery(String query) {
        return query.contains("today") || query.contains("this day");
    }
    
    private static boolean isTomorrowTasksQuery(String query) {
        return query.contains("tomorrow") || query.contains("next day");
    }
    
    private static boolean isImportantTasksQuery(String query) {
        return query.contains("important") || query.contains("priority") || query.contains("urgent");
    }
    
    private static boolean isCompletedTasksQuery(String query) {
        return query.contains("completed") || query.contains("finished") || query.contains("done");
    }
    
    private static boolean isWorkTasksQuery(String query) {
        return query.contains("work") || query.contains("job") || query.contains("office");
    }
    
    private static boolean isStudyTasksQuery(String query) {
        return query.contains("study") || query.contains("homework") || query.contains("assignment") || query.contains("school");
    }
    
    private static boolean isShoppingTasksQuery(String query) {
        return query.contains("shopping") || query.contains("buy") || query.contains("purchase") || query.contains("groceries");
    }
    
    private static boolean isSuggestionQuery(String query) {
        return query.contains("suggest") || query.contains("recommend") || query.contains("should i do") || 
               query.contains("what to do") || query.contains("which task");
    }
    
    private static boolean isNextTaskQuery(String query) {
        return query.contains("next") || query.contains("first") || query.contains("start with");
    }
    
    private static boolean isStatusQuery(String query) {
        return query.contains("status") || query.contains("summary") || query.contains("overview");
    }
    
    // Query handling methods
    private static String handleTaskCountQuery(String query, List<Task> tasks) {
        if (query.contains("completed") || query.contains("finished")) {
            int completedCount = 0;
            for (Task task : tasks) {
                if (task.isCompleted()) completedCount++;
            }
            return String.format("You have completed %d task%s.", completedCount, completedCount != 1 ? "s" : "");
        } else {
            int totalCount = tasks.size();
            int pendingCount = 0;
            for (Task task : tasks) {
                if (!task.isCompleted()) pendingCount++;
            }
            return String.format("You have %d total task%s, with %d pending.", 
                    totalCount, totalCount != 1 ? "s" : "", pendingCount);
        }
    }
    
    private static String handleTodayTasksQuery(List<Task> tasks) {
        List<Task> todayTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (!task.isCompleted() && task.isToday()) {
                todayTasks.add(task);
            }
        }
        
        if (todayTasks.isEmpty()) {
            return "You have no specific tasks scheduled for today. Consider checking your priority tasks!";
        }
        
        StringBuilder response = new StringBuilder("Your tasks for today:\n");
        for (int i = 0; i < todayTasks.size(); i++) {
            Task task = todayTasks.get(i);
            response.append(String.format("%d. %s (%s)%s\n", 
                    i + 1, 
                    task.getName(), 
                    task.getCategory(),
                    task.isImportant() ? " [Important]" : ""));
        }
        
        return response.toString().trim();
    }
    
    private static String handleTomorrowTasksQuery(List<Task> tasks) {
        List<Task> tomorrowTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (!task.isCompleted() && task.getTime() != null && 
                task.getTime().toLowerCase().contains("tomorrow")) {
                tomorrowTasks.add(task);
            }
        }
        
        if (tomorrowTasks.isEmpty()) {
            return "You have no specific tasks scheduled for tomorrow.";
        }
        
        StringBuilder response = new StringBuilder("Your tasks for tomorrow:\n");
        for (int i = 0; i < tomorrowTasks.size(); i++) {
            Task task = tomorrowTasks.get(i);
            response.append(String.format("%d. %s (%s)%s\n", 
                    i + 1, 
                    task.getName(), 
                    task.getCategory(),
                    task.isImportant() ? " [Important]" : ""));
        }
        
        return response.toString().trim();
    }
    
    private static String handleImportantTasksQuery(List<Task> tasks) {
        List<Task> importantTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (!task.isCompleted() && task.isImportant()) {
                importantTasks.add(task);
            }
        }
        
        if (importantTasks.isEmpty()) {
            return "You have no important tasks pending. Great job!";
        }
        
        StringBuilder response = new StringBuilder("Your important tasks:\n");
        for (int i = 0; i < importantTasks.size(); i++) {
            Task task = importantTasks.get(i);
            response.append(String.format("%d. %s (%s - %s)\n", 
                    i + 1, 
                    task.getName(), 
                    task.getCategory(),
                    task.getTime() != null ? task.getTime() : "Anytime"));
        }
        
        return response.toString().trim();
    }
    
    private static String handleCompletedTasksQuery(List<Task> tasks) {
        List<Task> completedTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            }
        }
        
        if (completedTasks.isEmpty()) {
            return "You haven't completed any tasks yet. Time to get started!";
        }
        
        StringBuilder response = new StringBuilder(String.format("You have completed %d task%s:\n", 
                completedTasks.size(), completedTasks.size() != 1 ? "s" : ""));
        
        int displayCount = Math.min(5, completedTasks.size()); // Show max 5 recent completed tasks
        for (int i = 0; i < displayCount; i++) {
            Task task = completedTasks.get(i);
            response.append(String.format("• %s (%s)\n", task.getName(), task.getCategory()));
        }
        
        if (completedTasks.size() > 5) {
            response.append(String.format("...and %d more completed tasks.", completedTasks.size() - 5));
        }
        
        return response.toString().trim();
    }
    
    private static String handleCategoryTasksQuery(List<Task> tasks, String category) {
        List<Task> categoryTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (!task.isCompleted() && category.equals(task.getCategory())) {
                categoryTasks.add(task);
            }
        }
        
        if (categoryTasks.isEmpty()) {
            return String.format("You have no pending %s tasks.", category.toLowerCase());
        }
        
        StringBuilder response = new StringBuilder(String.format("Your %s tasks:\n", category.toLowerCase()));
        for (int i = 0; i < categoryTasks.size(); i++) {
            Task task = categoryTasks.get(i);
            response.append(String.format("%d. %s (%s)%s\n", 
                    i + 1, 
                    task.getName(), 
                    task.getTime() != null ? task.getTime() : "Anytime",
                    task.isImportant() ? " [Important]" : ""));
        }
        
        return response.toString().trim();
    }
    
    private static String handleSuggestionQuery(List<Task> tasks) {
        List<TaskSuggestionService.TaskSuggestion> suggestions = 
                TaskSuggestionService.getTaskSuggestions(tasks, 3);
        
        if (suggestions.isEmpty()) {
            return "You have no pending tasks. Great job staying on top of everything!";
        }
        
        StringBuilder response = new StringBuilder("Here are my top recommendations:\n\n");
        for (TaskSuggestionService.TaskSuggestion suggestion : suggestions) {
            response.append(String.format("🎯 %s\n   Reason: %s\n\n", 
                    suggestion.getTask().getName(), 
                    suggestion.getReason()));
        }
        
        return response.toString().trim();
    }
    
    private static String handleNextTaskQuery(List<Task> tasks) {
        Task nextTask = TaskSuggestionService.getNextTaskSuggestion(tasks);
        
        if (nextTask == null) {
            return "You have no pending tasks. Enjoy your free time!";
        }
        
        return String.format("I recommend starting with: '%s'\n\nThis is a %s task%s%s", 
                nextTask.getName(),
                nextTask.getCategory().toLowerCase(),
                nextTask.getTime() != null && !nextTask.getTime().equals("Anytime") ? 
                    " scheduled for " + nextTask.getTime().toLowerCase() : "",
                nextTask.isImportant() ? " and it's marked as important." : ".");
    }
    
    private static String handleStatusQuery(List<Task> tasks) {
        int totalTasks = tasks.size();
        int completedTasks = 0;
        int importantTasks = 0;
        int todayTasks = 0;
        
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks++;
            } else {
                if (task.isImportant()) {
                    importantTasks++;
                }
                if (task.isToday()) {
                    todayTasks++;
                }
            }
        }
        
        int pendingTasks = totalTasks - completedTasks;
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;
        
        return String.format("📊 Task Status Overview:\n\n" +
                "Total tasks: %d\n" +
                "Completed: %d (%.1f%%)\n" +
                "Pending: %d\n" +
                "Important pending: %d\n" +
                "Due today: %d\n\n" +
                "%s",
                totalTasks, completedTasks, completionRate, pendingTasks, 
                importantTasks, todayTasks,
                completionRate >= 70 ? "🎉 Great progress!" : 
                completionRate >= 40 ? "👍 Keep it up!" : "💪 You can do it!");
    }
    
    private static String handleGenericQuery(String query, List<Task> tasks) {
        // Try to find tasks matching keywords in the query
        List<Task> matchingTasks = new ArrayList<>();
        String[] keywords = query.split("\\s+");
        
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                String taskText = (task.getName() + " " + task.getDescription() + " " + task.getCategory()).toLowerCase();
                for (String keyword : keywords) {
                    if (keyword.length() > 2 && taskText.contains(keyword)) {
                        matchingTasks.add(task);
                        break;
                    }
                }
            }
        }
        
        if (!matchingTasks.isEmpty()) {
            StringBuilder response = new StringBuilder("I found these related tasks:\n");
            int displayCount = Math.min(3, matchingTasks.size());
            for (int i = 0; i < displayCount; i++) {
                Task task = matchingTasks.get(i);
                response.append(String.format("• %s (%s)\n", task.getName(), task.getCategory()));
            }
            return response.toString().trim();
        }
        
        // Fallback response
        return "I can help you with questions like:\n" +
               "• 'What are my tasks today?'\n" +
               "• 'Show me important tasks'\n" +
               "• 'How many tasks do I have?'\n" +
               "• 'What should I do next?'\n" +
               "• 'Give me task suggestions'";
    }
} 