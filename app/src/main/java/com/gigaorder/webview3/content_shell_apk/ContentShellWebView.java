package com.gigaorder.webview3.content_shell_apk;

import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.gigaorder.webview3.R;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.PathUtils;
import org.chromium.base.library_loader.LibraryLoader;
import org.chromium.base.library_loader.LibraryProcessType;
import org.chromium.base.library_loader.ProcessInitException;
import org.chromium.content_public.browser.BrowserStartupController;
import org.chromium.content_public.browser.JavascriptInjector;
import org.chromium.content_public.browser.WebContents;
import org.chromium.content_public.browser.WebContentsObserver;
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
    private List<Action> actionQueueBeforeReady;
    private WebViewClient webViewClient;
    WebContentsObserver webContentsObserver;

    public ContentShellWebView(Context context) {
        super(context);
        initShellManager(context);
        initShellManagerView(context);
        startBrowserProcess();
    }

    public ContentShellWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initShellManager(context);
        initShellManagerView(context);
        startBrowserProcess();
    }

    private void initShellManager(Context context) {
        ContextUtils.initApplicationContext(context.getApplicationContext());
        PathUtils.setPrivateDataDirectorySuffix("content_shell");
        ApplicationStatus.initialize((Application) context.getApplicationContext());
        actionQueueBeforeReady = new ArrayList<>();

        try {
            LibraryLoader.getInstance().ensureInitialized(LibraryProcessType.PROCESS_BROWSER);
        } catch (ProcessInitException e) {
            Log.e(TAG, "ContentView initialization failed.", e);
            System.exit(-1);
        }
    }

    private void initShellManagerView(Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View rootView = layoutInflater.inflate(R.layout.content_shell_web_view, this);

        shellManager = rootView.findViewById(R.id.shell_manager);
        shellManager.setWindow(new ActivityWindowAndroid(context, false));
    }

    public void handleAction(Action action) {
        Map<String, Object> parameterMap = action.getParameterMap();

        switch (action.getActionCode()) {
            case Action.LOAD_URL:
                shellManager.getActiveShell().loadUrl((String) parameterMap.get("url"));
                break;
            case Action.EVAL_JS:
                String script = (String) parameterMap.get("script");
                ValueCallback<String> callback = (ValueCallback<String>) parameterMap.get("callback");

                shellManager.getActiveShell().getWebContents().evaluateJavaScript(script, callback::onReceiveValue);
                break;
            case Action.ADD_JS_INTERFACE:
                Object interfaceObj = parameterMap.get("interfaceObj");
                String interfaceName = (String) parameterMap.get("interfaceName");

                WebContents webContents = shellManager.getActiveShell().getWebContents();
                JavascriptInjector javascriptInjector = JavascriptInjector.fromWebContents(webContents);

                javascriptInjector.addPossiblyUnsafeInterface(interfaceObj, interfaceName, JavascriptInterface.class);
                break;
            case Action.REMOVE_JS_INTERFACE:
                interfaceName = (String) parameterMap.get("interfaceName");

                webContents = shellManager.getActiveShell().getWebContents();
                javascriptInjector = JavascriptInjector.fromWebContents(webContents);
                javascriptInjector.removeInterface(interfaceName);
                break;
            case Action.SET_WEBVIEW_CLIENT:
                this.webViewClient = (WebViewClient) parameterMap.get("webViewClient");
                mapWebViewClientListeners(this.webViewClient);
                break;
        }
    }

    public void loadUrl(String url) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);

        createAction(Action.LOAD_URL, params);
    }

    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("script", script);
        params.put("callback", callback);

        createAction(Action.EVAL_JS, params);
    }

    public void addJavascriptInterface(Object interfaceObj, String interfaceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("interfaceObj", interfaceObj);
        params.put("interfaceName", interfaceName);

        createAction(Action.ADD_JS_INTERFACE, params);
    }

    public void removeJavascriptInterface(String interfaceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("interfaceName", interfaceName);

        createAction(Action.REMOVE_JS_INTERFACE, params);
    }

    private void createAction(int actionCode, Map<String, Object> parameterMap) {
        Action action = new Action(actionCode, parameterMap);

        if (!browserProcessStarted) {
            actionQueueBeforeReady.add(action);
        } else {
            handleAction(action);
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
                                    browserProcessStarted = true;

                                    // After the view is initialized successfully, execute the remaining
                                    // messages, then disable the action message list.
                                    // Subsequent messages are sent to actionHandler directly
                                    for (Action action : actionQueueBeforeReady) {
                                        handleAction(action);
                                    }
                                    actionQueueBeforeReady = null;
                                }

                                @Override
                                public void onFailure() {
                                }
                            });
        } catch (ProcessInitException e) {
            Log.e(TAG, "Unable to load native library.", e);
        }
    }

    public WebViewClient getWebViewClient() {
        return webViewClient;
    }

    public void setWebViewClient(WebViewClient webViewClient) {
        Map<String, Object> params = new HashMap<>();
        params.put("webViewClient", webViewClient);

        createAction(Action.SET_WEBVIEW_CLIENT, params);
    }

    private void mapWebViewClientListeners(WebViewClient webViewClient) {
        WebContents webContents = shellManager.getActiveShell().getWebContents();

        if (webContentsObserver != null) {
            webContents.removeObserver(webContentsObserver);
        }

        webContentsObserver = new WebContentsObserver() {
            @Override
            public void didStartLoading(String url) {
                super.didStartLoading(url);
                webViewClient.onPageStarted(null, url, null);
            }

            @Override
            public void didFinishLoad(long frameId, String validatedUrl, boolean isMainFrame) {
                super.didFinishLoad(frameId, validatedUrl, isMainFrame);
                webViewClient.onPageFinished(null, validatedUrl);
            }

            @Override
            public void didFailLoad(boolean isMainFrame, int errorCode, String description, String failingUrl) {
                super.didFailLoad(isMainFrame, errorCode, description, failingUrl);
                webViewClient.onReceivedError(null, errorCode, description, failingUrl);
            }
        };

        webContents.addObserver(webContentsObserver);
    }

    private static class Action {
        private static final int LOAD_URL = 1;
        private static final int EVAL_JS = 2;
        private static final int ADD_JS_INTERFACE = 3;
        private static final int REMOVE_JS_INTERFACE = 4;
        private static final int SET_WEBVIEW_CLIENT = 5;

        private int actionCode;
        private Map<String, Object> parameterMap;

        public Action(int actionCode, Map<String, Object> parameterMap) {
            this.actionCode = actionCode;
            this.parameterMap = parameterMap;
        }

        public int getActionCode() {
            return actionCode;
        }

        public void setActionCode(int actionCode) {
            this.actionCode = actionCode;
        }

        public Map<String, Object> getParameterMap() {
            return parameterMap;
        }

        public void setParameterMap(Map<String, Object> parameterMap) {
            this.parameterMap = parameterMap;
        }
    }
}
