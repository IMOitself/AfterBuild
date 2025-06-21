package imo.after_build;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ApkReceiverActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // storage permission logic from launcher activity
        if(! MainActivity.hasStoragePermission(this)) {
            MainActivity.requestStoragePermission(this);
            finishAffinity();
            return;
        }

        LinearLayout rootLayout = new LinearLayout(this);
        TextView projectFileListText = new TextView(this);
        LinearLayout apkActionLayout = new LinearLayout(this);
        CheckBox addApkCheckbox = new CheckBox(this);
        Button continueInstallBtn = new Button(this);
        rootLayout.addView(projectFileListText);
        apkActionLayout.addView(addApkCheckbox);
        apkActionLayout.addView(continueInstallBtn);
        rootLayout.addView(apkActionLayout);

        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setTitle("sample.apk.package.name");
        projectFileListText.setText("app\n.gitignore\nbuild.gradle\nREADME.md");
        addApkCheckbox.setChecked(true);
        addApkCheckbox.setText("Add Apk To Project");
        continueInstallBtn.setText("Continue Install Apk");
        setContentView(rootLayout);
        
        onReceiveApk(continueInstallBtn);
    }
    
    void onReceiveApk(Button continueInstallBtn){
        boolean isRecieveApk = Intent.ACTION_VIEW.equals(getIntent().getAction());
        if(!isRecieveApk){
            Toast.makeText(this, "You opened "+getClass()+" in the wrong way", Toast.LENGTH_LONG).show();
            finish();
        }
        
        final Uri apkUri = getIntent().getData();
        
        setTitle(getApkPackageName(this, apkUri));

        continueInstallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                            return;
                        }
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Recommended for starting a new task
                    startActivity(intent);
                }
            });
    }
    
    public String getApkPackageName(Context context, Uri apkUri) {
        PackageManager pm = context.getPackageManager();
        File tempFile = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(apkUri);
            if (inputStream == null) return null;
            
            tempFile = new File(context.getCacheDir(), "temp_apk.apk");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) 
                    outputStream.write(buffer, 0, length);
            }
            inputStream.close();

            PackageInfo packageInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), 0);
            if (packageInfo != null) return packageInfo.packageName;
            
        } catch (Exception e) {} 
        finally {
            if (tempFile != null && tempFile.exists()) 
                tempFile.delete();
        }
        return null;
    }
}
