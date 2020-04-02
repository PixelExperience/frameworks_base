package com.android.internal.custom.longshot;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ScrollView;
import com.android.internal.widget.RecyclerView;
import java.util.ArrayList;

public class LongshotUtil {
    private static final int MSG_SEARCH_COMPLETE = 1000;
    static String TAG = "LongshotUtil";
    private boolean DEBUG = false;
    private LongshotUtilCallback mCallback;
    private Context mContext;
    private ViewGroup mDecordView;
    private int mLastPosition;
    private int mLongshotScope;
    private int mLongshotScopeBottom;
    private int mLongshotScopeTop;
    private View mMainScrollView;
    private int mMainScrollViewlayer = 0;
    private int mScrollContainerType = 0;
    private int mWindowLayer = 0;

    public interface LongshotUtilCallback {
        int getLastWipe();

        void moveY(int i);
    }

    public LongshotUtil(Context context, ViewGroup decordView) {
        mDecordView = decordView;
        mContext = context;
        setLongshotScope(900, 720, 1660);
    }

    public void setLongshotScope(int top, int main, int bottom) {
        mLongshotScopeTop = top;
        mLongshotScopeBottom = bottom;
        mLongshotScope = main;
    }

    public void longshotStart() {
        if (mDecordView != null) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("canScroll:");
                sb.append(mMainScrollView != null);
                Log.d(TAG, sb.toString());
            }
            try {
                searchMainScrollView(mDecordView);
                searchScrollViewComplete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void longshotStop() {
        Log.d(TAG, "longshotStop mMainScrollView:" + mMainScrollView + " mDecordView:" + mDecordView);
        View view = mMainScrollView;
        if (view != null) {
            view.setLongshotUtil(null);
        }
        ViewGroup viewGroup = mDecordView;
        if (viewGroup != null) {
            viewGroup.setLongshotUtil(null);
        }
    }

    private boolean searchMainScrollView(ViewGroup containView) {
        int i;
        if (DEBUG) {
            Log.d(TAG, "start searchMainScrollView:");
        }
        int targetlevel = 0;
        ArrayList<ViewGroup> parentViewList = new ArrayList<>();
        ArrayList<ViewGroup> childViewList = new ArrayList<>();
        boolean findViewComplete = false;
        parentViewList.add(containView);
        while (!findViewComplete) {
            for (int i2 = 0; i2 < parentViewList.size(); i2++) {
                ViewGroup targetView = (ViewGroup) parentViewList.get(i2);
                int count = targetView.getChildCount();
                for (int j = 0; j < count; j++) {
                    if (targetView.getChildAt(j) != null && (targetView.getChildAt(j) instanceof ViewGroup)) {
                        ViewGroup childView = (ViewGroup) targetView.getChildAt(j);
                        if (childView.canScrollVertically(0) && childView.getVisibility() == 0 && (targetlevel >= (i = mMainScrollViewlayer) || i == 0)) {
                            if (isInScrollScope(targetView)) {
                                mMainScrollViewlayer = targetlevel;
                                mMainScrollView = childView;
                            }
                            printViewState(childView);
                        }
                        if (childView.isScrollContainer() && childView.getVisibility() == 0 && DEBUG) {
                            Log.d(TAG, "is scrollcontainer:" + childView.isScrollContainer() + " :" + childView);
                        }
                        childViewList.add(childView);
                    }
                }
            }
            if (childViewList.isEmpty()) {
                findViewComplete = true;
                parentViewList.clear();
                childViewList.clear();
            } else {
                parentViewList.clear();
                parentViewList.addAll(childViewList);
                childViewList.clear();
            }
            targetlevel++;
            if (DEBUG) {
                Log.d(TAG, " search level:" + targetlevel);
            }
        }
        View view = mMainScrollView;
        if (view != null) {
            int[] position = new int[2];
            mScrollContainerType = getScrollViewType(view);
            mMainScrollView.getLocationInWindow(position);
            int cotainerTop = position[1];
            setLongshotScope(cotainerTop, mMainScrollView.getHeight(), cotainerTop + mMainScrollView.getHeight());
        }
        printViewState(mMainScrollView);
        if (mMainScrollView != null) {
            return true;
        }
        return false;
    }

    private void printViewState(View view) {
        int cotainerTop = 0;
        int cotainerBottom = 0;
        int type = 0;
        if (view != null) {
            int[] position = new int[2];
            type = getScrollViewType(view);
            view.getLocationInWindow(position);
            cotainerTop = position[1];
            cotainerBottom = cotainerTop + view.getHeight();
        }
        if (DEBUG) {
            Log.d(TAG, "end searchMainScrollView mScrollContainerType:" + type + " cotainerTop:" + cotainerTop + " cotainerBottom:" + cotainerBottom);
        }
    }

    public void registerLongshotUtilCallback(LongshotUtilCallback callback) {
        mCallback = callback;
    }

    public void unRegisterLongshotUtilCallback(LongshotUtilCallback callback) {
        if (mCallback == callback) {
            mCallback = null;
        }
    }

    private boolean isOverScroll() {
        View view = mMainScrollView;
        if (view != null) {
            return !view.canScrollVertically(0);
        }
        return true;
    }

    public int getScrollViewType(View scrollContainer) {
        int type = 0;
        if (scrollContainer == null) {
            return 0;
        }
        if (scrollContainer instanceof ScrollView) {
            type = 1;
        } else if (scrollContainer instanceof WebView) {
            type = 2;
        } else if (scrollContainer instanceof GridView) {
            type = 3;
        } else if (isRecyclerView(scrollContainer)) {
            type = 4;
        }
        for (Class C = scrollContainer.getClass(); C != null; C = C.getSuperclass()) {
            if (DEBUG) {
                Log.d(TAG, "mMainScrollView tree:" + C.toString());
            }
        }
        return type;
    }

    private boolean isInScrollScope(View view) {
        int[] position = new int[2];
        view.getLocationInWindow(position);
        int cotainerTop = position[1];
        int cotainerBottom = view.getHeight() + cotainerTop;
        if (cotainerTop >= mLongshotScopeTop || cotainerBottom <= mLongshotScopeBottom) {
            return false;
        }
        return true;
    }

    private void setUtils() {
        View view = mMainScrollView;
        if (view != null) {
            view.setLongshotUtil(this);
        }
    }

    public void updateMainScrollView(View view) {
        boolean isDiff = view != mMainScrollView;
        Log.d(TAG, "updateMainScrollView:" + mMainScrollView + " type:" + getScrollViewType(mMainScrollView));
        if (isDiff) {
            Log.d(TAG, "change to:" + view + " type:" + getScrollViewType(view));
            View view2 = mMainScrollView;
            if (view2 != null) {
                view2.setLongshotUtil(null);
            }
            mMainScrollView = view;
            mMainScrollView.setLongshotUtil(this);
        }
    }

    private void searchScrollViewComplete() {
        setUtils();
        Log.d(TAG, "searchScrollViewComplete:" + mMainScrollView + " mDecordView:" + mDecordView + getScrollViewType(mMainScrollView));
    }

    public void onScrollChanged(Context context, int l, int t, int oldl, int oldt) {
        OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
        if (DEBUG) {
            Log.d(TAG, " onScrollChanged t:" + t + " oldt:" + oldt);
        }
        if (sm != null && sm.isLongshotMoveState()) {
            sm.notifyLongshotScrollChanged(l, t, oldl, oldt);
        }
    }

    public boolean isRecyclerView(View view) {
        boolean result = false;
        if (view == null) {
            return false;
        }
        if ((view instanceof AbsListView) || (view instanceof RecyclerView)) {
            return true;
        }
        for (Class C = view.getClass(); C != null; C = C.getSuperclass()) {
            if ("android.support.v7.widget.RecyclerView".equals(C.getName().toString())) {
                result = true;
            }
            if ("androidx.recyclerview.widget.RecyclerView".equals(C.getName().toString())) {
                result = true;
            }
        }
        return result;
    }
}
