package com.luanvan.shipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.jwt.Claim;
import com.auth0.android.jwt.JWT;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.luanvan.shipper.components.RequestCode;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;

public class ManagerProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private CircleImageView ivProfile, ivChangePhoto;
    private TextView tvSlotFront, tvSlotBack;
    private ImageView ivSlotFront, ivSlotBack;
    private ImageView ivRemoveImageFront, ivRemoveImageBack;
    private TextView tvSave;
    private ProgressBar progressBar;
    private RelativeLayout layoutProgressBar;
    private EditText etFirstName, etLastName, etPhone, etEmail, etGender, etDOB, etAddress, etDriverLicense,etIdCardNumber;

    private int requestCode;

    private Uri uriSavedImageFront, uriSavedImageBack;
    private Uri uriProfile;

    private StorageReference storageRef;
    private StorageReference shipperImagesRef;

    private Intent intentChooser;

    private Calendar calendar = null;
    private DatePickerDialog.OnDateSetListener onDateSetListener = null;

    public static int minWidthQuality = 300;
    private static String TEMP_IMAGE_NAME;
    private static String TEMP_IMAGE_NAME1;
    private static String TEMP_IMAGE_NAME2;
    private static String TEMP_IMAGE_NAME_PROFILE;

    private String token;
    private int shipperId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String dayOfBirth;
    private String gender;
    private String email;
    private String driverLicense;
    private String idCardNumber;
    private String idCardFront;
    private String idCardBack;
    private String profileImage;

    public final String TAG = "ManagerProfileActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_profile);

        ivProfile = findViewById(R.id.ivProfile);
        ivChangePhoto = findViewById(R.id.ivChangePhoto);
        tvSlotFront = findViewById(R.id.tvSlotFront);
        tvSlotBack = findViewById(R.id.tvSlotBack);
        ivSlotFront = findViewById(R.id.ivSlotFront);
        ivSlotBack = findViewById(R.id.ivSlotBack);
        ivRemoveImageFront = findViewById(R.id.ivRemoveImageFront);
        ivRemoveImageBack = findViewById(R.id.ivRemoveImageBack);
        tvSave = findViewById(R.id.tvSave);
        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etDOB = findViewById(R.id.etDOB);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etGender = findViewById(R.id.etGender);
        etAddress = findViewById(R.id.etAddress);
        etDriverLicense = findViewById(R.id.etDriverLicense);
        etIdCardNumber = findViewById(R.id.etIdCardNumber);

        shipperId = getIntent().getIntExtra("id", -1);
        username = getIntent().getStringExtra("username");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        dayOfBirth = getIntent().getStringExtra("dayOfBirth");
        gender = getIntent().getStringExtra("gender");
        email = getIntent().getStringExtra("email");
        driverLicense = getIntent().getStringExtra("driverLicense");
        idCardNumber = getIntent().getStringExtra("idCardNumber");
        idCardFront = getIntent().getStringExtra("idCardFront");
        idCardBack = getIntent().getStringExtra("idCardBack");
        profileImage = getIntent().getStringExtra("profileImage");

        // set Profile image
        ivProfile.setImageResource(R.drawable.ic_person_24);
        ivProfile.setCircleBackgroundColorResource(R.color.light_gray);
//        RequestOptions options = new RequestOptions()
//                .centerCrop()
//                .placeholder(R.drawable.ic_person_24)
//                .error(R.drawable.ic_person_24);
//        Glide.with(this).load(profileImage).apply(options).into(ivProfile);

        ivChangePhoto.setImageResource(R.drawable.ic_edit_20);
        ivChangePhoto.setCircleBackgroundColorResource(R.color.colorPrimary);
        ivChangePhoto.setPadding(5,5,5,5);

        etFirstName.setText(firstName);
        etLastName.setText(lastName);
        etPhone.setText(phoneNumber);
        etEmail.setText(email);
        etGender.setText(gender);
        etDriverLicense.setText(driverLicense);
        etIdCardNumber.setText(idCardNumber);

