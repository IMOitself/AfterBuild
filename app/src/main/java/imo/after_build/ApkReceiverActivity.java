package imo.after_build;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        addApkCheckbox.setText("Add Apk To Project");
        continueInstallBtn.setText("Continue Install Apk");
        setContentView(rootLayout);
        
        
        boolean isRecieveApk = Intent.ACTION_VIEW.equals(getIntent().getAction());
        if(!isRecieveApk){
            Toast.makeText(this, "You opened "+getClass()+" in the wrong way", Toast.LENGTH_LONG).show();
            finish();
        }
        Uri apkUri = getIntent().getData();
    }
}
