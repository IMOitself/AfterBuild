package imo.after_build;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity 
{
    //TODO: MAJOR REFACTORING BECAUSE IT IS GETTING LESS UNDERSTANDABLE
    String projectPackageName = "nope";
    Button projectPathBtn;
    Button apkContinueInstallBtn;
    Button addApkToProjectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if(! hasStoragePermission()) {
            requestStoragePermission();
            finishAffinity();
            return;
        }

        final EditText projectPathEdit = findViewById(R.id.project_path_edit);
        projectPathBtn = findViewById(R.id.project_path_btn);
        final TextView outputText = findViewById(R.id.output_txt);
        apkContinueInstallBtn = findViewById(R.id.apk_install_btn);
        addApkToProjectBtn = findViewById(R.id.add_apk_btn);

        boolean isRecieveApk = receiveApk(this);

        projectPathBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    File detectedProjectFolder = findProjectByPackageName(projectPackageName);

                    String projectPathText = projectPathEdit.getText().toString();

                    File projectFolder = null;
                    if(projectPathText.isEmpty() && detectedProjectFolder != null) {
                        projectFolder = detectedProjectFolder;

                        projectPathEdit.setText(projectFolder.getAbsolutePath());
                        projectPathBtn.setText("LOAD");
                    } else {
                        projectFolder = new File(projectPathText);
                        if(! projectFolder.exists()) {
                            //TODO: also detect if folder is an aide project
                            projectPathEdit.setText("");
                            projectPathBtn.setText("DETECT");
                        }
                    }

                    String output = "";
                    if(projectFolder != null)
                        for(File file : projectFolder.listFiles()) {
                            output += "\n" + file.getName();
                        }
                    outputText.setText(output);
                    MainActivity.this.setTitle(projectPackageName);
                }
            });

        if(isRecieveApk) projectPathBtn.performClick();
    }

    public boolean receiveApk(final Context mContext) {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if(! Intent.ACTION_VIEW.equals(action)) return false;
        if(! "application/vnd.android.package-archive".equals(type)) return false;
        final Uri apkUri = intent.getData();
        if(apkUri == null) return false;
        projectPackageName = getApkPackageName(mContext, apkUri);

        apkContinueInstallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    installApk(mContext, apkUri);
                }
            });

        addApkToProjectBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    File detectedProjectFolder = findProjectByPackageName(projectPackageName);
                    if(copyApkToFolder(apkUri, detectedProjectFolder, projectPackageName.replace(".", "-") + ".apk")) {
                        //TODO: refresh project files list
                        addApkToProjectBtn.setText("Successfully Added Apk.");
                        addApkToProjectBtn.setEnabled(false);
                    }
                }
            });
        return true;
    }


    public static String getApkPackageName(Context context, Uri apkUri) {
        File tempFile = null;
        try {
            tempFile = copyToTempCacheFile(context, apkUri);
            if(tempFile == null) return null;

            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), 0);

            if(packageInfo == null) return null;

            return packageInfo.packageName;

        } catch(Exception e) {}

        if(tempFile != null && tempFile.exists()) 
            tempFile.delete();
        return null;
    }

    private static File copyToTempCacheFile(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if(inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), "temp_apk.apk");

            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            inputStream.close();
            return tempFile;

        } catch(Exception e) {
            return null;
        }
    }

    private static File findProjectByPackageName(String projectPackageName) {
        String javaPath = projectPackageName.replace(".", "/");

        // used by AIDE to store projects
        final File AppProjectsFolder = new File("/storage/emulated/0/", "AppProjects");
        if(! AppProjectsFolder.exists()) return null;

        for(File folder : AppProjectsFolder.listFiles()) {
            if(! folder.isDirectory()) continue;
            File javafolder = new File(folder, "app/src/main/java/");

            if(! javafolder.exists()) continue;
            File subfolder = new File(javafolder, javaPath);

            if(subfolder.exists()) return folder;
        }
        return null;
    }

    static void installApk(Context mContext, Uri apkUri) {
        if(apkUri != null) {
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                mContext.startActivity(installIntent);
            } catch(Exception e) {
                Toast.makeText(mContext, "Failed to open package installer.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "APK file URI is not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean copyApkToFolder(Uri apkUri, File destinationFolder, String filename) {
        try {
            if(!destinationFolder.exists()) destinationFolder.mkdirs();
            File destinationFile = new File(destinationFolder, filename);

            InputStream in = getContentResolver().openInputStream(apkUri);
            OutputStream out = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

        } catch(Exception e) {
            Toast.makeText(this, "Failed to copy APK", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    boolean hasStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    void requestStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }
}
