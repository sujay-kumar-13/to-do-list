package com.sujay.apps.todolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sujay.apps.todolist.databinding.FragmentTaskListBinding;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class TaskListFragment extends Fragment {

    protected static String[] editText = {null, null, null, null};
    protected static boolean isRepeating = false;
    private FragmentTaskListBinding binding;
    private LinearLayout taskContainer, completedContainer;
    private LayoutInflater inflater1;
    private FirebaseFirestore firestoreDb; // Firestore database instance
    private HashSet<String> selectedTasks = new HashSet<>();
    private Map<String, View> taskRowMap = new HashMap<>();
    private Map<String, View> completedRowMap = new HashMap<>();
    private boolean isSelectionMode = false; // Track selection mode
    private int selectedCount = 0;
    int taskCount = 0;
    int completeCount = 0;
    private SwipeRefreshLayout swipeRefreshLayout;

    // LocalBroadcastManager Receiver
    private final BroadcastReceiver taskUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("TaskListFragment", "Received TASKS_UPDATED broadcast. Refreshing UI...");
//            swipeRefreshLayout.setRefreshing(true);
            getTasks(); // Refresh task list
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        swipeRefreshLayout = binding.swipeRefreshLayout;

        // Set refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            getTasks(); // Reload the tasks from Firestore
            swipeRefreshLayout.setRefreshing(false); // Stop the loading animation
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Override back press behavior
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if any tasks are selected
                if (isSelectionMode) {
                    exitSelectionMode(); // Deselect tasks
                }
                else {
                    // Closes the app on single press
                    requireActivity().finish();

                    // Closes the app on two presses
//                    if (backPressCount == 0) {
//                        // Toast.makeText(getContext(), "Press back again to exit.", Toast.LENGTH_SHORT).show()
//                        backPressCount++
//                        Handler(Looper.getMainLooper()).postDelayed({
//                                backPressCount = 0
//                        }, 2000) // Reset counter after 2 seconds
//                    } else {
//                        backPressCount = 0
//                        requireActivity().finish() // Finish the Activity
//                    }
                }
            }
        });

        // Initialize UI components and variables
        taskContainer = binding.taskContainer;
        completedContainer = binding.completedContainer;

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText[0] = null;
                editText[1] = null;
                isRepeating = false;
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.action_TaskListFragment_to_AddTaskFragment);
            }
        });

        inflater1 = LayoutInflater.from(getContext());

        // Initialize Firestore
        firestoreDb = FirebaseFirestore.getInstance();

        // Set listener for swipe-to-refresh
        // Reload tasks from Firebase
        binding.swipeRefreshLayout.setOnRefreshListener(this::getTasks);

        // Load tasks from Firestore on start
        Log.d("TaskListFragment", "onViewCreated: Loading tasks");
        getTasks();

        // Set click listener for delete button
        binding.delete.setOnClickListener(v -> deleteSelectedTasks());

        // Find the Select All checkbox
        CheckBox selectAllCheckBox = binding.getRoot().findViewById(R.id.selectAllcb);

        // Set listener for the Select All checkbox
        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enterSelectionMode();
                toggleSelectAllTasks(true); // Select all tasks
            } else {
                toggleSelectAllTasks(false); // Deselect all tasks
            }
        });

        // Register Local BroadcastReceiver to refresh UI
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(taskUpdateReceiver,
                new IntentFilter("TASKS_UPDATED"));
    }

    private void getTasks() {
//        swipeRefreshLayout.setRefreshing(true); // Show loading indicator
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            swipeRefreshLayout.setRefreshing(false);
            Log.d("TaskListFragment", "No user is logged in. Cannot retrieve tasks.");
            return;
        }

        String userId = currentUser.getUid();

        firestoreDb.collection("users").document(userId).collection("tasks")
                .addSnapshotListener((querySnapshot, e) -> {
                    swipeRefreshLayout.setRefreshing(false); // Hide loading indicator
                    if (e != null) {
                        Log.w("TaskListFragment", "Listen failed.", e);
                        return;
                    }
                    if (querySnapshot != null) {
                        taskCount = 0;
                        completeCount = 0;
                        taskContainer.removeAllViews(); // Clear existing tasks
                        taskRowMap.clear(); // Clear previous mappings
                        completedContainer.removeAllViews();
                        completedRowMap.clear();

//                        for (QueryDocumentSnapshot document : task.getResult()) {
                        for(DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String taskId = document.getId(); // Get Firestore document ID
                            String taskName = document.getString("taskName");
                            boolean done = document.getBoolean("taskDone");
                            String time = document.getString("dueTime");
                            String date = document.getString("dueDate");
                            boolean repeat = Boolean.TRUE.equals(document.getBoolean("isRepeating"));

                            final View taskRow = inflater1.inflate(R.layout.task_row, null);
                            TextView text = taskRow.findViewById(R.id.task);
                            CheckBox box = taskRow.findViewById(R.id.cb);
                            text.setText(taskName);
                            box.setChecked(done);

                            if (!done) {
                                taskRowMap.put(taskId, taskRow);
                                taskRowClickListener(taskRow, taskId, time, date, repeat);
                                taskCount++;
                                textColorChange(text, date, time);
                                taskContainer.addView(taskRow);
                            } else {
                                completedRowMap.put(taskId, taskRow);
                                completedRowClickListener(taskRow, taskId);
                                completeCount++;
                                completedContainer.addView(taskRow);
                            }

                            box.setOnCheckedChangeListener((buttonView, isChecked) -> updateCompleted(taskId, taskRow, isChecked));
                        }
                        taskEmpty();
                        completedEmpty();
                    } else {
                        TextView error = binding.taskContainer.findViewById(R.id.text0);
                        error.setText("Error loading tasks");
                        TextView errorc = binding.completedContainer.findViewById(R.id.completedText0);
                        errorc.setText("Error loading tasks");
                        Log.d("TaskListFragment", "Error retrieving tasks: ");
                    }
                });
    }

    private void textColorChange(TextView text, String dueDate, String dueTime) {
        if(dueDate.isEmpty() || dueTime.isEmpty()) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        String combinedDateTime = dueDate + " " + dueTime;

        try {
            Date dueDateTime = sdf.parse(combinedDateTime);
            long dueTimeMillis = dueDateTime.getTime();
            long currentTimeMillis = System.currentTimeMillis();
            if(dueTimeMillis < currentTimeMillis) {
                text.setTextColor(ContextCompat.getColor(getContext(), R.color.overdue_text));
            }
        }
        catch (ParseException e) {
            Log.e("TaskListFragment", "Due Date/Time is empty", e);
        }
        catch (NullPointerException e) {
            Log.e("TaskListFragment", "Color can not be changed", e);
        }
    }

    private void clearCompletedTasks() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (completedRowMap.isEmpty()) {
            Toast.makeText(getContext(), "No completed tasks to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String taskId : completedRowMap.keySet()) {
            firestoreDb.collection("users").document(userId).collection("tasks").document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d("TaskListFragment", "Completed task deleted: " + taskId))
                    .addOnFailureListener(e -> Log.e("TaskListFragment", "Error deleting completed task: " + taskId, e));
        }

        // Clear the completed container and mapping
        completedContainer.removeAllViews();
        completedRowMap.clear();
        completeCount = 0;

        completedEmpty(); // Update empty message for completed tasks
        Toast.makeText(getContext(), "Completed tasks cleared", Toast.LENGTH_SHORT).show();
    }

    private void taskRowClickListener(View taskRow, String taskId, String time, String date, boolean repeat) {
        taskRow.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                enterSelectionMode();
            }

            toggleTaskSelection(taskId, taskRow);
            return true;
        });

        // Handle individual task click in selection mode
        taskRow.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleTaskSelection(taskId, taskRow);
            }
            else {
//                editTask(taskId);
                editText[0] = taskId;
                TextView text = taskRow.findViewById(R.id.task);
                editText[1] = text.getText().toString().trim();
                editText[2] = time;
                editText[3] = date;
                isRepeating = repeat;

                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.action_TaskListFragment_to_AddTaskFragment);
            }
        });
    }

    private void completedRowClickListener(View taskRow, String taskId) {
        taskRow.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                enterSelectionMode();
            }

            toggleTaskSelection(taskId, taskRow);
            return true;
        });

        // Handle individual task click in selection mode
        taskRow.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleTaskSelection(taskId, taskRow);
            }
        });
    }

    private void taskEmpty() {
        // Remove any existing empty message
        View emptyMessage = taskContainer.findViewWithTag("empty_task_message");
        if (emptyMessage != null) {
            taskContainer.removeView(emptyMessage);
        }

        // Add the empty message if no tasks exist
        if (taskCount == 0) {
            View taskRow = inflater1.inflate(R.layout.task_row, null);
            TextView text = taskRow.findViewById(R.id.task);
            CheckBox box = taskRow.findViewById(R.id.cb);

            // Configure empty message row
            text.setText("You have no tasks");
            text.setTextColor(Color.GRAY);
            taskRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background));
            box.setVisibility(View.GONE); // Hide the checkbox for empty message
            taskRow.setTag("empty_task_message"); // Add a tag to identify this row

            taskContainer.addView(taskRow);
        }
    }

    private void completedEmpty() {
        // Remove any existing empty message
        View emptyMessage = completedContainer.findViewWithTag("empty_completed_message");
        if (emptyMessage != null) {
            completedContainer.removeView(emptyMessage);
        }

        // Add the empty message if no completed tasks exist
        if (completeCount == 0) {
            View taskRow = inflater1.inflate(R.layout.task_row, null);
            TextView text = taskRow.findViewById(R.id.task);
            CheckBox box = taskRow.findViewById(R.id.cb);

            // Configure empty message row
            text.setText("You have not completed any task yet");
            text.setTextColor(Color.GRAY);
            taskRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background));
            box.setVisibility(View.GONE); // Hide the checkbox for empty message
            taskRow.setTag("empty_completed_message"); // Add a tag to identify this row

            completedContainer.addView(taskRow);
        }
    }

    private void createNextTask(String userId, DocumentSnapshot oldTask) {
        // Get old task details
        String taskTitle = oldTask.getString("taskName");
        String time = oldTask.getString("dueTime");
        String date = oldTask.getString("dueDate");
        String nextDate;

        // Calculate the next day's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try{
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            nextDate = dateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            Log.e("TaskListFragment", "Error adding date", e);
            nextDate = date;
        }

        // Create new task data
        Map<String, Object> newTask = new HashMap<>();
        newTask.put("taskName", taskTitle);
        newTask.put("dueTime", time);
        newTask.put("dueDate", nextDate);
        newTask.put("taskDone", false);
        newTask.put("isRepeating", true);

        // Save the new task in Firestore
        String finalNextDate = nextDate;
        firestoreDb.collection("users").document(userId).collection("tasks")
                .add(newTask)
                .addOnCompleteListener(taskAdd -> {
                    String newTaskId = taskAdd.getResult().getId();
                    Log.d("TaskListFragment", "New repeating task created for next day");
                    ReminderWorker.scheduleReminder(getContext(), newTaskId, taskTitle, time, finalNextDate, true);
                })
                .addOnFailureListener(e -> Log.e("TaskListFragment", "Failed to create next repeating task", e));
    }


    protected void updateCompleted(String taskId, View taskRow, boolean isChecked) {
        if (!isSelectionMode) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (userId == null) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference taskRef = firestoreDb.collection("users").document(userId)
                    .collection("tasks").document(taskId);

            taskRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {

                    boolean repeatDaily = Boolean.TRUE.equals(documentSnapshot.getBoolean("isRepeating"));

                    // Mark current task as done
                    taskRef.update("taskDone", isChecked, "isRepeating", false).addOnSuccessListener(aVoid -> {
                        ReminderWorker.cancelReminder(getContext(), taskId);
                        Log.d("TaskListFragment", "Task status updated successfully");

                        // Cancel notification when task is marked as done
                        NotificationHelper notificationHelper = new NotificationHelper(getContext());
                        notificationHelper.cancelNotification(taskId);

                        if (repeatDaily && isChecked) {
                            createNextTask(userId, documentSnapshot);
                        }
                    }).addOnFailureListener(e -> {
                        CheckBox checkBox = taskRow.findViewById(R.id.cb);
                        checkBox.setChecked(!isChecked);
                        Toast.makeText(getContext(), "Failed to update task status", Toast.LENGTH_SHORT).show();
                        Log.e("TaskListFragment", "Error updating task completion", e);
                    });
                }
            });
        } else {
            CheckBox cb = taskRow.findViewById(R.id.cb);
            cb.setChecked(!isChecked);
            taskRow.performClick();
        }
    }

    private void selectedCountUpdate() {
        TextView selected = binding.count;
        if (selectedCount > 0) {
            selected.setVisibility(View.VISIBLE);
            selected.setText(selectedCount + " selected");
        } else {
            selected.setVisibility(View.INVISIBLE);
        }
    }

    private void toggleSelectAllTasks(boolean selectAll) {
        if (selectAll) {
            // Select all tasks (both incomplete and completed)
            selectedCount = taskRowMap.size() + completedRowMap.size();

            // Iterate through incomplete tasks
            for (Map.Entry<String, View> entry : taskRowMap.entrySet()) {
                String taskId = entry.getKey();
                View taskRow = entry.getValue();

                if (!selectedTasks.contains(taskId)) { // Only add if not already selected
                    selectedTasks.add(taskId);
                    updateTaskSelectionUI(taskRow, true);
                }
            }

            // Iterate through completed tasks
            for (Map.Entry<String, View> entry : completedRowMap.entrySet()) {
                String taskId = entry.getKey();
                View taskRow = entry.getValue();

                if (!selectedTasks.contains(taskId)) { // Only add if not already selected
                    selectedTasks.add(taskId);
                    updateTaskSelectionUI(taskRow, true);
                }
            }
        } else {
            // Deselect all tasks (both incomplete and completed)
            selectedCount = 0;

            // Iterate through incomplete tasks
            for (Map.Entry<String, View> entry : taskRowMap.entrySet()) {
                String taskId = entry.getKey();
                View taskRow = entry.getValue();

                selectedTasks.remove(taskId);
                updateTaskSelectionUI(taskRow, false);
            }

            // Iterate through completed tasks
            for (Map.Entry<String, View> entry : completedRowMap.entrySet()) {
                String taskId = entry.getKey();
                View taskRow = entry.getValue();

                selectedTasks.remove(taskId);
                updateTaskSelectionUI(taskRow, false);
            }
            exitSelectionMode();
        }

        // Update UI after toggle
        selectedCountUpdate();           // Update the count display
        updateDeleteButtonVisibility();  // Show/hide delete button based on selection
        updateSelectAllCheckboxState();  // Update the state of the "Select All" checkbox
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        swipeRefreshLayout.setEnabled(false);
    }

    private void toggleTaskSelection(String taskId, View taskRow) {
        if (selectedTasks.contains(taskId)) {
            selectedTasks.remove(taskId);
            selectedCount--;
            updateTaskSelectionUI(taskRow, false);
        } else {
            selectedTasks.add(taskId);
            selectedCount++;
            updateTaskSelectionUI(taskRow, true);
        }

        selectedCountUpdate();
        updateDeleteButtonVisibility();
        updateSelectAllCheckboxState();

        if (selectedTasks.isEmpty()) {
            exitSelectionMode(); // Exit selection mode if no tasks are selected
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        swipeRefreshLayout.setEnabled(true);
        selectedTasks.clear();
        selectedCount = 0;
        binding.selectAllcb.setChecked(false);

        for (View taskRow : taskRowMap.values()) {
            updateTaskSelectionUI(taskRow, false); // Remove highlights
        }
        for (View taskRow : completedRowMap.values()) {
            updateTaskSelectionUI(taskRow, false); // Remove highlights
        }

        updateDeleteButtonVisibility();
        updateSelectAllCheckboxState();
        selectedCountUpdate();
    }

    private void deleteSelectedTasks() {
        if (selectedTasks.isEmpty()) {
            Toast.makeText(getContext(), "No tasks selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String taskId : selectedTasks) {
            firestoreDb.collection("users").document(userId).collection("tasks").document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TaskListFragment", "Task deleted: " + taskId);
                        ReminderWorker.cancelReminder(getContext(), taskId);
                    })
                    .addOnFailureListener(e -> Log.e("TaskListFragment", "Error deleting task: " + taskId, e));
        }

        // Refresh tasks and exit selection mode
        exitSelectionMode();
        getTasks();

        Toast.makeText(getContext(), "Selected tasks deleted", Toast.LENGTH_SHORT).show();
    }

    private void updateTaskSelectionUI(View taskRow, boolean isSelected) {
        taskRow.setSelected(isSelected);
//        if (isSelected) {
////            taskRow.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
//            taskRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.task_selected));
//
//        } else {
////            taskRow.setBackgroundColor(getResources().getColor(android.R.color.transparent));
//            taskRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray));
//        }
    }

    private void updateSelectAllCheckboxState() {
        CheckBox selectAllCheckBox = binding.selectAllcb;
        boolean allSelected = selectedTasks.size() == taskRowMap.size() + completedRowMap.size();
        selectAllCheckBox.setOnCheckedChangeListener(null); // Remove listener temporarily
        selectAllCheckBox.setChecked(allSelected);
        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                toggleSelectAllTasks(true);
            } else {
                toggleSelectAllTasks(false);
            }
        });
    }

    private void updateDeleteButtonVisibility() {
        if (selectedTasks.isEmpty()) {
            isSelectionMode = false;
            binding.linearLayout.setVisibility(View.INVISIBLE);
            binding.delete.setVisibility(View.INVISIBLE);
        } else {
            binding.linearLayout.setVisibility(View.VISIBLE);
            binding.delete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_history) {
            clearCompletedTasks(); // Call your function
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        isSelectionMode = false;
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(taskUpdateReceiver);
    }
}