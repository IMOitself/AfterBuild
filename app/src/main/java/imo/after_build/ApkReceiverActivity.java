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
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        onReceiveApk(continueInstallBtn, projectFileListText, addApkCheckbox);
    }
    
    void onReceiveApk(Button continueInstallBtn, TextView projectFileListText, final CheckBox addApkCheckBox){
        boolean isRecieveApk = Intent.ACTION_VIEW.equals(getIntent().getAction());
        if(!isRecieveApk){
            Toast.makeText(this, "You opened "+getClass()+" in the wrong way", Toast.LENGTH_LONG).show();
            finish();
        }
        
        final Uri apkUri = getIntent().getData();

        continueInstallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    installApk(apkUri);
                }
            });
            
        final String packageName = getApkPackageName(this, apkUri);
        setTitle(packageName);
        
        String projectPath = getProjectPathByPackageName(packageName);
        File projectFile = new File(projectPath);
        String projectFileList = "";
        
        if(projectFile.exists())
        for(File File : projectFile.listFiles())
            projectFileList += "<br>"+File.getName();
        
        projectFileListText.setText(Html.fromHtml("<b>"+projectPath+"</b><br>"+projectFileList));
    }
    
    
    
    public void installApk(Uri apkUri){
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
    
    String getProjectPathByPackageName(String packageName){
        final File AppProjectsFolder = new File("/storage/emulated/0/AppProjects");
        if(! AppProjectsFolder.exists()) return AppProjectsFolder+" does not exist";
        
        for(File Folder : AppProjectsFolder.listFiles()){
            if(! Folder.isDirectory()) continue;
            
            File manifestFile = new File(Folder, "app/src/main/AndroidManifest.xml");
            boolean isAIDEProject = manifestFile.exists();
            
            if(! isAIDEProject) continue;
            
            Pattern pattern = Pattern.compile("package=\"([a-zA-Z0-9_.]+)\"");

            try {
                BufferedReader reader = new BufferedReader(new FileReader(manifestFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String foundPackageName = matcher.group(1);
                        if(packageName.equals(foundPackageName)){
                            return Folder.getAbsolutePath();
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                return "Error reading the manifest file: " + e.getMessage();
            }
        }
        return packageName+"'s related project folder not found";
    }
}
