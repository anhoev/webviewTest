package com.gigaorder.webview3.content_shell_apk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.gigaorder.webview3.R;

import org.chromium.base.library_loader.LibraryProcessType;
import org.chromium.base.library_loader.ProcessInitException;
import org.chromium.content_public.browser.BrowserStartupController;
import org.chromium.content_public.browser.JavaScriptCallback;
import org.chromium.content_shell.ShellManager;
import org.chromium.ui.base.ActivityWindowAndroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ContentShellWebView extends FrameLayout {
    private ShellManager shellManager;
    private static boolean browserProcessStarted;
    private static final String TAG = "ContentShellWebView";
    private Handler actionHandler;
    private List<Message> actionQueueBeforeReady;

    public ContentShellWebView(Context context) {
        super(context);
        initShellManager(context);
        initActionHandler();
    }

    public ContentShellWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initShellManager(context);
        initActionHandler();
    }


    private void initShellManager(Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View rootView = layoutInflater.inflate(R.layout.content_shell_web_view, this);

        shellManager = rootView.findViewById(R.id.shell_manager);
        shellManager.setWindow(new ActivityWindowAndroid(context, false));
        startBrowserProcess();
    }

    @SuppressLint("HandlerLeak")
    private void initActionHandler() {
        actionQueueBeforeReady = new ArrayList<>();

        actionHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Action.LOAD_URL:
                        shellManager.getActiveShell().loadUrl((String) msg.obj);
                        break;
                    case Action.EVAL_JS:
                        Map params = (HashMap) msg.obj;
                        String script = (String) params.get("script");
                        JavaScriptCallback callback = (JavaScriptCallback) params.get("callback");
                        shellManager.getActiveShell().getWebContents().evaluateJavaScript(script, callback);
                        break;
                }
            }
        };
    }

    public void loadUrl(String url) {
        sendMessage(Action.LOAD_URL, url);
    }

    public void evaluateJavaScript(String script, JavaScriptCallback callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("script", script);
        params.put("callback", callback);
        sendMessage(Action.EVAL_JS, params);
    }

    private void sendMessage(int what, Object obj) {
        Message message = Message.obtain(null, what, obj);

        if (!browserProcessStarted) {
            actionQueueBeforeReady.add(message);
        } else {
            actionHandler.sendMessage(message);
        }
    }

    private void startBrowserProcess() {
        if (browserProcessStarted) {
            return;
        }

        try {
            BrowserStartupController.get(LibraryProcessType.PROCESS_BROWSER)
                    .startBrowserProcessesAsync(
                            true, false, new BrowserStartupController.StartupCallback() {
                                @Override
                                public void onSuccess() {
                                    shellManager.launchShell(ShellManager.DEFAULT_SHELL_URL);
                                    browserProcessStarted = true;
                                    // After the view is initialized successfully, execute the remaining
                                    // messages, then disable the action message list.
                                    // Subsequent messages are sent to actionHandler directly
                                    for (Message message : actionQueueBeforeReady) {
                                        actionHandler.sendMessage(message);
                                    }
                                    actionQueueBeforeReady = null;
                                }

                                @Override
                                public void onFailure() {
                                    // TODO: handle error
                                }
                            });
        } catch (ProcessInitException e) {
            Log.e(TAG, "Unable to load native library.", e);
        }
    }

    private static class Action {
        private static final int LOAD_URL = 1;
        private static final int EVAL_JS = 2;
    }
}
