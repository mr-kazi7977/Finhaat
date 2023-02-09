package com.finhaat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import Model.Url;
import ServiceImpl.UrlServiceImpl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    /* access modifiers changed from: private */
    public int STORAGE_PERMISSION_CODE = 1;


    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;

    WebView webView;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Complete");
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            requestStoragePermission();
        }

        webView=findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);

//        webView.loadUrl("https://demo.finhaat.com/mobile/auth/sign-in");

        Call<Url> url = UrlServiceImpl.getUrlservice().getUrl();
        url.enqueue(new Callback<Url>() {
            @Override
            public void onResponse(Call<Url> call, Response<Url> response) {
                Url link=response.body();

                System.out.println(link.getUrl());
//                Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_SHORT).show();
                webView.loadUrl(link.getUrl());


                webView.setWebViewClient(new WebViewClient() {
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        // do your handling codes here, which url is the requested url
                        // probably you need to open that url rather than redirect:
                        if ( url.contains(".pdf")){
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(url), "application/pdf");
                            try{
                                view.getContext().startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                //user does not have a pdf viewer installed
                            }
                        } else {
                            webView.loadUrl(url);
                        }
                        return false; // then it is not handled by default action
                    }


                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                        Log.e("error",description);
                    }


                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {        //show progressbar here

                        super.onPageStarted(view, url, favicon);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //hide progressbar here

                    }

                });
                webView.setWebChromeClient(new WebChromeClient(){
                    // For Android 5.0
                    public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
                        // Double check that we don't have any existing callbacks
                        if (mFilePathCallback != null) {
                            mFilePathCallback.onReceiveValue(null);
                        }
                        mFilePathCallback = filePath;

                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            // Create the File where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                            } catch (IOException ex) {
                                // Error occurred while creating the File
//                        Log.e(Common.TAG, "Unable to create Image File", ex);
                            }

                            // Continue only if the File was successfully created
                            if (photoFile != null) {
                                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                        Uri.fromFile(photoFile));
                            } else {
                                takePictureIntent = null;
                            }
                        }

                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType("image/*");

                        Intent[] intentArray;
                        if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                        return true;

                    }

                    // openFileChooser for Android 3.0+
                    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

                        mUploadMessage = uploadMsg;
                        // Create AndroidExampleFolder at sdcard
                        // Create AndroidExampleFolder at sdcard

                        File imageStorageDir = new File(
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES)
                                , "AndroidExampleFolder");

                        if (!imageStorageDir.exists()) {
                            // Create AndroidExampleFolder at sdcard
                            imageStorageDir.mkdirs();
                        }

                        // Create camera captured image file path and name
                        File file = new File(
                                imageStorageDir + File.separator + "IMG_"
                                        + String.valueOf(System.currentTimeMillis())
                                        + ".jpg");

                        mCapturedImageURI = Uri.fromFile(file);

                        // Camera capture image intent
                        final Intent captureIntent = new Intent(
                                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("image/*");

                        // Create file chooser intent
                        Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                        // Set camera intent to file chooser
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                                , new Parcelable[] { captureIntent });

                        // On select image call onActivityResult method of activity
                        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);


                    }

                    // openFileChooser for Android < 3.0
                    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                        openFileChooser(uploadMsg, "");
                    }

                    //openFileChooser for other Android versions
                    public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                                String acceptType,
                                                String capture) {

                        openFileChooser(uploadMsg, acceptType);
                    }
                });




            }

            @Override
            public void onFailure(Call<Url> call, Throwable t) {

            }
        });



    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            if (requestCode == FILECHOOSER_RESULTCODE) {

                if (null == this.mUploadMessage) {
                    return;

                }

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK) {

                        result = null;

                    } else {

                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;

            }
        }

        return;
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }






    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.camera");
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.CAMERA")) {
            new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Camera permission needed for this Application.").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity mainActivity = MainActivity.this;
                    ActivityCompat.requestPermissions(mainActivity, new String[]{"android.permission.CAMERA"}, mainActivity.STORAGE_PERMISSION_CODE);
                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, this.STORAGE_PERMISSION_CODE);
        }
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i != this.STORAGE_PERMISSION_CODE) {
            return;
        }
        if (iArr.length <= 0 || iArr[0] != 0) {
            Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
        }
    }

}