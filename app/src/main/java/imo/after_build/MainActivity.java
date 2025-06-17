package imo.after_build;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity 
{
    String output = "nope";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView textview = new TextView(this);

        receiveApk(this);

        textview.setText(output);
        setContentView(textview);
    }

    public boolean receiveApk(Context mContext) {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if(! Intent.ACTION_VIEW.equals(action)) return false;
        if(! "application/vnd.android.package-archive".equals(type)) return false;
        Uri apkUri = intent.getData();
        if(apkUri == null) return false;
        output = getApkPackageName(mContext, apkUri);
        return true;
    }


    public static String getApkPackageName(Context context, Uri apkUri) {
        File tempFile = null;
        try {
            tempFile = copyToTempCacheFile(context, apkUri);
            if (tempFile == null) return null;

            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), 0);

            if (packageInfo == null) return null;
            
            return packageInfo.packageName;

        } catch (Exception e) {}

        if (tempFile != null && tempFile.exists()) 
            tempFile.delete();
        return null;
    }

    private static File copyToTempCacheFile(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), "temp_apk.apk");

            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            inputStream.close();
            return tempFile;

        } catch (Exception e) {
            return null;
        }
    }
}
