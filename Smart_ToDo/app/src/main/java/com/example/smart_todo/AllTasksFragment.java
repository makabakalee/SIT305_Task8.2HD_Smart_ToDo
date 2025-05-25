package com.example.smart_todo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AllTasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private List<Task> tasks;
    private TaskAdapter taskAdapter;
    private TaskDatabase taskDatabase;

    public AllTasksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate a simple layout with a RecyclerView
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        recyclerView = view.findViewById(R.id.tasksRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize database
        taskDatabase = new TaskDatabase(requireContext());
        
        // Get tasks from MainActivity if available, otherwise load from database
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.taskList != null && !mainActivity.taskList.isEmpty()) {
                tasks = mainActivity.taskList;
                android.util.Log.d("AllTasksFragment", "Initialized with MainActivity taskList, size: " + tasks.size());
            } else {
                tasks = taskDatabase.loadTasks();
                android.util.Log.d("AllTasksFragment", "MainActivity taskList empty, loaded from database, size: " + tasks.size());
            }
        } else {
            tasks = taskDatabase.loadTasks();
            android.util.Log.d("AllTasksFragment", "Not in MainActivity context, loaded from database, size: " + tasks.size());
        }
        
        // Setup RecyclerView
        taskAdapter = new TaskAdapter(tasks, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(taskAdapter);
        
        // Update empty view visibility
        updateEmptyViewVisibility();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshTasks();
    }
    
    public void refreshTasks() {
        // Get tasks from MainActivity instead of loading from database
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity.taskList != null) {
                tasks = mainActivity.taskList;
                android.util.Log.d("AllTasksFragment", "Refreshed tasks from MainActivity, size: " + tasks.size());
            } else {
                // Fallback to database if MainActivity taskList is null
                tasks = taskDatabase.loadTasks();
                android.util.Log.d("AllTasksFragment", "MainActivity taskList is null, loaded from database, size: " + tasks.size());
            }
        } else {
            // Fallback to database if not in MainActivity context
            tasks = taskDatabase.loadTasks();
            android.util.Log.d("AllTasksFragment", "Not in MainActivity context, loaded from database, size: " + tasks.size());
        }
        
        taskAdapter.updateTasks(tasks);
        updateEmptyViewVisibility();
    }
    
    private void updateEmptyViewVisibility() {
        if (tasks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No tasks available");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTaskClick(int position) {
        // Handle task click
        // For example, open a task detail view
    }

    @Override
    public void onCheckBoxClick(int position, boolean isChecked) {
        // Safety check: ensure position is valid and tasks is not empty
        if (tasks == null || position < 0 || position >= tasks.size()) {
            android.util.Log.e("AllTasksFragment", "Invalid position for checkbox: " + position + 
                    ", tasks size: " + (tasks != null ? tasks.size() : "null"));
            return;
        }
        
        // Get task from the current list
        Task task = tasks.get(position);
        
        // Find the corresponding task in MainActivity's task list by ID
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            List<Task> mainTaskList = mainActivity.taskList;
            
            // Find task in main list by ID
            int mainTaskPosition = -1;
            for (int i = 0; i < mainTaskList.size(); i++) {
                if (mainTaskList.get(i).getId().equals(task.getId())) {
                    mainTaskPosition = i;
                    break;
                }
            }
            
            // If found, delegate to MainActivity
            if (mainTaskPosition >= 0) {
                mainActivity.onCheckBoxClick(mainTaskPosition, isChecked);
            }
        }
    }

    @Override
    public void onEditClick(int position) {
        android.util.Log.d("AllTasksFragment", "onEditClick called with position: " + position);
        
        // Safety check: ensure position is valid and tasks is not empty
        if (tasks == null || position < 0 || position >= tasks.size()) {
            android.util.Log.e("AllTasksFragment", "Invalid position for edit: " + position + 
                    ", tasks size: " + (tasks != null ? tasks.size() : "null"));
            return;
        }
        
        // Get task from the current list
        Task task = tasks.get(position);
        android.util.Log.d("AllTasksFragment", "Found task: " + task.getName() + " with ID: " + task.getId());
        
        // Find the corresponding task in MainActivity's task list by ID
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            List<Task> mainTaskList = mainActivity.taskList;
            
            android.util.Log.d("AllTasksFragment", "MainActivity taskList size: " + 
                    (mainTaskList != null ? mainTaskList.size() : "null"));
            
            // Find task in main list by ID
            int mainTaskPosition = -1;
            for (int i = 0; i < mainTaskList.size(); i++) {
                android.util.Log.d("AllTasksFragment", "Comparing task ID: " + mainTaskList.get(i).getId() + 
                        " with target ID: " + task.getId());
                if (mainTaskList.get(i).getId().equals(task.getId())) {
                    mainTaskPosition = i;
                    android.util.Log.d("AllTasksFragment", "Found matching task at position: " + i);
                    break;
                }
            }
            
            // If found, delegate to MainActivity
            if (mainTaskPosition >= 0) {
                android.util.Log.d("AllTasksFragment", "Calling MainActivity.onEditClick with position: " + mainTaskPosition);
                mainActivity.onEditClick(mainTaskPosition);
            } else {
                android.util.Log.e("AllTasksFragment", "Could not find task with ID: " + task.getId() + " in MainActivity taskList");
            }
        } else {
            android.util.Log.e("AllTasksFragment", "getActivity() is not MainActivity instance");
        }
    }

    @Override
    public void onDeleteClick(int position) {
        android.util.Log.d("AllTasksFragment", "onDeleteClick called with position: " + position);
        
        // Safety check: ensure position is valid and tasks is not empty
        if (tasks == null || position < 0 || position >= tasks.size()) {
            android.util.Log.e("AllTasksFragment", "Invalid position for delete: " + position + 
                    ", tasks size: " + (tasks != null ? tasks.size() : "null"));
            return;
        }
        
        // Get task from the current list
        Task task = tasks.get(position);
        android.util.Log.d("AllTasksFragment", "Found task: " + task.getName() + " with ID: " + task.getId());
        
        // Find the corresponding task in MainActivity's task list by ID
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            List<Task> mainTaskList = mainActivity.taskList;
            
            android.util.Log.d("AllTasksFragment", "MainActivity taskList size: " + 
                    (mainTaskList != null ? mainTaskList.size() : "null"));
            
            // Find task in main list by ID
            int mainTaskPosition = -1;
            for (int i = 0; i < mainTaskList.size(); i++) {
                android.util.Log.d("AllTasksFragment", "Comparing task ID: " + mainTaskList.get(i).getId() + 
                        " with target ID: " + task.getId());
                if (mainTaskList.get(i).getId().equals(task.getId())) {
                    mainTaskPosition = i;
                    android.util.Log.d("AllTasksFragment", "Found matching task at position: " + i);
                    break;
                }
            }
            
            // If found, delegate to MainActivity
            if (mainTaskPosition >= 0) {
                android.util.Log.d("AllTasksFragment", "Calling MainActivity.onDeleteClick with position: " + mainTaskPosition);
                mainActivity.onDeleteClick(mainTaskPosition);
            } else {
                android.util.Log.e("AllTasksFragment", "Could not find task with ID: " + task.getId() + " in MainActivity taskList");
            }
        } else {
            android.util.Log.e("AllTasksFragment", "getActivity() is not MainActivity instance");
        }
    }

    @Override
    public void onImportantClick(int position) {
        // Safety check: ensure position is valid and tasks is not empty
        if (tasks == null || position < 0 || position >= tasks.size()) {
            android.util.Log.e("AllTasksFragment", "Invalid position for important: " + position + 
                    ", tasks size: " + (tasks != null ? tasks.size() : "null"));
            return;
        }
        
        // Get task from the current list
        Task task = tasks.get(position);
        
        // Find the corresponding task in MainActivity's task list by ID
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            List<Task> mainTaskList = mainActivity.taskList;
            
            // Find task in main list by ID
            int mainTaskPosition = -1;
            for (int i = 0; i < mainTaskList.size(); i++) {
                if (mainTaskList.get(i).getId().equals(task.getId())) {
                    mainTaskPosition = i;
                    break;
                }
            }
            
            // If found, delegate to MainActivity
            if (mainTaskPosition >= 0) {
                mainActivity.onImportantClick(mainTaskPosition);
            }
        }
    }
} 