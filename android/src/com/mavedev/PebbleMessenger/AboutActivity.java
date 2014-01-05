package com.mavedev.PebbleMessenger;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

import com.mavedev.PebbleMessenger.R;

public class AboutActivity extends Activity
{
	public static final String CREATOR = "Jannis Pohl";

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		try
		{
			Method m = Activity.class.getMethod("getActionBar", new Class[] {});
			Object o = m.invoke(this);

			Class<?> actionBarClass = Class.forName("android.app.ActionBar");
			Method m2 = actionBarClass.getMethod("setDisplayHomeAsUpEnabled",
					new Class[] { boolean.class });
			m2.invoke(o, true);
		}
		catch (Exception e)
		{
		}

		TextView aboutText = (TextView) findViewById(R.id.aboutText);

		try
		{
			PackageManager pm = getPackageManager();
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);

			String appLabel = info.applicationInfo.loadLabel(pm).toString();
			String version = info.versionName;

			CharSequence seq_description = info.applicationInfo.loadDescription(pm);
			String description = seq_description != null ? seq_description.toString() : "";

			aboutText.setText(appLabel + "\n" + "Version " + version + "\n\nby " + CREATOR + "\n\n"
					+ description);
		}
		catch (NameNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
