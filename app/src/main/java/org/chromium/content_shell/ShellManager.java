package org.chromium.content_shell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.gigaorder.webview3.R;

import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.components.embedder_support.view.ContentViewRenderView;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.WindowAndroid;

@JNINamespace("content")
public class ShellManager extends FrameLayout {
    public static final String DEFAULT_SHELL_URL = "https://www.google.com/";
    private WindowAndroid mWindow;
    private Shell mActiveShell;
    private ContentViewRenderView mContentViewRenderView;

    public ShellManager(Context context) {
        super(context);
        nativeInit(this);
    }

    public ShellManager(Context context, AttributeSet attrs) {
        super(context, attrs);
        nativeInit(this);
    }

    public void setWindow(WindowAndroid window) {
        if (window != null) {
            this.mWindow = window;
            this.mContentViewRenderView = new ContentViewRenderView(this.getContext());
            this.mContentViewRenderView.onNativeLibraryLoaded(window);
        }
    }

    public void launchShell(String url) {
        ThreadUtils.assertOnUiThread();
        Shell previousShell = this.mActiveShell;
        nativeLaunchShell(url);
        if (previousShell != null) {
            previousShell.close();
        }
    }

    public Shell getActiveShell() {
        return mActiveShell;
    }

    @CalledByNative
    private Object createShell(long nativeShellPtr) {
        if (this.mContentViewRenderView == null) {
            this.mContentViewRenderView = new ContentViewRenderView(this.getContext());
            this.mContentViewRenderView.onNativeLibraryLoaded(this.mWindow);
        }

        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Shell shellView = (Shell) inflater.inflate(R.layout.shell_view, null);
        shellView.initialize(nativeShellPtr, this.mWindow);
        if (this.mActiveShell != null) {
            this.removeShell(this.mActiveShell);
        }

        this.showShell(shellView);
        return shellView;
    }

    private void showShell(Shell shellView) {
        shellView.setContentViewRenderView(this.mContentViewRenderView);
        this.addView(shellView, new LayoutParams(-1, -1));
        this.mActiveShell = shellView;
        WebContents webContents = this.mActiveShell.getWebContents();
        if (webContents != null) {
            this.mContentViewRenderView.setCurrentWebContents(webContents);
            webContents.onShow();
        }

    }

    @CalledByNative
    private void removeShell(Shell shellView) {
        if (shellView == this.mActiveShell) {
            this.mActiveShell = null;
        }

        if (shellView.getParent() != null) {
            shellView.setContentViewRenderView(null);
            this.removeView(shellView);
        }
    }

    @CalledByNative
    public void destroy() {
        if (this.mActiveShell != null) {
            this.removeShell(this.mActiveShell);
        }

        if (this.mContentViewRenderView != null) {
            this.mContentViewRenderView.destroy();
            this.mContentViewRenderView = null;
        }

    }

    private static native void nativeInit(Object var0);

    private static native void nativeLaunchShell(String var0);
}
