package com.sujay.apps.todolist;

import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sujay.apps.todolist.databinding.FragmentAddTaskBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTaskFragment extends Fragment {

    private FragmentAddTaskBinding binding;
    private FirebaseFirestore firestore; // Firestore database instance
    private Calendar selectedDate, selectedTime;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get reference to the EditText input field
        EditText taskInput = binding.taskInput;
        selectedDate = Calendar.getInstance();
        selectedTime = Calendar.getInstance();

        binding.time.setOnClickListener(v -> {
            showTimePicker();
        });
        binding.date.setOnClickListener(v -> {
            showDatePicker();
        });

        String textToEdit = TaskListFragment.editText[1];
        if(textToEdit != null){
            taskInput.setText(textToEdit);
            binding.time.setText(TaskListFragment.editText[2]);
            binding.date.setText(TaskListFragment.editText[3]);
            binding.repeating.setChecked(TaskListFragment.isRepeating);
        }

        // Automatically show the keyboard and request focus
        taskInput.requestFocus();
        showKeyboard(taskInput);

        // Initialize firestore
        firestore = FirebaseFirestore.getInstance();

        // Set up the save button click listener
        binding.saveButton.setOnClickListener(v -> saveTask());

        String text = binding.taskInput.getText().toString();
        String time = binding.time.getText().toString();
        String date = binding.date.getText().toString();
        boolean isRepeating = binding.repeating.isChecked();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String textChanged = binding.taskInput.getText().toString();
                String timeChanged = binding.time.getText().toString();
                String dateChanged = binding.date.getText().toString();
                boolean repeatchanged = binding.repeating.isChecked();

                if(!textChanged.equals(text) || !timeChanged.equals(time) || !dateChanged.equals(date) || isRepeating != repeatchanged){
                    saveTaskAlert(getContext());
                }
                else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    binding.date.setText(formattedDate);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    Calendar selectedTime = Calendar.getInstance(); // Create a new Calendar instance
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);

                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    String formattedTime = sdf.format(selectedTime.getTime()); // Format the Date

                    binding.time.setText(formattedTime.toUpperCase());
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                false // Set to true for 24-hour format
        );
        timePickerDialog.show();
    }

    private void saveTaskAlert(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Save Task");
        builder.setMessage("Do you want to save the changes?");
        builder.setPositiveButton("Yes", (dialog, which) -> saveTask());
//        builder.setNegativeButton("No", (dialog, which) -> requireActivity().finish());
        builder.setNegativeButton("No", (dialog, which) -> requireActivity().getSupportFragmentManager().popBackStack());
        builder.show();
    }

    private void saveTask() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d("AddTaskFragment", "No user is logged in. Cannot save task.");
            return;
        }

        String userId = currentUser.getUid();
        String task = binding.taskInput.getText().toString().trim();
        String timeInput = binding.time.getText().toString().trim();
        String dateInput = binding.date.getText().toString().trim();
        boolean isRepeating = binding.repeating.isChecked();
        String time = getTime(timeInput, dateInput);
        String date = getDate(timeInput, dateInput);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        String combinedDateTime = date + " " + time;

        if (task.isEmpty()) {
            binding.taskInput.setError("Task cannot be blank");
        } else {
            if(time.isEmpty() && date.isEmpty()) {
                String taskId = TaskListFragment.editText[0]; // Check if editing an existing task
                if (taskId != null) {
                    // Update the existing task in the user's tasks subcollection
                    firestore.collection("users").document(userId).collection("tasks")
                            .document(taskId)
                            .update("taskName", task, "dueTime", time, "dueDate", date, "isRepeating", isRepeating)
                            .addOnCompleteListener(taskUpdate -> {
                                if (taskUpdate.isSuccessful()) {
                                    Log.d("AddTaskFragment", "Task updated successfully!");
                                    TaskListFragment.editText = new String[]{null, null, null, null};
                                    TaskListFragment.isRepeating = false;
                                } else {
                                    Log.d("AddTaskFragment", "Could not update task!");
                                }
                            });
                } else {
                    // Create a new task in the user's tasks subcollection
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("taskName", task);
                    taskMap.put("taskDone", false);
                    taskMap.put("dueTime", time);
                    taskMap.put("dueDate", date);
                    taskMap.put("isRepeating", isRepeating);

                    firestore.collection("users").document(userId).collection("tasks")
                            .add(taskMap)
                            .addOnCompleteListener(taskAdd -> {
                                if (taskAdd.isSuccessful()) {
                                    Log.d("AddTaskFragment", "Task saved successfully!");
                                } else {
                                    Log.d("AddTaskFragment", "Error saving task!");
                                }
                            });
                }

                // Go back to the previous screen
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                try {
                    Date dueDateTime = dateTimeFormat.parse(combinedDateTime);
                    long dueTimeMillis = dueDateTime.getTime();
                    long currentTimeMillis = System.currentTimeMillis();

                    if (dueTimeMillis <= currentTimeMillis) {
                        // If the due date/time is in the past, show an error and return
                        Toast.makeText(getContext(), "Cannot create task for past date and time!", Toast.LENGTH_SHORT).show();
                        return; // Stop task creation
                    }

                    String taskId = TaskListFragment.editText[0]; // Check if editing an existing task
                    if (taskId != null) {
                        // Update the existing task in the user's tasks subcollection
                        firestore.collection("users").document(userId).collection("tasks")
                                .document(taskId)
                                .update("taskName", task, "dueTime", time, "dueDate", date, "isRepeating", isRepeating)
                                .addOnCompleteListener(taskUpdate -> {
                                    if (taskUpdate.isSuccessful()) {
                                        Log.d("AddTaskFragment", "Task updated successfully!");
                                        TaskListFragment.editText = new String[]{null, null, null, null};
                                        TaskListFragment.isRepeating = false;

                                        ReminderWorker.scheduleReminder(getContext(), taskId, task, time, date, isRepeating);
                                    } else {
                                        Log.d("AddTaskFragment", "Could not update task!");
                                    }
                                });
                    } else {
                        // Create a new task in the user's tasks sub-collection
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("taskName", task);
                        taskMap.put("taskDone", false);
                        taskMap.put("dueTime", time);
                        taskMap.put("dueDate", date);
                        taskMap.put("isRepeating", isRepeating);

                        firestore.collection("users").document(userId).collection("tasks")
                                .add(taskMap)
                                .addOnCompleteListener(taskAdd -> {
                                    if (taskAdd.isSuccessful()) {
                                        String newTaskId = taskAdd.getResult().getId();
                                        Log.d("AddTaskFragment", "Task saved successfully!");
                                        ReminderWorker.scheduleReminder(getContext(), newTaskId, task, time, date, isRepeating);
                                    } else {
                                        Log.d("AddTaskFragment", "Error saving task!");
                                    }
                                });
                    }

                    // Go back to the previous screen
                    requireActivity().getSupportFragmentManager().popBackStack();
                } catch (Exception e) {
                    Log.e("AddTaskFragment", "Error parsing date/time: " + e.getMessage());
                    Toast.makeText(getContext(), "Invalid date/time format!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getTime(String time, String date) {
        if(time.isEmpty() && !date.isEmpty()) {
            return Helper.getUserTime(getContext());
        }
        else {
            return time;
        }
    }

    private String getDate(String time, String date) {
        if(date.isEmpty() && !time.isEmpty()) {
            // gives today's date
            Calendar calendar = Calendar.getInstance();
            Date currentDate = calendar.getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(currentDate);
        }
        else {
            return date;
        }
    }

    private void showKeyboard(EditText editText) {
        // Show the keyboard programmatically
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // To hide menu option
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Clear all menu items in this fragment
        menu.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
