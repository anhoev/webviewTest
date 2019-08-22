package org.chromium.content_shell;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.gigaorder.webview3.R;

import org.chromium.base.Callback;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.components.embedder_support.view.ContentView;
import org.chromium.components.embedder_support.view.ContentViewRenderView;
import org.chromium.content_public.browser.ActionModeCallbackHelper;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.content_public.browser.NavigationController;
import org.chromium.content_public.browser.SelectionPopupController;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.ViewAndroidDelegate;
import org.chromium.ui.base.WindowAndroid;

@JNINamespace("content")
public class Shell extends LinearLayout {
    private WebContents mWebContents;
    private NavigationController mNavigationController;
    private long mNativeShell;
    private ContentViewRenderView mContentViewRenderView;
    private WindowAndroid mWindow;
    private ShellViewAndroidDelegate mViewAndroidDelegate;
    private boolean mIsFullscreen;
    private boolean mLoading;
    private Callback<Boolean> mOverlayModeChangedCallbackForTesting;

    public Shell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContentViewRenderView(ContentViewRenderView contentViewRenderView) {
        FrameLayout contentViewHolder = this.findViewById(R.id.contentview_holder);
        if (contentViewRenderView == null) {
            if (this.mContentViewRenderView != null) {
                contentViewHolder.removeView(this.mContentViewRenderView);
            }
        } else {
            contentViewHolder.addView(contentViewRenderView, new LayoutParams(-1, -1));
        }

        this.mContentViewRenderView = contentViewRenderView;
    }

    public void initialize(long nativeShell, WindowAndroid window) {
        this.mNativeShell = nativeShell;
        this.mWindow = window;
    }

    public void close() {
        if (this.mNativeShell != 0L) {
            nativeCloseShell(this.mNativeShell);
        }
    }

    @CalledByNative
    private void onNativeDestroyed() {
        this.mWindow = null;
        this.mNativeShell = 0L;
        this.mWebContents = null;
    }

    public boolean isDestroyed() {
        return this.mNativeShell == 0L;
    }

    public boolean isLoading() {
        return this.mLoading;
    }

    public void loadUrl(String url) {
        if (url != null) {
            if (TextUtils.equals(url, this.mWebContents.getLastCommittedUrl())) {
                this.mNavigationController.reload(true);
            } else {
                this.mNavigationController.loadUrl(new LoadUrlParams(sanitizeUrl(url)));
            }

            this.getContentView().clearFocus();
            this.getContentView().requestFocus();
        }
    }

    public static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        } else {
            if (url.startsWith("www.") || !url.contains(":")) {
                url = "http://" + url;
            }

            return url;
        }
    }

    @CalledByNative
    private void onUpdateUrl(String url) {
    }

    @CalledByNative
    private void onLoadProgressChanged(double progress) {
    }

    @CalledByNative
    private void toggleFullscreenModeForTab(boolean enterFullscreen) {
        this.mIsFullscreen = enterFullscreen;
    }

    @CalledByNative
    private boolean isFullscreenForTabOrPending() {
        return this.mIsFullscreen;
    }

    @CalledByNative
    private void setIsLoading(boolean loading) {
        this.mLoading = loading;
    }

    public ShellViewAndroidDelegate getViewAndroidDelegate() {
        return this.mViewAndroidDelegate;
    }

    @CalledByNative
    private void initFromNativeTabContents(WebContents webContents) {
        Context context = this.getContext();
        ContentView cv = ContentView.createContentView(context, webContents);
        this.mViewAndroidDelegate = new ShellViewAndroidDelegate(cv);
        webContents.initialize("", this.mViewAndroidDelegate, cv, this.mWindow, WebContents.createDefaultInternalsHolder());
        this.mWebContents = webContents;
        SelectionPopupController.fromWebContents(webContents).setActionModeCallback(this.defaultActionCallback());
        this.mNavigationController = this.mWebContents.getNavigationController();
        if (this.getParent() != null) {
            this.mWebContents.onShow();
        }

        if (this.mWebContents.getVisibleUrl() != null) {
        }

        ((FrameLayout) this.findViewById(R.id.contentview_holder)).addView(cv, new LayoutParams(-1, -1));
        cv.requestFocus();
        this.mContentViewRenderView.setCurrentWebContents(this.mWebContents);
    }

    private ActionMode.Callback defaultActionCallback() {
        final ActionModeCallbackHelper helper = SelectionPopupController.fromWebContents(this.mWebContents).getActionModeCallbackHelper();
        return new ActionMode.Callback() {
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                helper.onCreateActionMode(mode, menu);
                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return helper.onPrepareActionMode(mode, menu);
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return helper.onActionItemClicked(mode, item);
            }

            public void onDestroyActionMode(ActionMode mode) {
                helper.onDestroyActionMode();
            }
        };
    }

    @CalledByNative
    public void setOverlayMode(boolean useOverlayMode) {
        this.mContentViewRenderView.setOverlayVideoMode(useOverlayMode);
        if (this.mOverlayModeChangedCallbackForTesting != null) {
            this.mOverlayModeChangedCallbackForTesting.onResult(useOverlayMode);
        }

    }

    @CalledByNative
    public void sizeTo(int width, int height) {
        this.mWebContents.setSize(width, height);
    }

    public void setOverayModeChangedCallbackForTesting(Callback<Boolean> callback) {
        this.mOverlayModeChangedCallbackForTesting = callback;
    }

    @CalledByNative
    private void enableUiControl(int controlId, boolean enabled) {
    }

    public ViewGroup getContentView() {
        ViewAndroidDelegate viewDelegate = this.mWebContents.getViewAndroidDelegate();
        return viewDelegate != null ? viewDelegate.getContainerView() : null;
    }

    public WebContents getWebContents() {
        return this.mWebContents;
    }

    private static native void nativeCloseShell(long var0);
}
