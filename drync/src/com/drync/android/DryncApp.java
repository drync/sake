package com.drync.android;


import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dDlnRm5uTHNsVVJSbExzamtVWUg4Q1E6MQ")
public class DryncApp extends Application {

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
	}
	
	

}
