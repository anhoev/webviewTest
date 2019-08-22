// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.gigaorder.webview3.content_shell_apk;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Button;

import com.gigaorder.webview3.R;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.PathUtils;
import org.chromium.base.library_loader.LibraryLoader;
import org.chromium.base.library_loader.LibraryProcessType;
import org.chromium.base.library_loader.ProcessInitException;

import java.lang.reflect.Method;

/**
 * Activity for managing the Content Shell.
 */
public class ContentShellActivity extends Activity {

    private static final String TAG = "ContentShellActivity";

    private ContentShellWebView csWebView;

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
        csWebView = findViewById(R.id.web_view_container);
        csWebView.loadUrl("https://www.youtube.com/");

//        Button button = findViewById(R.id.test);
//        button.setOnClickListener((v) -> {
//            try {
//                csWebView.evaluateJavaScript("(function() {return document.getElementsByTagName('body')[0].innerHTML;})();"
//                        , s -> {
//                            Log.d("abc", s);
//                        });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }


    @Override
    protected void onDestroy() {
//        if (csWebView != null) csWebView.destroy();
        super.onDestroy();
    }
}
