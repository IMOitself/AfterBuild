package imo.after_build;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        Button continueInstallBtn = new Button(this);
        CheckBox addApkCheckbox = new CheckBox(this);
        rootLayout.addView(projectFileListText);
        rootLayout.addView(continueInstallBtn);
        rootLayout.addView(addApkCheckbox);

        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(rootLayout);
    }
}
