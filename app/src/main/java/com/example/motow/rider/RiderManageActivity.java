package com.example.motow.rider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.LoginActivity;
import com.example.motow.UserInfoActivity;
import com.example.motow.databinding.ActivityRiderManageBinding;
import com.example.motow.vehicles.ManageVehicleActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class RiderManageActivity extends AppCompatActivity {

    private ActivityRiderManageBinding binding;

    // Firebase
    private FirebaseFirestore fStore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRiderManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getUid();

        loadUserDetails();
        setListeners();
    }

    private void loadUserDetails() {
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.riderName.setText(documentSnapshot.getString("name"));
                    byte[] bytes = Base64.decode(documentSnapshot.getString("pfp"), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.pfp.setImageBitmap(bitmap);
                });
    }

    private void setListeners() {
        binding.logoutBtn.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Navbar
        binding.homeBtn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RiderActivity.class));
            finish();
        });

        // Manage interface
        binding.personalInfo.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), UserInfoActivity.class));
            finish();
        });
        binding.manageVehicles.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
            finish();
        });
        binding.deleteAccount.setOnClickListener(view ->
                deleteAccount());
        binding.changePfpBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.pfp.setImageBitmap(bitmap);
                            String encodedImage = encodeImage(bitmap);
                            HashMap<String, Object> userInfo = new HashMap<>();
                            userInfo.put("pfp", encodedImage);
                            fStore.collection("Users")
                                    .document(userId)
                                    .update(userInfo);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void deleteAccount() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("This account will deleted from the system and it is irreversible.");
        alert.setPositiveButton("YES", (dialogInterface, i) -> {
            // Delete account
            FirebaseAuth fAuth = FirebaseAuth.getInstance();
            FirebaseUser user = fAuth.getCurrentUser();

            HashMap<String, Object> delete = new HashMap<>();
            delete.put("isDeleted", "1");

            fStore.collection("Users")
                    .document(userId)
                    .update(delete);

            Objects.requireNonNull(user).delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account has been deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        });
        alert.setNegativeButton("NO", (dialogInterface, i) -> {
            //
        });
        alert.create().show();
    }
}