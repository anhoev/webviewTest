// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.gigaorder.webview3.content_shell_apk;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

import com.gigaorder.webview3.R;

import org.chromium.base.ApplicationStatus;
import org.chromium.content.browser.JavascriptInterface;

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

        // onStateChange requires "this" - the activity - so it must be used inside the activity,
        // passing the activity to the ContentShellWebView is possible but it will make the component
        // less usable in different scenarios (used in other components which are not Activity)
        try {
            Method method = ApplicationStatus.class.getDeclaredMethod("onStateChange", Activity.class, int.class);
            method.setAccessible(true);
            method.invoke(null, this, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.content_shell_activity);
        csWebView = findViewById(R.id.web_view_container);
        csWebView.loadUrl("about:blank");

        // Example for csWebView methods
        Button button = findViewById(R.id.test);
        button.setOnClickListener(v -> {
            Object interfaceObj = new Object() {
                @JavascriptInterface
                public int test() {
                    return Integer.MAX_VALUE;
                }
            };
            String interfaceName = "testJsInterface";

            csWebView.addJavascriptInterface(interfaceObj, interfaceName);
            csWebView.loadUrl("javascript:testJsInterface.test()");
        });
    }
}