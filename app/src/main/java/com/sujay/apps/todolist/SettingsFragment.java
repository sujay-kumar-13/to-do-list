package com.sujay.apps.todolist;

import static androidx.core.app.ActivityCompat.recreate;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;

import com.google.firebase.auth.FirebaseAuth;
import com.sujay.apps.todolist.databinding.FragmentSettingsBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.currUser.setText(Helper.getUser(requireContext()));

        binding.time.setOnClickListener(v -> showTimePicker());
        binding.defTime.setText(Helper.getUserTime(requireContext()));

        binding.notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());

                        startActivity(intent);
                    }
                }
            }
        });

        binding.isOn.setText(getNotificationStatus());

        binding.battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBatteryOptimized(requireContext())) {
                    Intent intent = new Intent();
                    // Open the Battery Optimization settings page
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);

                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Battery settings not accessible!", Toast.LENGTH_SHORT).show();
                        Log.e("SettingsFragment", "Error opening battery settings", e);
                    }
                }
//                binding.optimized.setText(isBatteryOptimized(requireContext()) ? "Not Disabled" : "Disabled");
            }
        });

        binding.optimized.setText(isBatteryOptimized(requireContext()) ? "Not Disabled" : "Disabled");

        // Theme selection
        binding.theme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemeSelectionDialog(getContext());
            }
        });
        binding.currTheme.setText(getThemeName());

        // Logout
        binding.logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();

                // Reset navigation to start destination (LoginFragment)
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.action_SettingsFragment_to_LoginFragment);
                NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
                navGraph.setStartDestination(R.id.LoginFragment);
                navController.setGraph(navGraph);
                ThemeUtils.setTheme(requireContext(), "system");

                Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimePicker() {
        // Retrieve the saved time from Helper class
        String savedTime = Helper.getUserTime(requireContext());

        // Default values (current time)
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // If there is a saved time, parse it
        if (savedTime != null && !savedTime.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date date = sdf.parse(savedTime);
                if (date != null) {
                    calendar.setTime(date);
                    hour = calendar.get(Calendar.HOUR_OF_DAY);
                    minute = calendar.get(Calendar.MINUTE);
                }
            } catch (Exception e) {
//                e.printStackTrace();
                Log.e("SettingFragment", "Saved Time is null", e);
            }
        }

        // Open TimePicker with saved time as the default selection
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfDay) -> {
                    // Update the selected time
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minuteOfDay);

                    // Format and save the time
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    String formattedTime = sdf.format(calendar.getTime()).toUpperCase();

                    Helper.saveUserTime(requireContext(), formattedTime);
                    binding.defTime.setText(formattedTime);
                },
                hour,
                minute,
                false // Set to true for 24-hour format
        );
        timePickerDialog.show();
    }


    private boolean isBatteryOptimized(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return false;
    }


    private String getNotificationStatus() {
        String status = "Off";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    status = "Off";
            }
            else {
                status = "On";
            }
        }

        return status;
    }

    private void showThemeSelectionDialog(Context context) {
        String[] themes = {"Light", "Dark", "System Default"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Theme");

        // Preselect the current theme
        String currentTheme = ThemeUtils.getTheme(context);
        int checkedItem = "light".equals(currentTheme) ? 0 : "dark".equals(currentTheme) ? 1 : 2;

        builder.setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
            // Save selected theme
            switch (which) {
                case 0:
                    ThemeUtils.setTheme(context, "light");
                    break;
                case 1:
                    ThemeUtils.setTheme(context, "dark");
                    break;
                case 2:
                    ThemeUtils.setTheme(context, "system");
                    break;
            }

            // Recreate activity to apply theme
            requireActivity().recreate();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getThemeName() {
        String[] themes = {"Light", "Dark", "System Default"};
        String theme = ThemeUtils.getTheme(requireContext());
        switch (theme) {
            case "light":
                return themes[0];
            case "dark":
                return themes[1];
            default:
                return themes[2];
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the text when returning to the fragment
        binding.optimized.setText(isBatteryOptimized(requireContext()) ? "Not Disabled" : "Disabled");
        binding.isOn.setText(getNotificationStatus());
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
