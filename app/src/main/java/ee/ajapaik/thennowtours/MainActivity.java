package ee.ajapaik.thennowtours;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_CHOOSER_RESULT_CODE = 12345;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private Uri mPicUri;
    private String mCameraPhotoPath;
    private WebView mWebView;

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    public class GeoPhotoWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        // For Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
            Log.d("DEBUG", "openFileChooser");
            mUploadMessage = uploadFile;
            Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
            Calendar cal = Calendar.getInstance();
            File photo = new File(Environment.getExternalStorageDirectory(), (cal.getTimeInMillis() + ".jpg"));
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
            mPicUri = Uri.fromFile(photo);
            startActivityForResult(cameraIntent, FILE_CHOOSER_RESULT_CODE);
        }

        // For Android 5.0+
        @Override
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    Calendar cal = Calendar.getInstance();
                    photoFile = new File(Environment.getExternalStorageDirectory(), (cal.getTimeInMillis() + ".jpg"));
                } catch (Exception ex) {
                    // Error occurred while creating the File
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            // Set up the intent to get an existing image
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            // Set up the intents for the Intent chooser
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

            startActivityForResult(chooserIntent, MainActivity.FILE_CHOOSER_RESULT_CODE);

            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (requestCode != FILE_CHOOSER_RESULT_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }

            Uri[] results = null;
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                String dataString = null;
                try {
                    dataString = intent.getDataString();
                } catch (Exception e) {
                    // Good for us
                }
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        } else {
            Log.d("DEBUG", "HERE");
            if (requestCode != FILE_CHOOSER_RESULT_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }

            if (null == this.mUploadMessage) {
                return;
            }

            Uri result;
            if (resultCode != Activity.RESULT_OK) {
                result = null;
            } else {
                result = intent == null ? this.mPicUri : intent.getData(); // retrieve from the private variable if the intent is null
            }

            this.mUploadMessage.onReceiveValue(result);
            this.mUploadMessage = null;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mWebView = (WebView) findViewById(R.id.mainWebView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        mWebView.setWebChromeClient(new GeoPhotoWebChromeClient());
        mWebView.setWebViewClient(new MyCustomWebViewClient());
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.loadUrl("http://staging.ajapaik.ee/then-and-now-tours/");
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}