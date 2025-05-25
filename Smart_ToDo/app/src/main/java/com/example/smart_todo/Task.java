package com.example.smart_todo;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private String id;
    private String name;
    private String description;
    private String category;
    private String time;
    private Date createdAt;
    private Date dueDate;
    private boolean completed;
    private boolean important;
    private int priority; // 0: Low, 1: Medium, 2: High

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.completed = false;
        this.important = false;
        this.priority = 0;
        this.description = "";
    }

    public Task(String name, String category, String time) {
        this();
        this.name = name;
        this.category = category;
        this.time = time;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority >= 0 && priority <= 2) {
            this.priority = priority;
        }
    }

    public boolean isToday() {
        // Check if time contains today or is for today
        String lowerTime = time.toLowerCase();
        return lowerTime.contains("today") || 
               lowerTime.contains("morning") || 
               lowerTime.contains("afternoon") || 
               lowerTime.contains("evening") ||
               lowerTime.contains("tonight");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 