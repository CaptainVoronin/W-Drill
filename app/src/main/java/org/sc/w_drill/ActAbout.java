package org.sc.w_drill;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;


public class ActAbout extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try
        {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int number = pInfo.versionCode;

            TextView tv = (TextView) findViewById(R.id.version_name);
            tv.setText(version);

            tv = (TextView) findViewById(R.id.version_number);
            tv.setText(Integer.valueOf(number).toString());
        }
        catch (Exception ex)
        {

        }
    }
}
