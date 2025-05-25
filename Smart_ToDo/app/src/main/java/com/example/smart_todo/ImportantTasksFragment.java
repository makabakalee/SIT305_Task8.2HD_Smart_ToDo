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

import java.util.ArrayList;
import java.util.List;

public class ImportantTasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private List<Task> allTasks;
    private List<Task> importantTasks;
    private TaskAdapter taskAdapter;
    private TaskDatabase taskDatabase;

    public ImportantTasksFragment() {
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
        
        // Initialize database and tasks
        taskDatabase = new TaskDatabase(requireContext());
        allTasks = taskDatabase.loadTasks();
        importantTasks = new ArrayList<>();
        
        // Filter important tasks
        filterImportantTasks();
        
        // Setup RecyclerView
        taskAdapter = new TaskAdapter(importantTasks, this);
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
        allTasks = taskDatabase.loadTasks();
        filterImportantTasks();
        taskAdapter.updateTasks(importantTasks);
        updateEmptyViewVisibility();
    }
    
    private void filterImportantTasks() {
        importantTasks.clear();
        for (Task task : allTasks) {
            if (task.isImportant() && !task.isCompleted()) {
                importantTasks.add(task);
            }
        }
    }
    
    private void updateEmptyViewVisibility() {
        if (importantTasks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No important tasks");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTaskClick(int position) {
        // Handle task click
    }

    @Override
    public void onCheckBoxClick(int position, boolean isChecked) {
        // Get task from the current filtered list
        Task task = importantTasks.get(position);
        
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
        // Get task from the current filtered list
        Task task = importantTasks.get(position);
        
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
                mainActivity.onEditClick(mainTaskPosition);
            }
        }
    }

    @Override
    public void onDeleteClick(int position) {
        // Get task from the current filtered list
        Task task = importantTasks.get(position);
        
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
                mainActivity.onDeleteClick(mainTaskPosition);
            }
        }
    }

    @Override
    public void onImportantClick(int position) {
        // Get task from the current filtered list
        Task task = importantTasks.get(position);
        
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