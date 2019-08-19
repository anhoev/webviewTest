// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.gigaorder.webview3.content_shell_apk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gigaorder.webview3.R;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.PathUtils;
import org.chromium.base.library_loader.LibraryLoader;
import org.chromium.base.library_loader.LibraryProcessType;
import org.chromium.base.library_loader.ProcessInitException;
import org.chromium.content_public.browser.BrowserStartupController;
import org.chromium.content_public.browser.WebContents;
import org.chromium.content_shell.Shell;
import org.chromium.content_shell.ShellManager;
import org.chromium.ui.base.ActivityWindowAndroid;
import org.chromium.ui.base.WindowAndroid;

import java.lang.reflect.Method;

//import org.chromium.base.CommandLine;

/**
 * Activity for managing the Content Shell.
 */
public class ContentShellActivity extends Activity {

    private static final String TAG = "ContentShellActivity";

    private ShellManager mShellManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContextUtils.initApplicationContext(getApplication());
        //test
        PathUtils.setPrivateDataDirectorySuffix("content_shell");
        ApplicationStatus.initialize(getApplication());

        try {
            Method method = ApplicationStatus.class.getDeclaredMethod("onStateChange", Activity.class, int.class);
            method.setAccessible(true);
            method.invoke(null, this, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            LibraryLoader.getInstance().ensureInitialized(LibraryProcessType.PROCESS_BROWSER);
        } catch (ProcessInitException e) {
            Log.e(TAG, "ContentView initialization failed.", e);
            System.exit(-1);
            return;
        }

        setContentView(R.layout.content_shell_activity);
        mShellManager = findViewById(R.id.shell_container);
        mShellManager.setWindow(new ActivityWindowAndroid(this, false));


        try {
            BrowserStartupController.get(LibraryProcessType.PROCESS_BROWSER)
                    .startBrowserProcessesAsync(
                            true, false, new BrowserStartupController.StartupCallback() {
                                @Override
                                public void onSuccess() {
                                    mShellManager.launchShell(ShellManager.DEFAULT_SHELL_URL);
                                }

                                @Override
                                public void onFailure() {
                                    finish();
                                }
                            });
        } catch (ProcessInitException e) {
            Log.e(TAG, "Unable to load native library.", e);
            System.exit(-1);
        }
    }


    @Override
    protected void onDestroy() {
        if (mShellManager != null) mShellManager.destroy();
        super.onDestroy();
    }
}
