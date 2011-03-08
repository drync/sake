package com.drync.android;

import java.util.HashMap;

import com.flurry.android.FlurryAgent;

public class DryncThread extends Thread {

	public DryncThread() {
		// TODO Auto-generated constructor stub
	}

	public DryncThread(Runnable runnable) {
		super(runnable);
		// TODO Auto-generated constructor stub
	}

	public DryncThread(String threadName) {
		super(threadName);
		// TODO Auto-generated constructor stub
	}

	public DryncThread(Runnable runnable, String threadName) {
		super(runnable, threadName);
		Thread.setDefaultUncaughtExceptionHandler(new DryncThread.DryncUncaughtExceptionHandler());
	}

	public DryncThread(ThreadGroup group, Runnable runnable) {
		super(group, runnable);
		Thread.setDefaultUncaughtExceptionHandler(new DryncThread.DryncUncaughtExceptionHandler());
	}

	public DryncThread(ThreadGroup group, String threadName) {
		super(group, threadName);
		Thread.setDefaultUncaughtExceptionHandler(new DryncThread.DryncUncaughtExceptionHandler());
	}

	public DryncThread(ThreadGroup group, Runnable runnable, String threadName) {
		super(group, runnable, threadName);

		Thread.setDefaultUncaughtExceptionHandler(new DryncThread.DryncUncaughtExceptionHandler());
	}

	public DryncThread(ThreadGroup group, Runnable runnable, String threadName,
			long stackSize) {
		super(group, runnable, threadName, stackSize);
		Thread.setDefaultUncaughtExceptionHandler(new DryncThread.DryncUncaughtExceptionHandler());
	}
	
	public static class DryncUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
	{
		Thread.UncaughtExceptionHandler coreHandler;
		
		public DryncUncaughtExceptionHandler() {
			super();
			
			coreHandler = Thread.getDefaultUncaughtExceptionHandler();
		}

		public void uncaughtException(Thread thread, Throwable ex) 
		{
			HashMap<String, String> parameters = new HashMap<String, String>();
			parameters.put("ExType", ex.getClass().getName());
			parameters.put("ExMsg", ex.getMessage());
			
			StringBuilder stackbldr = new StringBuilder();
			StackTraceElement stack[] = ex.getStackTrace();
			int depth = 0;
			for (StackTraceElement elem : stack)
			{
				if (depth > 4)
					break;
				
				stackbldr.append(elem.toString());
			
			}
			
			parameters.put("stack", stackbldr.toString());
			
			FlurryAgent.onEvent("UncaughtException Report", parameters);
			
			
			// finally, do the original handler
			coreHandler.uncaughtException(thread, ex);
		}
		
	}
	

}
