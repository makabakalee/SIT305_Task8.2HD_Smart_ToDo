package com.example.smart_todo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(int position);
        void onCheckBoxClick(int position, boolean isChecked);
        void onEditClick(int position);
        void onDeleteClick(int position);
        void onImportantClick(int position);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item_improved, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        // Set task name and check state
        holder.taskName.setText(task.getName());
        holder.taskCheckBox.setChecked(task.isCompleted());
        
        // Update UI for completed tasks
        if (task.isCompleted()) {
            holder.taskName.setAlpha(0.5f);
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.taskName.setAlpha(1.0f);
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        
        // Set description if available
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.taskDescription.setText(task.getDescription());
            holder.taskDescription.setVisibility(View.VISIBLE);
        } else {
            holder.taskDescription.setVisibility(View.GONE);
        }
        
        // Set category and time
        holder.taskCategory.setText(task.getCategory());
        holder.taskTime.setText(task.getTime());
        
        // Set priority star - ALWAYS SHOW IT, just change the icon
        holder.taskPriority.setVisibility(View.VISIBLE);
        
        // Set star color based on importance and priority
        if (task.isImportant() || task.getPriority() > 0) {
            switch(task.getPriority()) {
                case 2: // High
                    holder.taskPriority.setImageResource(android.R.drawable.btn_star_big_on);
                    break;
                case 1: // Medium
                    holder.taskPriority.setImageResource(android.R.drawable.btn_star);
                    break;
                default: // Low
                    holder.taskPriority.setImageResource(android.R.drawable.btn_star);
                    break;
            }
        } else {
            // Not important
            holder.taskPriority.setImageResource(android.R.drawable.btn_star_big_off);
        }
        
        // Set category background color based on category type
        int categoryColor;
        switch (task.getCategory().toLowerCase()) {
            case "work":
                categoryColor = 0xFF5C6BC0; // Indigo
                break;
            case "study":
                categoryColor = 0xFF66BB6A; // Green
                break;
            case "shopping":
                categoryColor = 0xFFEF5350; // Red
                break;
            case "health":
                categoryColor = 0xFF26A69A; // Teal
                break;
            default:
                categoryColor = 0xFF7E57C2; // Deep Purple
                break;
        }
        holder.taskCategory.getBackground().setTint(categoryColor);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        
        // Sort tasks: incomplete tasks first, then completed tasks
        java.util.Collections.sort(this.taskList, (task1, task2) -> {
            // First, compare completion status
            if (task1.isCompleted() && !task2.isCompleted()) {
                return 1; // Task1 is completed, Task2 is not, so Task1 comes after
            } else if (!task1.isCompleted() && task2.isCompleted()) {
                return -1; // Task1 is not completed, Task2 is, so Task1 comes before
            }
            
            // If both tasks have the same completion status, sort by creation time (newer first)
            // This assumes newer tasks are added to the beginning of the list
            return 0; // Maintain original order
        });
        
        notifyDataSetChanged();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskCategory, taskTime, taskDescription;
        CheckBox taskCheckBox;
        ImageView taskPriority, taskMenu;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskName);
            taskCategory = itemView.findViewById(R.id.taskCategory);
            taskTime = itemView.findViewById(R.id.taskTime);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskPriority = itemView.findViewById(R.id.taskPriority);
            taskMenu = itemView.findViewById(R.id.taskMenu);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTaskClick(getAdapterPosition());
                }
            });

            taskCheckBox.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCheckBoxClick(getAdapterPosition(), taskCheckBox.isChecked());
                }
            });
            
            taskMenu.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, getAdapterPosition());
                }
            });
            
            taskPriority.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onImportantClick(getAdapterPosition());
                }
            });
        }

        private void showPopupMenu(View view, int position) {
            android.util.Log.d("TaskAdapter", "showPopupMenu called for position: " + position);
            
            android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.task_menu);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                android.util.Log.d("TaskAdapter", "Menu item clicked: " + itemId + " for position: " + position);
                
                if (itemId == R.id.action_edit) {
                    android.util.Log.d("TaskAdapter", "Edit action selected for position: " + position);
                    listener.onEditClick(position);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    android.util.Log.d("TaskAdapter", "Delete action selected for position: " + position);
                    listener.onDeleteClick(position);
                    return true;
                }
                return false;
            });
            popup.show();
            android.util.Log.d("TaskAdapter", "Popup menu shown");
        }
    }
} 