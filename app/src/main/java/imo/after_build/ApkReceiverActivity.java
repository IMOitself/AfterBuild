package imo.after_build;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
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
import java.util.List;
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
        rootLayout.addView(apkActionLayout);
        rootLayout.addView(continueInstallBtn);

        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setTitle("Not An AIDE Project");
        projectFileListText.setText("");
        projectFileListText.setTypeface(Typeface.MONOSPACE);
        projectFileListText.setMovementMethod(new ScrollingMovementMethod());
		projectFileListText.setMaxLines(25);
        addApkCheckbox.setChecked(true);
        addApkCheckbox.setEnabled(false); // initial
        addApkCheckbox.setText("Add Apk To Project");
        continueInstallBtn.setText("Continue Install Apk");
        
        ((LinearLayout.LayoutParams) 
            projectFileListText.getLayoutParams()).weight = 2.0f;
        
        ((LinearLayout.LayoutParams) 
            continueInstallBtn.getLayoutParams()).width = LinearLayout.LayoutParams.MATCH_PARENT;
			
		((LinearLayout.LayoutParams) 
            continueInstallBtn.getLayoutParams()).weight = 1.0f;
		
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

        final String packageName = getApkPackageName(this, apkUri);
        
        String projectPath = getProjectPathByPackageName(packageName);
        final File projectFile = new File(projectPath);

        StringBuilder displayText = new StringBuilder(projectPath);

        if (projectFile.exists() && projectFile.listFiles() != null) {
            for (File file : projectFile.listFiles()) {
                displayText.append("\n").append(file.getName());
            }
        }

        projectFileListText.setText(displayText.toString());

        if (projectFile.exists()) {
            setTitle(packageName);
            addApkCheckBox.setEnabled(true);
        }
        
        continueInstallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    
                    installApk(apkUri);

                    if(addApkCheckBox.isChecked()) {
                        addApkToProject(apkUri, projectFile);
                        Toast.makeText(v.getContext(), "apk added to project folder", Toast.LENGTH_LONG).show();
                    }
                    
                    onAfterInstall(packageName, apkUri);
                }
            });
    }
    
    void onAfterInstall(final String packageName, final Uri apkUri){
        LinearLayout rootLayout = new LinearLayout(this);
        TextView textview = new TextView(this);
        Button openButton = new Button(this);
        Button exitButton = new Button(this);
        //TODO: option to go to AfterGit
        rootLayout.addView(textview);
        rootLayout.addView(openButton);
        rootLayout.addView(exitButton);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(rootLayout);
        
        Button[] buttons = {openButton, exitButton};
        for(Button button : buttons){
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
            params.weight = 1.0f;
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            button.setLayoutParams(params);
        }
        
        ((LinearLayout.LayoutParams) 
        textview.getLayoutParams()).weight = 2.0f;
        ((LinearLayout.LayoutParams) 
        textview.getLayoutParams()).width = LinearLayout.LayoutParams.MATCH_PARENT;
        textview.setGravity(Gravity.CENTER);
        textview.setText("Continue installing the apk in \nPackage Installer");
		
		textview.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    installApk(apkUri);
                }
            });
        openButton.setText("Open");
        openButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    Intent intent = ApkReceiverActivity.this.getPackageManager().getLaunchIntentForPackage(packageName);
                    ApkReceiverActivity.this.startActivity(intent);
                    finish();
                }
            });
        exitButton.setText("Exit");
        exitButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    finish();
                }
            });
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		String defaultInstallerPackageName = null;
		PackageManager pm = getPackageManager();
		List<ResolveInfo> installers = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		String googleInstaller = "com.google.android.packageinstaller";
		String legacyInstaller = "com.android.packageinstaller";// OEM devices (Samsung, etc.)

		for (ResolveInfo resolveInfo : installers) {
			String packageName = resolveInfo.activityInfo.packageName;
			if (googleInstaller.equals(packageName) || 
				legacyInstaller.equals(packageName)) 
				defaultInstallerPackageName = packageName;
		}
		if (defaultInstallerPackageName != null) 
			intent.setPackage(defaultInstallerPackageName);
		
        startActivity(intent);
    }
    
    public void addApkToProject(Uri apkUri, File projectFile){
        if(! projectFile.exists()) return;

        String fileName = "compiled.apk";
        File destinationFile = new File(projectFile, fileName);

        try {
            InputStream inputStream = getContentResolver().openInputStream(apkUri);
            OutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {}
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
        final File[] FoldersToLook = {
            new File("/storage/emulated/0/AppProjects"), 
            new File("/storage/emulated/0/AppProjects2")
        };
        boolean isFolderExist = false;
        String log = "";
        
        for(File AppProjectsFolder : FoldersToLook){
            if(! AppProjectsFolder.exists()) continue;
            isFolderExist = true;

            for(File Folder : AppProjectsFolder.listFiles()){
                if(! Folder.isDirectory()) continue;
                log += "\nsearched inside "+Folder.getPath();

                File manifestFile = new File(Folder, "app/src/main/AndroidManifest.xml");
                boolean isAIDEProject = manifestFile.exists();

                if(! isAIDEProject) continue;
                log += "\nread manifestFile "+manifestFile;

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
        }
        
        if(! isFolderExist) return "AppProjects or AppProjects2 does not exist";
        
        return packageName+"'s related project folder not found\n"+log;
    }

    @Override
    protected void onStop() {
        super.onStop();
        finishAffinity();
    }
}
