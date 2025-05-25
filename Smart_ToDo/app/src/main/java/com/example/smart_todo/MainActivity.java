package com.example.smart_todo;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatDelegate;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

// MongoDB imports
import android.os.Handler;
import android.os.Looper;

// Permission imports
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;
    private static final String TAG = "MainActivity";
    
    private Toolbar toolbar;
    private TextInputEditText taskInputEditText;
    private MaterialButton voiceInputButton, addTaskButton;
    private FloatingActionButton suggestionsFab;
    private View progressBar;
    private BottomNavigationView bottomNav;
    
    public List<Task> taskList;
    private TaskAdapter taskAdapter;
    private ApiService apiService;
    private TaskDatabase taskDatabase;
    
    // MongoDB manager
    private MongoDBTaskManager mongoDBTaskManager;
    private Handler mainHandler;
    
    // Fragments
    private AllTasksFragment allTasksFragment;
    private TodayTasksFragment todayTasksFragment;
    private ImportantTasksFragment importantTasksFragment;
    
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 应用默认设置
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize UI components
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        taskInputEditText = findViewById(R.id.taskInputEditText);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        addTaskButton = findViewById(R.id.addTaskButton);
        suggestionsFab = findViewById(R.id.suggestionsFab);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.bottomNav);
        
        // Initialize MongoDB
        initializeMongoDB();
        
        // Initialize database
        taskDatabase = new TaskDatabase(this);
        
        // Initialize empty task list first
        taskList = new ArrayList<>();
        
        // Load tasks from MongoDB first, then fallback to local storage
        loadTasksFromMongoDB();
        
        // 使用默认URL初始化API服务
        apiService = new ApiService("http://10.0.2.2:5000/chat");
        
        // Initialize fragments
        initFragments();
        
        // Set up bottom navigation
        setupBottomNavigation();
        
        // Set up click listeners
        voiceInputButton.setOnClickListener(v -> startVoiceRecognition());
        addTaskButton.setOnClickListener(v -> processUserInput());
        suggestionsFab.setOnClickListener(v -> showTaskSuggestions());
        
        // Check if speech recognition is available
        checkSpeechRecognitionAvailability();
    }
    
    /**
     * Initialize MongoDB connection and manager
     */
    private void initializeMongoDB() {
        try {
            // Initialize MongoDB configuration
            MongoDBConfig.initialize();
            
            // Initialize MongoDB task manager
            mongoDBTaskManager = MongoDBTaskManager.getInstance();
            
            // Initialize main thread handler for UI updates
            mainHandler = new Handler(Looper.getMainLooper());
            
            // Test MongoDB connection in background
            new Thread(() -> {
                boolean connectionTest = MongoDBConfig.testConnection();
                mainHandler.post(() -> {
                    if (connectionTest) {
                        Toast.makeText(this, "MongoDB connected successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "MongoDB connection failed - using local storage", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to initialize MongoDB", e);
            Toast.makeText(this, "MongoDB initialization failed - using local storage", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Load tasks from MongoDB
     */
    private void loadTasksFromMongoDB() {
        if (mongoDBTaskManager != null) {
            mongoDBTaskManager.getAllTasks(new MongoDBTaskManager.TaskListCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                    mainHandler.post(() -> {
                        taskList.clear();
                        taskList.addAll(tasks);
                        
                        // Sort tasks (uncompleted first)
                        java.util.Collections.sort(taskList, (task1, task2) -> {
                            if (task1.isCompleted() && !task2.isCompleted()) {
                                return 1;
                            } else if (!task1.isCompleted() && task2.isCompleted()) {
                                return -1;
                            }
                            return 0;
                        });
                        
                        android.util.Log.d(TAG, "Loaded " + tasks.size() + " tasks from MongoDB");
                        refreshFragments();
                    });
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e(TAG, "Failed to load tasks from MongoDB: " + error);
                    mainHandler.post(() -> {
                        // Fallback to local storage
                        List<Task> localTasks = taskDatabase.loadTasks();
                        taskList.clear();
                        taskList.addAll(localTasks);
                        refreshFragments();
                    });
                }
            });
        } else {
            // Fallback to local storage
            List<Task> localTasks = taskDatabase.loadTasks();
            taskList.addAll(localTasks);
        }
    }
    
    private void initFragments() {
        allTasksFragment = new AllTasksFragment();
        todayTasksFragment = new TodayTasksFragment();
        importantTasksFragment = new ImportantTasksFragment();
        
        // Set initial fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, importantTasksFragment, "important")
                .hide(importantTasksFragment)
                .add(R.id.fragmentContainer, todayTasksFragment, "today")
                .hide(todayTasksFragment)
                .add(R.id.fragmentContainer, allTasksFragment, "all")
                .commit();
        
        activeFragment = allTasksFragment;
    }
    
    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_all) {
                switchFragment(allTasksFragment);
                return true;
            } else if (itemId == R.id.navigation_today) {
                switchFragment(todayTasksFragment);
                return true;
            } else if (itemId == R.id.navigation_important) {
                switchFragment(importantTasksFragment);
                return true;
            }
            
            return false;
        });
    }
    
    private void switchFragment(Fragment fragment) {
        if (activeFragment != fragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            activeFragment = fragment;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Save tasks when app is paused
        taskDatabase.saveTasks(taskList);
    }
    
    /**
     * Check if speech recognition is available on this device
     */
    private void checkSpeechRecognitionAvailability() {
        PackageManager pm = getPackageManager();
        List<android.content.pm.ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        
        if (activities.size() == 0) {
            // Speech recognition not available
            voiceInputButton.setEnabled(false);
            voiceInputButton.setAlpha(0.5f);
            android.util.Log.w(TAG, "Speech recognition not available on this device");
        } else {
            android.util.Log.d(TAG, "Speech recognition is available");
        }
    }
    
    private void startVoiceRecognition() {
        // Check if we have permission to record audio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }
        
        // Permission granted, proceed with voice recognition
        performVoiceRecognition();
    }
    
    private void performVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add a task");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start voice recognition
                performVoiceRecognition();
            } else {
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // Show explanation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Microphone Permission Required")
                            .setMessage("This app needs microphone access to convert your speech to text for adding tasks. Please grant the permission to use voice input feature.")
                            .setPositiveButton("Grant Permission", (dialog, which) -> {
                                ActivityCompat.requestPermissions(this, 
                                        new String[]{Manifest.permission.RECORD_AUDIO}, 
                                        PERMISSION_REQUEST_RECORD_AUDIO);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(this, "Microphone permission is required for voice input. Please enable it in app settings.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    android.util.Log.d(TAG, "Speech recognition result: " + spokenText);
                    taskInputEditText.setText(spokenText);
                    processUserInput();
                } else {
                    android.util.Log.w(TAG, "Speech recognition returned empty results");
                    Toast.makeText(this, "No speech detected. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle different error cases
                switch (resultCode) {
                    case RecognizerIntent.RESULT_AUDIO_ERROR:
                        android.util.Log.e(TAG, "Audio recording error");
                        Toast.makeText(this, "Audio recording error. Please check your microphone.", Toast.LENGTH_SHORT).show();
                        break;
                    case RecognizerIntent.RESULT_CLIENT_ERROR:
                        android.util.Log.e(TAG, "Client side error");
                        Toast.makeText(this, "Speech recognition client error. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                    case RecognizerIntent.RESULT_NETWORK_ERROR:
                        android.util.Log.e(TAG, "Network error");
                        Toast.makeText(this, "Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                        break;
                    case RecognizerIntent.RESULT_NO_MATCH:
                        android.util.Log.w(TAG, "No speech match found");
                        Toast.makeText(this, "Could not understand speech. Please speak clearly and try again.", Toast.LENGTH_SHORT).show();
                        break;
                    case RecognizerIntent.RESULT_SERVER_ERROR:
                        android.util.Log.e(TAG, "Server error");
                        Toast.makeText(this, "Speech recognition server error. Please try again later.", Toast.LENGTH_SHORT).show();
                        break;
                    case RESULT_CANCELED:
                        android.util.Log.d(TAG, "Speech recognition canceled by user");
                        // User canceled, no need to show error message
                        break;
                    default:
                        android.util.Log.w(TAG, "Speech recognition failed with result code: " + resultCode);
                        Toast.makeText(this, "Speech recognition failed. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
    
    private void processUserInput() {
        String userInput = taskInputEditText.getText() != null ? taskInputEditText.getText().toString().trim() : "";
        
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter a task or ask a question", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if input is a question/query or a task creation request
        if (isUserQuery(userInput)) {
            // Handle as query - Feature 4: Ask questions
            handleUserQuery(userInput);
        } else {
            // Handle as task creation - Feature 1: Natural task input & Feature 2: Smart sorting
            handleTaskCreation(userInput);
        }
    }
    
    /**
     * Determine if user input is a question/query rather than task creation
     * Feature 4 support: Natural language question detection
     */
    private boolean isUserQuery(String input) {
        String lowerInput = input.toLowerCase().trim();
        
        // Question indicators
        String[] questionWords = {"what", "how", "when", "where", "why", "which", "who"};
        String[] queryPhrases = {"show me", "tell me", "list", "give me", "do i have", "suggest", "recommend"};
        
        // Check for question words at the beginning
        for (String questionWord : questionWords) {
            if (lowerInput.startsWith(questionWord + " ")) {
                return true;
            }
        }
        
        // Check for query phrases
        for (String phrase : queryPhrases) {
            if (lowerInput.startsWith(phrase)) {
                return true;
            }
        }
        
        // Check for question marks
        if (lowerInput.contains("?")) {
            return true;
        }
        
        // Check for specific query keywords
        if (lowerInput.contains("tasks today") || lowerInput.contains("tasks tomorrow") ||
            lowerInput.contains("important tasks") || lowerInput.contains("completed tasks") ||
            lowerInput.contains("task status") || lowerInput.contains("next task")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle user queries - Feature 4: Ask questions implementation
     */
    private void handleUserQuery(String userInput) {
        showLoading(true);
        
        // Process query in background thread to avoid UI blocking
        new Thread(() -> {
            String response = TaskQueryService.processQuery(userInput, taskList);
            
            runOnUiThread(() -> {
                showLoading(false);
                showQueryResponse(response);
                taskInputEditText.setText("");
            });
        }).start();
    }
    
    /**
     * Show query response to user with suggestions if applicable
     */
    private void showQueryResponse(String response) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Smart Assistant Response");
        builder.setMessage(response);
        
        // Add suggestions button if there are pending tasks
        boolean hasPendingTasks = false;
        for (Task task : taskList) {
            if (!task.isCompleted()) {
                hasPendingTasks = true;
                break;
            }
        }
        
        if (hasPendingTasks) {
            builder.setPositiveButton("Show Suggestions", (dialog, which) -> {
                showTaskSuggestions();
            });
        }
        
        builder.setNeutralButton("OK", null);
        builder.show();
    }
    
    /**
     * Show task suggestions dialog - Feature 3: Suggestions implementation
     */
    private void showTaskSuggestions() {
        List<TaskSuggestionService.TaskSuggestion> suggestions = 
                TaskSuggestionService.getTaskSuggestions(taskList, 5);
        
        if (suggestions.isEmpty()) {
            Toast.makeText(this, "No task suggestions available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎯 Task Recommendations");
        
        StringBuilder message = new StringBuilder();
        for (TaskSuggestionService.TaskSuggestion suggestion : suggestions) {
            message.append(String.format("▶ %s\n", suggestion.getTask().getName()));
            message.append(String.format("   Reason: %s\n", suggestion.getReason()));
            message.append(String.format("   Category: %s | Time: %s\n\n", 
                    suggestion.getTask().getCategory(),
                    suggestion.getTask().getTime()));
        }
        
        builder.setMessage(message.toString().trim());
        builder.setPositiveButton("Start First Task", (dialog, which) -> {
            Task firstTask = suggestions.get(0).getTask();
            markTaskAsStarted(firstTask);
        });
        builder.setNeutralButton("Close", null);
        builder.show();
    }
    
    /**
     * Mark a task as started (for suggestion implementation)
     */
    private void markTaskAsStarted(Task task) {
        Toast.makeText(this, "Starting task: " + task.getName(), Toast.LENGTH_LONG).show();
        
        // You could add additional "started" state to Task class if needed
        // For now, we'll just show a confirmation and highlight the task
        
        // Find the task in list and show it to user
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(task.getId())) {
                // You could implement highlighting or other visual feedback here
                break;
            }
        }
    }
    
    /**
     * Handle task creation - Original functionality enhanced
     * Feature 1: Natural task input & Feature 2: Smart sorting
     */
    private void handleTaskCreation(String userInput) {
        showLoading(true);
        
        // Send to API service for processing - Feature 1 & 2 implementation
        apiService.processTask(userInput, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(Task task) {
                runOnUiThread(() -> {
                    showLoading(false);
                    addTaskToList(task);
                    taskInputEditText.setText("");
                    
                    // Save tasks to persistent storage
                    taskDatabase.saveTasks(taskList);
                    
                    // Refresh fragments
                    refreshFragments();
                    
                    // Show suggestion if this is an important task
                    if (task.isImportant() || task.getPriority() > 0) {
                        showQuickSuggestion(task);
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    
                    // Add basic task anyway with smart categorization fallback
                    Task basicTask = createBasicTaskWithSmartCategorization(userInput);
                    addTaskToList(basicTask);
                    taskInputEditText.setText("");
                    
                    // Save tasks to persistent storage
                    taskDatabase.saveTasks(taskList);
                    
                    // Refresh fragments
                    refreshFragments();
                });
            }
        });
    }
    
    /**
     * Create basic task with local smart categorization when API fails
     * Feature 2 fallback: Local smart sorting implementation
     */
    private Task createBasicTaskWithSmartCategorization(String userInput) {
        String lowerInput = userInput.toLowerCase();
        String category = "Personal"; // Default
        String time = "Anytime"; // Default
        int priority = 0;
        boolean important = false;
        
        // Smart categorization - Feature 2 implementation
        if (lowerInput.contains("work") || lowerInput.contains("meeting") || 
            lowerInput.contains("office") || lowerInput.contains("client") ||
            lowerInput.contains("project") || lowerInput.contains("report")) {
            category = "Work";
            priority = 1;
        } else if (lowerInput.contains("study") || lowerInput.contains("homework") || 
                   lowerInput.contains("assignment") || lowerInput.contains("exam") ||
                   lowerInput.contains("class") || lowerInput.contains("learn")) {
            category = "Study";
            priority = 1;
        } else if (lowerInput.contains("buy") || lowerInput.contains("shop") || 
                   lowerInput.contains("purchase") || lowerInput.contains("groceries") ||
                   lowerInput.contains("store") || lowerInput.contains("market")) {
            category = "Shopping";
        } else if (lowerInput.contains("exercise") || lowerInput.contains("doctor") || 
                   lowerInput.contains("health") || lowerInput.contains("medicine") ||
                   lowerInput.contains("hospital") || lowerInput.contains("gym")) {
            category = "Health";
            priority = 1;
        }
        
        // Time extraction
        if (lowerInput.contains("today")) {
            time = "Today";
            priority = Math.max(priority, 1);
        } else if (lowerInput.contains("tomorrow")) {
            time = "Tomorrow";
            priority = Math.max(priority, 1);
        } else if (lowerInput.contains("morning")) {
            time = "Morning";
        } else if (lowerInput.contains("afternoon")) {
            time = "Afternoon";
        } else if (lowerInput.contains("evening")) {
            time = "Evening";
        }
        
        // Priority detection
        if (lowerInput.contains("important") || lowerInput.contains("urgent") || 
            lowerInput.contains("priority") || lowerInput.contains("asap")) {
            important = true;
            priority = 2;
        }
        
        Task task = new Task(userInput, category, time);
        task.setPriority(priority);
        task.setImportant(important);
        
        return task;
    }
    
    /**
     * Show quick suggestion for newly added important tasks
     * Feature 3: Contextual suggestions
     */
    private void showQuickSuggestion(Task newTask) {
        Task nextSuggestion = TaskSuggestionService.getNextTaskSuggestion(taskList);
        
        if (nextSuggestion != null && !nextSuggestion.getId().equals(newTask.getId())) {
            String message = String.format("Task added! Next suggested task: '%s' (%s)", 
                    nextSuggestion.getName(), nextSuggestion.getCategory());
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    
    private void addTaskToList(Task task) {
        // Add the task to the list (uncompleted tasks should be at the beginning)
        if (task.isCompleted()) {
            // Find the position after the last uncompleted task
            int insertPosition = 0;
            while (insertPosition < taskList.size() && !taskList.get(insertPosition).isCompleted()) {
                insertPosition++;
            }
            taskList.add(insertPosition, task);
        } else {
            // Add to the beginning for uncompleted tasks
            taskList.add(0, task);
        }
        
        // Save to MongoDB
        if (mongoDBTaskManager != null) {
            mongoDBTaskManager.saveTask(task, new MongoDBTaskManager.TaskOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    android.util.Log.d(TAG, "Task saved to MongoDB: " + task.getName());
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e(TAG, "Failed to save task to MongoDB: " + error);
                    // Task is already in local list, so we can continue
                }
            });
        }
        
        // Show toast with task information
        String message = "Added: " + task.getName() + " (" + task.getCategory() + ", " + task.getTime() + ")";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void refreshFragments() {
        if (allTasksFragment != null) {
            allTasksFragment.refreshTasks();
        }
        if (todayTasksFragment != null) {
            todayTasksFragment.refreshTasks();
        }
        if (importantTasksFragment != null) {
            importantTasksFragment.refreshTasks();
        }
    }
    
    public void refreshTasks() {
        // Reload tasks from database
        taskList = taskDatabase.loadTasks();
        
        // Apply sorting for completed tasks
        java.util.Collections.sort(taskList, (task1, task2) -> {
            if (task1.isCompleted() && !task2.isCompleted()) {
                return 1;
            } else if (!task1.isCompleted() && task2.isCompleted()) {
                return -1;
            }
            return 0;
        });
        
        // Refresh all fragments to ensure consistency
        refreshFragments();
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        addTaskButton.setEnabled(!show);
        voiceInputButton.setEnabled(!show);
    }
    
    @Override
    public void onTaskClick(int position) {
        // Safety check: ensure position is valid and taskList is not empty
        if (taskList == null || position < 0 || position >= taskList.size()) {
            android.util.Log.e(TAG, "Invalid position for task click: " + position + ", taskList size: " + 
                    (taskList != null ? taskList.size() : "null"));
            return;
        }
        
        Task task = taskList.get(position);
        Toast.makeText(this, "Task: " + task.getName(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCheckBoxClick(int position, boolean isChecked) {
        // Safety check: ensure position is valid and taskList is not empty
        if (taskList == null || position < 0 || position >= taskList.size()) {
            android.util.Log.e(TAG, "Invalid position for checkbox: " + position + ", taskList size: " + 
                    (taskList != null ? taskList.size() : "null"));
            return;
        }
        
        Task task = taskList.get(position);
        task.setCompleted(isChecked);
        
        // Save task status change to local database
        taskDatabase.saveTasks(taskList);
        
        // Update task in MongoDB
        if (mongoDBTaskManager != null) {
            mongoDBTaskManager.updateTask(task, new MongoDBTaskManager.TaskOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    android.util.Log.d(TAG, "Task status updated in MongoDB: " + task.getName());
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e(TAG, "Failed to update task in MongoDB: " + error);
                }
            });
        }
        
        // If current fragment is AllTasksFragment, need to reorder the tasks
        // Other fragments will automatically hide completed tasks
        if (activeFragment instanceof AllTasksFragment) {
            java.util.Collections.sort(taskList, (task1, task2) -> {
                if (task1.isCompleted() && !task2.isCompleted()) {
                    return 1;
                } else if (!task1.isCompleted() && task2.isCompleted()) {
                    return -1;
                }
                return 0;
            });
            
            allTasksFragment.refreshTasks();
        }
        
        // Always refresh all fragments to ensure consistency
        if (todayTasksFragment != null) {
            todayTasksFragment.refreshTasks();
        }
        if (importantTasksFragment != null) {
            importantTasksFragment.refreshTasks();
        }
    }
    
    @Override
    public void onEditClick(int position) {
        android.util.Log.d(TAG, "MainActivity.onEditClick called with position: " + position);
        // Open edit dialog
        showEditTaskDialog(position);
    }
    
    @Override
    public void onDeleteClick(int position) {
        android.util.Log.d(TAG, "MainActivity.onDeleteClick called with position: " + position);
        // Show confirmation dialog
        showDeleteConfirmationDialog(position);
    }
    
    @Override
    public void onImportantClick(int position) {
        // Safety check: ensure position is valid and taskList is not empty
        if (taskList == null || position < 0 || position >= taskList.size()) {
            android.util.Log.e(TAG, "Invalid position for important: " + position + ", taskList size: " + 
                    (taskList != null ? taskList.size() : "null"));
            return;
        }
        
        Task task = taskList.get(position);
        
        // Toggle important status
        boolean newStatus = !task.isImportant();
        task.setImportant(newStatus);
        
        // Set priority based on importance
        if (newStatus) {
            task.setPriority(2); // High priority when marked as important
        } else {
            // Reset to normal priority if not marked as important
            task.setPriority(0);
        }
        
        // Save changes to local database
        taskDatabase.saveTasks(taskList);
        
        // Update task in MongoDB
        if (mongoDBTaskManager != null) {
            mongoDBTaskManager.updateTask(task, new MongoDBTaskManager.TaskOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    android.util.Log.d(TAG, "Task importance updated in MongoDB: " + task.getName());
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e(TAG, "Failed to update task importance in MongoDB: " + error);
                }
            });
        }
        
        // Show feedback
        int messageResId = task.isImportant() ? 
                R.string.toast_task_important : 
                R.string.toast_task_not_important;
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
        
        // Refresh all fragments to ensure consistency
        refreshFragments();
    }
    
    // Add new helper methods for dialogs
    private void showEditTaskDialog(int position) {
        android.util.Log.d(TAG, "showEditTaskDialog called with position: " + position);
        
        // Safety check: ensure position is valid and taskList is not empty
        if (taskList == null || position < 0 || position >= taskList.size()) {
            android.util.Log.e(TAG, "Invalid position for edit: " + position + ", taskList size: " + 
                    (taskList != null ? taskList.size() : "null"));
            Toast.makeText(this, "Error: Invalid task position", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Task task = taskList.get(position);
        android.util.Log.d(TAG, "Editing task: " + task.getName() + " with ID: " + task.getId());
        
        // Create dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        android.util.Log.d(TAG, "Dialog view inflated successfully");
        
        // Get references to dialog views
        TextInputEditText nameEditText = dialogView.findViewById(R.id.taskNameEditText);
        TextInputEditText descriptionEditText = dialogView.findViewById(R.id.taskDescriptionEditText);
        TextInputEditText timeEditText = dialogView.findViewById(R.id.taskTimeEditText);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        
        android.util.Log.d(TAG, "Dialog views found: nameEditText=" + (nameEditText != null) + 
                ", descriptionEditText=" + (descriptionEditText != null) + 
                ", timeEditText=" + (timeEditText != null) + 
                ", categorySpinner=" + (categorySpinner != null));
        
        // Set current values
        nameEditText.setText(task.getName());
        descriptionEditText.setText(task.getDescription());
        timeEditText.setText(task.getTime());
        
        // Setup category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, 
                R.array.categories, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        // Set selected category
        String[] categories = getResources().getStringArray(R.array.categories);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(task.getCategory())) {
                categorySpinner.setSelection(i);
                break;
            }
        }
        
        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_edit_task)
                .setView(dialogView)
                .setPositiveButton(R.string.button_save, (dialogInterface, which) -> {
                    // Update task with new values
                    task.setName(nameEditText.getText().toString().trim());
                    task.setDescription(descriptionEditText.getText().toString().trim());
                    task.setTime(timeEditText.getText().toString().trim());
                    task.setCategory(categorySpinner.getSelectedItem().toString());
                    
                    // Save changes to local database
                    taskDatabase.saveTasks(taskList);
                    
                    // Update task in MongoDB
                    if (mongoDBTaskManager != null) {
                        mongoDBTaskManager.updateTask(task, new MongoDBTaskManager.TaskOperationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                android.util.Log.d(TAG, "Task updated in MongoDB: " + task.getName());
                            }
                            
                            @Override
                            public void onError(String error) {
                                android.util.Log.e(TAG, "Failed to update task in MongoDB: " + error);
                            }
                        });
                    }
                    
                    // Refresh fragments
                    refreshFragments();
                    
                    Toast.makeText(this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .create();
            
        dialog.show();
    }
    
    private void showDeleteConfirmationDialog(int position) {
        android.util.Log.d(TAG, "showDeleteConfirmationDialog called with position: " + position);
        
        // Safety check: ensure position is valid and taskList is not empty
        if (taskList == null || position < 0 || position >= taskList.size()) {
            android.util.Log.e(TAG, "Invalid position for delete: " + position + ", taskList size: " + 
                    (taskList != null ? taskList.size() : "null"));
            Toast.makeText(this, "Error: Invalid task position", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Task task = taskList.get(position);
        android.util.Log.d(TAG, "Deleting task: " + task.getName() + " with ID: " + task.getId());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_task)
                .setMessage(getString(R.string.message_delete_confirmation, task.getName()))
                .setPositiveButton(R.string.button_delete, (dialogInterface, which) -> {
                    android.util.Log.d(TAG, "Delete confirmed for task: " + task.getName());
                    
                    // Get task ID for MongoDB deletion
                    String taskId = task.getId();
                    
                    // Remove task from local list
                    taskList.remove(position);
                    
                    // Save changes to local database
                    taskDatabase.saveTasks(taskList);
                    
                    // Delete task from MongoDB
                    if (mongoDBTaskManager != null) {
                        mongoDBTaskManager.deleteTask(taskId, new MongoDBTaskManager.TaskOperationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                android.util.Log.d(TAG, "Task deleted from MongoDB: " + taskId);
                            }
                            
                            @Override
                            public void onError(String error) {
                                android.util.Log.e(TAG, "Failed to delete task from MongoDB: " + error);
                            }
                        });
                    }
                    
                    // Refresh fragments
                    refreshFragments();
                    
                    Toast.makeText(this, R.string.toast_task_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .create();
        
        dialog.show();
    }
    
    /**
     * Test MongoDB functionality by creating, updating, and retrieving a test task
     */
    private void testMongoDBFunctionality() {
        if (mongoDBTaskManager == null) {
            Toast.makeText(this, "MongoDB not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a test task
        Task testTask = new Task("Test MongoDB Connection", "Testing", "Now");
        testTask.setDescription("This is a test task to verify MongoDB integration");
        testTask.setImportant(true);
        
        // Save the test task
        mongoDBTaskManager.saveTask(testTask, new MongoDBTaskManager.TaskOperationCallback() {
            @Override
            public void onSuccess(String message) {
                android.util.Log.d(TAG, "MongoDB test task saved successfully");
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "MongoDB test: Task saved successfully", Toast.LENGTH_SHORT).show();
                });
                
                // Test retrieval
                mongoDBTaskManager.getAllTasks(new MongoDBTaskManager.TaskListCallback() {
                    @Override
                    public void onSuccess(List<Task> tasks) {
                        android.util.Log.d(TAG, "MongoDB test: Retrieved " + tasks.size() + " tasks");
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "MongoDB test: Retrieved " + tasks.size() + " tasks", Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e(TAG, "MongoDB test: Failed to retrieve tasks - " + error);
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "MongoDB test failed: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e(TAG, "MongoDB test: Failed to save task - " + error);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "MongoDB test failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Close MongoDB connections
        if (mongoDBTaskManager != null) {
            mongoDBTaskManager.shutdown();
        }
        MongoDBConfig.close();
    }
}