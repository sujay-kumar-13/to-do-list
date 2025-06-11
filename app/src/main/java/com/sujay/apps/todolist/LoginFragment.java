package com.sujay.apps.todolist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sujay.apps.todolist.databinding.FragmentLoginBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    AtomicBoolean onSignup = new AtomicBoolean(false);

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
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Remove the back arrow
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        }

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        EditText passwordInput = binding.passwordInput;
        passwordInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEnd = passwordInput.getCompoundDrawables()[2] != null ?
                        passwordInput.getCompoundDrawables()[2].getBounds().width() : 0;
                if (event.getRawX() >= (passwordInput.getRight() - drawableEnd - passwordInput.getPaddingEnd())) {
                    if (passwordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0);
                    } else {
                        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
                    }
                    passwordInput.setSelection(passwordInput.getText().length());

                    // Call performClick() for accessibility
                    v.performClick();
                    return true;
                }
            }
            return false;
        });

        EditText cnfPasswordInput = binding.confirmPasswordInput;
        cnfPasswordInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEnd = cnfPasswordInput.getCompoundDrawables()[2] != null ?
                        cnfPasswordInput.getCompoundDrawables()[2].getBounds().width() : 0;
                if (event.getRawX() >= (cnfPasswordInput.getRight() - drawableEnd - cnfPasswordInput.getPaddingEnd())) {
                    if (cnfPasswordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        cnfPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        cnfPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0);
                    } else {
                        cnfPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        cnfPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
                    }
                    cnfPasswordInput.setSelection(cnfPasswordInput.getText().length());

                    // Call performClick() for accessibility
                    v.performClick();
                    return true;
                }
            }
            return false;
        });

        Button login = binding.login;
        TextView signup = binding.signup;

        // Login button click listener
        login.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString().trim();
            String password = binding.passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
//                binding.emailInput.setError("Email can not be empty!");
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                if(onSignup.get()) {
//                    onSignup.set(false);
                    String cnfPassword = binding.confirmPasswordInput.getText().toString().trim();
                    if(password.equals(cnfPassword)) {
                        signUpUser(email, password);
                    }
                    else {
                        Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    loginUser(email, password);
                }
            }
        });

        // Sign-up button click listener
        signup.setOnClickListener(v -> {
            onSignup.set(!onSignup.get());
            if(onSignup.get()){
                binding.emailInput.setText("");
                binding.passwordInput.setText("");
                login.setText("Sign Up");
//                binding.newUserSignUp.setVisibility(View.GONE);
                binding.signinMessage.setText("Create Your Account");
                binding.back.setVisibility(View.VISIBLE);
                binding.forgotpw.setVisibility(View.GONE);
                binding.confirmPasswordInput.setVisibility(View.VISIBLE);
                binding.newUser.setText("Existing User");
                binding.signup.setText("Sign In");
            }
            else {
                binding.back.performClick();
            }
        });

        binding.back.setOnClickListener(v -> {
            onSignup.set(false);
            binding.emailInput.setText("");
            binding.passwordInput.setText("");
            login.setText("Sign In");
//            binding.newUserSignUp.setVisibility(View.VISIBLE);
            binding.signinMessage.setText("Hello, Sign in");
            binding.back.setVisibility(View.GONE);
            binding.forgotpw.setVisibility(View.VISIBLE);
            binding.confirmPasswordInput.setVisibility(View.GONE);
            binding.newUser.setText("New User");
            binding.signup.setText("Sign Up");
        });

        binding.forgotpw.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Forgot Password", Toast.LENGTH_SHORT).show();
            showForgotPasswordDialog();
        });
    }
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reset Password");

        // Create an input field
        final EditText emailInput = new EditText(getContext());
        emailInput.setHint("Enter your registered email");
        builder.setView(emailInput);

        // Add dialog buttons
        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(getContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Could not send reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            navigateToTaskFragment();
                        }
                        Helper.saveUser(requireContext(), email);
                    } else {
                        Toast.makeText(getContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUpUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {
                    if (onSignup.get()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            createUserInFirestore(user.getUid(), email);
                        }
                        onSignup.set(false);
                        Helper.saveUser(requireContext(), email);
                    } else {
                        Toast.makeText(getContext(), "Sign-up failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthWeakPasswordException) {
                        Toast.makeText(getContext(), "Password requires at least 6 characters", Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(getContext(), "Email already in use", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error: 404", Toast.LENGTH_LONG).show();
                        Log.e("LoginFragment", "Error : 404", e);
                    }
                });
    }

    private void createUserInFirestore(String userId, String email) {
        // Create a user document in Firestore
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);

        firestore.collection("users").document(userId).set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                        navigateToTaskFragment();
                    } else {
                        Toast.makeText(getContext(), "Failed to create user in Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToTaskFragment() {
        NavHostFragment.findNavController(LoginFragment.this)
                .navigate(R.id.action_LoginFragment_to_TaskListFragment);
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
