package android.view;

import android.os.Build;
import android.util.Log;

import com.android.internal.custom.longshot.LongshotUtil;

public class ViewGroupInjector {
    private boolean LONGSHOT_DEBUG = false;
    private String LONGSHOT_TAG = "Longshot.ViewGroup";
    private int mLastItemTop;
    private View mLastListItem;

    public ViewGroupInjector() {
        mLastItemTop = 0;
    }

    /** @hide */
    public void updateLastItem(ViewGroup viewGroup, LongshotUtil longshotUtil) {
        if (viewGroup != null && longshotUtil != null) {
            View lastchild = viewGroup.getChildAt(viewGroup.getChildCount() - 1);
            int h = viewGroup.getResources().getDisplayMetrics().heightPixels;
            mLastListItem = lastchild;
            int[] position = new int[2];
            mLastListItem.getLocationOnScreen(position);
            updateLastTop(h - position[1]);
        }
    }

    /** @hide */
    public void onScrolled(ViewGroup viewGroup, LongshotUtil longshotUtil) {
        if (viewGroup != null && longshotUtil != null && mLastListItem != null) {
            int h = viewGroup.getResources().getDisplayMetrics().heightPixels;
            int[] position = new int[2];
            mLastListItem.getLocationOnScreen(position);
            int top = h - position[1];
            int swipe = mLastItemTop - top;
            if (LONGSHOT_DEBUG) {
                Log.d(LONGSHOT_TAG, "onScrolled swipe:" + swipe + " mLastItemTop:" + mLastItemTop + " top:" + top);
            }
            viewGroup.onScrollChangedForLongshot(0, top, 0, mLastItemTop);
            updateLastTop(top);
        }
    }

    private void updateLastTop(int top) {
        if (LONGSHOT_DEBUG) {
            Log.d(LONGSHOT_TAG, "updateLastTop:" + mLastItemTop + " to:" + top);
        }
        mLastItemTop = top;
    }

    /** @hide */
    public void onScrollChanged(ViewGroup viewGroup, int l, int t, int oldl, int oldt, LongshotUtil longshotUtil) {
        if (viewGroup != null) {
            viewGroup.onScrollChanged(l, t, oldl, oldt);
            if (longshotUtil != null && !viewGroup.isGeneralScrollView()) {
                onScrolled(viewGroup, longshotUtil);
            }
        }
    }
}
