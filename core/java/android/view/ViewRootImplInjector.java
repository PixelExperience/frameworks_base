package android.view;

import android.content.Context;
import android.os.Handler;

import com.android.internal.custom.longshot.LongshotUtil;

public class ViewRootImplInjector {
    private static final String TAG = "ViewRootImplInjector";
    private static LongshotUtil mLongshotUtil;
    Context mContext;
    ViewRootImpl mViewRootImpl;
    private LongshotRunnable mLongshotStartRunnable = new LongshotRunnable() {
        View mView;

        @Override
        public void setView(View view) {
            mView = view;
        }

        @Override
        public void run() {
            View view = mView;
            if (view instanceof ViewGroup) {
                mLongshotUtil = new LongshotUtil(view.getContext(), (ViewGroup) mView);
                mLongshotUtil.longshotStart();
                View view2 = mView;
                if (view2 != null) {
                    view2.setLongshotUtil(mLongshotUtil);
                }
            }
        }
    };
    private LongshotRunnable mLongshotStopRunnable = new LongshotRunnable() {
        View mView;

        @Override
        public void run() {
            if (mLongshotUtil != null) {
                mLongshotUtil.longshotStop();
                View view = mView;
                if (view != null) {
                    view.setLongshotUtil(null);
                }
                mLongshotUtil = null;
            }
        }

        @Override
        public void setView(View view) {
            mView = view;
        }
    };

    public ViewRootImplInjector(ViewRootImpl viewRoot) {
        mViewRootImpl = viewRoot;
        mContext = viewRoot.mContext;
    }

    public void longshotStart(Handler handler, View view) {
        mLongshotStartRunnable.setView(view);
        handler.post(mLongshotStartRunnable);
    }

    public void longshotStop(Handler handler, View view) {
        mLongshotStopRunnable.setView(view);
        handler.post(mLongshotStopRunnable);
    }

    private interface LongshotRunnable extends Runnable {
        void setView(View view);
    }
}