//        try {
//            Glide.with(this).load(idCardFront).into(ivSlotFront);
//            Glide.with(this).load(idCardBack).into(ivSlotBack);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
        /////////////////////////////////////////////////////////////////////////

        SharedPreferences sharedPreferences = getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "")+"";

        /////////////////////////////////////////////////////////////////////////
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        /////////////////////////////////////////////////////////////////////////
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);

        /////////////////////////////////////////////////////////////////////////
        calendar = Calendar.getInstance();

        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateUIDate();
            }

            private void updateUIDate() {
                String dateFormat = "dd/MM/yyyy";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
                etDOB.setText(simpleDateFormat.format(calendar.getTime()));
            }
        };

        /////////////////////////////////////////////////////////////////////////
        tvSlotFront.setOnClickListener(this);
        tvSlotBack.setOnClickListener(this);
        ivRemoveImageFront.setOnClickListener(this);
        ivRemoveImageBack.setOnClickListener(this);
        tvSave.setOnClickListener(this);
        etDOB.setOnClickListener(this);
        ivChangePhoto.setOnClickListener(this);
    }



    private void showProgressBar(RelativeLayout layout, ProgressBar progressBar){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);
        progressBar.setVisibility(View.VISIBLE);
    }
    private void hideProgressBar(ProgressBar progressBar){
        progressBar.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvSlotFront:
                TEMP_IMAGE_NAME = "shipper"+shipperId+"_front.jpg";
                TEMP_IMAGE_NAME1 = "shipper"+shipperId+"_front.jpg";

                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCode.REQUEST_PERMISSION_TO_PICK_PICTURE_1);
                    return;
                }

                intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PICTURE_1);
                break;
            case R.id.tvSlotBack:
                TEMP_IMAGE_NAME = "shipper"+shipperId+"_back.jpg";
                TEMP_IMAGE_NAME2 = "shipper"+shipperId+"_back.jpg";

                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCode.REQUEST_PERMISSION_TO_PICK_PICTURE_2);
                    return;
                }

                intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PICTURE_2);
                break;

            case R.id.ivRemoveImageFront:
                ivSlotFront.setVisibility(View.INVISIBLE);
                ivRemoveImageFront.setVisibility(View.INVISIBLE);
                break;

            case R.id.ivRemoveImageBack:
                ivSlotBack.setVisibility(View.INVISIBLE);
                ivRemoveImageBack.setVisibility(View.INVISIBLE);
                break;

            case R.id.tvSave:
                uploadImage(uriProfile, TEMP_IMAGE_NAME_PROFILE);

                if (ivSlotFront.getDrawable() == null || ivSlotBack.getDrawable() == null){
                    Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.enough_images), Toast.LENGTH_LONG).show();
                } else {
                    uploadImage(uriSavedImageFront, TEMP_IMAGE_NAME1);
                    uploadImage(uriSavedImageBack, TEMP_IMAGE_NAME2);
                }
                break;

            case R.id.etDOB:
                new DatePickerDialog(ManagerProfileActivity.this, onDateSetListener,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                break;

            case R.id.ivChangePhoto:
                TEMP_IMAGE_NAME = "temp_profile_image.jpg";
                TEMP_IMAGE_NAME_PROFILE = "temp_profile_image.jpg";

                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCode.REQUEST_PERMISSION_TO_PICK_PROFILE_IMAGE);
                    return;
                }

                intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PROFILE_IMAGE);
                break;
        }
    }

    public Uri getUriToSaveImage(String imageName){
        // create images dir
        File directory = new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName()+"/images");
        if (!directory.exists()){
            if (directory.mkdirs()){
                Log.i("ManagerProfileActivity", "filePath: "+directory.getAbsolutePath());
            }
        }

        // create image file
        File imagesDir = new File(directory+"/"+imageName);
        if (!imagesDir.exists()){
            try {
                if (imagesDir.createNewFile()){
                    Log.i("ManagerProfileActivity", "file created");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Uri.fromFile(imagesDir);
    }

    public void takePictureFromCamera(int requestCode){
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, RequestCode.REQUEST_TAKE_PICTURE_FROM_CAMERA);
            return;
        }

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        switch (requestCode){
            case RequestCode.REQUEST_TAKE_PICTURE_1_FROM_CAMERA:
                uriSavedImageFront = getUriToSaveImage("shipper"+shipperId+"_front"+".jpg");
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImageFront);
                break;
            case RequestCode.REQUEST_TAKE_PICTURE_2_FROM_CAMERA:
                uriSavedImageBack = getUriToSaveImage("shipper"+shipperId+"_back"+".jpg");
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImageBack);
                break;
        }
        startActivityForResult(cameraIntent, requestCode);
    }
    public void takePictureFromGallery(int requestCode){

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCode.REQUEST_PICK_PICTURE_FROM_GALLERY);
            this.requestCode = requestCode;
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(ManagerProfileActivity.this, "permission denied", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case RequestCode.REQUEST_PICK_PICTURE_1:
                if (resultCode != RESULT_OK) break;

                uriSavedImageFront = getImageUriFromResult(ManagerProfileActivity.this, resultCode, data);
                if (!uriSavedImageFront.getPath().contains(getPackageName()))
                    uriSavedImageFront = Uri.fromFile(new File(getRealPathFromUri(ManagerProfileActivity.this, uriSavedImageFront)));

                Log.i(TAG, "--> "+uriSavedImageFront.getPath());

                ivSlotFront.setImageDrawable(null);
                ivSlotFront.setImageURI(uriSavedImageFront);
                ivSlotFront.setVisibility(View.VISIBLE);

                ivRemoveImageFront.setVisibility(View.VISIBLE);
                break;

            case RequestCode.REQUEST_PICK_PICTURE_2:
                if (resultCode != RESULT_OK) break;

                uriSavedImageBack = getImageUriFromResult(ManagerProfileActivity.this, resultCode, data);
                if (!uriSavedImageBack.getPath().contains(getPackageName()))
                    uriSavedImageBack = Uri.fromFile(new File(getRealPathFromUri(ManagerProfileActivity.this, uriSavedImageBack)));

                Log.i(TAG, "--> "+uriSavedImageBack.getPath());

                ivSlotBack.setImageDrawable(null);
                ivSlotBack.setImageURI(uriSavedImageBack);
                ivSlotBack.setVisibility(View.VISIBLE);

                ivRemoveImageBack.setVisibility(View.VISIBLE);
                break;

            case RequestCode.REQUEST_PICK_PROFILE_IMAGE:
                if (resultCode != RESULT_OK) break;

                uriProfile = getImageUriFromResult(ManagerProfileActivity.this, resultCode, data);
                if (!uriProfile.getPath().contains(getPackageName()))
                    uriProfile = Uri.fromFile(new File(getRealPathFromUri(ManagerProfileActivity.this, uriProfile)));

                Log.i(TAG, "-->profile: "+uriProfile.getPath());

                ivProfile.setImageDrawable(null);
                ivProfile.setImageURI(uriProfile);

                break;
        }
    }

    private boolean copyFile(File src,File dst)throws IOException{
        if (src.getAbsolutePath().equals(dst.getAbsolutePath())) {
            return true;
        } else {
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(dst);
            byte[] buff = new byte[1024];
            int len;
            while ((len = is.read(buff)) > 0) {
                os.write(buff, 0, len);
            }
            is.close();
            os.close();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestCode.REQUEST_PERMISSION_TO_PICK_PICTURE_1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permissions granted", Toast.LENGTH_LONG).show();

                    intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                    startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PICTURE_1);
                } else {
                    Toast.makeText(this, "permissions denied", Toast.LENGTH_LONG).show();
                }
                break;

            case RequestCode.REQUEST_PERMISSION_TO_PICK_PICTURE_2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permissions granted", Toast.LENGTH_LONG).show();

                    intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                    startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PICTURE_2);
                } else {
                    Toast.makeText(this, "permissions denied", Toast.LENGTH_LONG).show();
                }
                break;

            case RequestCode.REQUEST_PERMISSION_TO_PICK_PROFILE_IMAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permissions granted", Toast.LENGTH_LONG).show();

                    intentChooser = getPickImageIntent(ManagerProfileActivity.this);
                    startActivityForResult(intentChooser, RequestCode.REQUEST_PICK_PROFILE_IMAGE);
                } else {
                    Toast.makeText(this, "permissions denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void uploadImage(final Uri uriImage, final String newName){
        showProgressBar(layoutProgressBar, progressBar);

        // location to save images in Firebase
        shipperImagesRef = storageRef.child("images/"+newName);

        UploadTask uploadTask = shipperImagesRef.putFile(uriImage);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ManagerProfileActivity.this, "image uploaded", Toast.LENGTH_SHORT).show();
                getUrl(newName);
                hideProgressBar(progressBar);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideProgressBar(progressBar);
                Toast.makeText(ManagerProfileActivity.this, "Could not uploaded. Error: "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getUrl (final String fileName){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("images");
        StorageReference dateRef = storageRef.child(fileName);
        dateRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
        {
            @Override
            public void onSuccess(Uri downloadUrl) {
                Log.i("ManagerProfileActivity", "imageUrl: "+downloadUrl.toString());
                if (fileName.contains("front")){
                    idCardFront = downloadUrl.toString();
                } else if (fileName.contains("back")){
                    idCardBack = downloadUrl.toString();
                } else {
                    profileImage = downloadUrl.toString();
                }

                // update profile task
                if (idCardFront.length() > 0 && idCardBack.length() > 0 && profileImage.length() > 0){
                    new UpdateProfileTask(shipperId,firstName,lastName,dayOfBirth,gender,phoneNumber,email,driverLicense,idCardNumber,
                            idCardFront,idCardBack,profileImage);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("ManagerProfileActivity", e.getMessage());
            }
        });
    }

    // choose image picker
    @SuppressLint("RestrictedApi")
    public static Intent getPickImageIntent(Context context) {
        Intent chooserIntent = null;

        List<Intent> intentList = new ArrayList<>();

        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        takePhotoIntent.putExtra("return-data", true);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(context)));
        intentList = addIntentsToList(context, intentList, pickIntent);
        intentList = addIntentsToList(context, intentList, takePhotoIntent);

        if (intentList.size() > 0) {
            chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1),
                    context.getString(R.string.pick_image_intent_text));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
        }

        return chooserIntent;
    }
    private static List<Intent> addIntentsToList(Context context, List<Intent> list, Intent intent) {
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            list.add(targetedIntent);
        }
        return list;
    }

    public static Uri getImageUriFromResult(Context context, int resultCode, Intent imageReturnedIntent) {
        Uri selectedImage = null;
        @SuppressLint("RestrictedApi")
        File imageFile = getTempFile(context);
        if (resultCode == Activity.RESULT_OK) {

            boolean isCamera = (imageReturnedIntent == null ||
                    imageReturnedIntent.getData() == null  ||
                    imageReturnedIntent.getData().toString().contains(imageFile.toString()));
            if (isCamera) {     /** CAMERA **/
                selectedImage = Uri.fromFile(imageFile);
                Log.i("ManagerProfileActivity", "uri: "+selectedImage.getPath());
            } else {            /** GALLERY **/
                selectedImage = imageReturnedIntent.getData();
                Log.i("ManagerProfileActivity", "uri: "+selectedImage.getPath());
                Log.i("ManagerProfileActivity", "path: "+getRealPathFromUri(context, selectedImage));
            }
        }
        return selectedImage;
    }
    public static Bitmap getImageFromResult(Context context, int resultCode, Intent imageReturnedIntent) {
        Bitmap bm = null;
        @SuppressLint("RestrictedApi")
        File imageFile = getTempFile(context);
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage;
            boolean isCamera = (imageReturnedIntent == null ||
                    imageReturnedIntent.getData() == null  ||
                    imageReturnedIntent.getData().toString().contains(imageFile.toString()));
            if (isCamera) {     /** CAMERA **/
                selectedImage = Uri.fromFile(imageFile);
            } else {            /** GALLERY **/
                selectedImage = imageReturnedIntent.getData();
                new File(Objects.requireNonNull(selectedImage.getPath()));
            }

            bm = getImageResized(context, selectedImage);
//            int rotation = getRotation(context, selectedImage, isCamera);
//            bm = rotate(bm, rotation);
        }
        return bm;
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static File getTempFile(Context context) {
        File imageFile = new File(Environment.getExternalStorageDirectory()+"/Android/media/"+context.getPackageName()+"/images", TEMP_IMAGE_NAME);
        imageFile.getParentFile().mkdirs();

        return imageFile;
    }

    private static Bitmap decodeBitmap(Context context, Uri theUri, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = context.getContentResolver().openAssetFileDescriptor(theUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap actuallyUsableBitmap = BitmapFactory.decodeFileDescriptor(
                fileDescriptor.getFileDescriptor(), null, options);

        Log.d("test", options.inSampleSize + " sample method bitmap ... " +
                actuallyUsableBitmap.getWidth() + " " + actuallyUsableBitmap.getHeight());

        return actuallyUsableBitmap;
    }

    // Resize to avoid using too much memory loading big images
    private static Bitmap getImageResized(Context context, Uri selectedImage) {
        Bitmap bm = null;
        int[] sampleSizes = new int[]{5, 3, 2, 1};
        int i = 0;
        do {
            bm = decodeBitmap(context, selectedImage, sampleSizes[i]);
            i++;
        } while (bm.getWidth() < minWidthQuality && i < sampleSizes.length);
        return bm;
    }


    private static int getRotation(Context context, Uri imageUri, boolean isCamera) {
        int rotation;
        if (isCamera) {
            rotation = getRotationFromCamera(context, imageUri);
        } else {
            rotation = getRotationFromGallery(context, imageUri);
        }
        return rotation;
    }
    private static int getRotationFromCamera(Context context, Uri imageFile) {
        int rotate = 0;
        try {

            context.getContentResolver().notifyChange(imageFile, null);
            ExifInterface exif = new ExifInterface(imageFile.getPath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }
    public static int getRotationFromGallery(Context context, Uri imageUri) {
        int result = 0;
        String[] columns = {MediaStore.Images.Media.ORIENTATION};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(imageUri, columns, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int orientationColumnIndex = cursor.getColumnIndex(columns[0]);
                result = cursor.getInt(orientationColumnIndex);
            }
        } catch (Exception e) {
            //Do nothing
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }//End of try-catch block
        return result;
    }
    private static Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            return bmOut;
        }
        return bm;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("StaticFieldLeak")
    private class UpdateProfileTask extends AsyncTask<String,String,String> {

        private int shipperId;
        private String firstName;
        private String lastName;
        private String dayOfBirth;
        private String gender;
        private String phoneNumber;
        private String email;
        private String driverLicense;
        private String idCardNumber;
        private String idCardFront, idCardBack;
        private String profileImage;

        private OutputStream os;

        public UpdateProfileTask(int shipperId, String firstName, String lastName, String dayOfBirth, String gender, String phoneNumber, String email, String driverLicense, String idCardNumber, String idCardFront, String idCardBack, String profileImage) {
            this.shipperId = shipperId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.dayOfBirth = dayOfBirth;
            this.gender = gender;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.driverLicense = driverLicense;
            this.idCardNumber = idCardNumber;
            this.idCardFront = idCardFront;
            this.idCardBack = idCardBack;
            this.profileImage = profileImage;
            execute();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            //http post
            try {
                URL url = new URL(RequestUrl.SHIPPER + shipperId);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("firstName", etFirstName.getText().toString());
                jsonObject.put("lastName", etLastName.getText().toString());
                jsonObject.put("dayOfBirth", "1998-08-25");
                jsonObject.put("gender", etGender.getText().toString());
                jsonObject.put("phoneNumber", etPhone.getText().toString());
                jsonObject.put("email", etEmail.getText().toString());
                jsonObject.put("driverLicense", etDriverLicense.getText().toString());
                jsonObject.put("idCardNumber", etIdCardNumber.getText().toString());
                jsonObject.put("idCardFront", idCardFront);
                jsonObject.put("idCardBack", idCardBack);
                jsonObject.put("profileImage", profileImage);
                String data = jsonObject.toString();
                Log.i("json request", data);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Authorization", token);
                connection.setDoOutput(true);
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setFixedLengthStreamingMode(data.getBytes().length);
                connection.connect();

                os = new BufferedOutputStream(connection.getOutputStream());
                os.write(data.getBytes());
                os.flush();

                InputStream is = null;
                int statusCode = connection.getResponseCode();
                Log.i("statusCode", statusCode+"");
                if (statusCode == 200) return "200";
                if (statusCode >= 200 && statusCode < 400){
                    is = connection.getInputStream();
                } else {
                    is = connection.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder buffer = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null){
                    buffer.append(line).append("\n");
                    Log.d("ResponseUpdateProfile: ", "> " + line);
                }
                return buffer.toString();

            } catch (SocketTimeoutException e) {
                Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.socket_timeout), Toast.LENGTH_LONG).show();
            } catch (IOException | JSONException e){
                e.printStackTrace();
            } finally {
                try {
                    if (os!=null) os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (connection != null) connection.disconnect();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.INVISIBLE);

            if (s == null) return;
            if (s.equals("200")){
                Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.update_success), Toast.LENGTH_LONG).show();

                SharedPreferences.Editor editor = getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE).edit();
                editor.putString(Shared.KEY_FIRST_NAME, firstName);
                editor.putString(Shared.KEY_LAST_NAME, lastName);
                editor.putString(Shared.KEY_USERNAME, username);
                editor.apply();
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONObject apierror = jsonObject.getJSONObject("apierror");
                    if (apierror.getString("status").equals("CONFLICT")){
                        if (apierror.getString("debugMessage").contains("phone_number")){
                            Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.conflict_phone), Toast.LENGTH_LONG).show();
                        } else if (apierror.getString("debugMessage").contains("email")){
                            Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.conflict_email), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ManagerProfileActivity.this, getResources().getString(R.string.update_failed), Toast.LENGTH_LONG).show();
                    }
                    setResult(Activity.RESULT_CANCELED);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}