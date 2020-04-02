package com.android.internal.custom.longshot.injector;

import android.content.Context;
import android.os.ServiceManager;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.android.internal.custom.longshot.OpLongScreenshotManagerService;

public class OpViewInjector {

    public static class View {
        private static final List<Element> ELEMENTS_NOOVERSCROLL = new ArrayList();
        private static final List<Element> ELEMENTS_NOSCROLL = new ArrayList();
        private static final List<Element> ELEMENTS_OVERSCROLL = new ArrayList();
        private static final List<Element> ELEMENTS_SCROLL = new ArrayList();
        private static final String TAG = "ViewInjector";
        public static boolean isInjection = false;

        public static void onUnscrollableView(Context context) {
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (isInjection && sm != null) {
                sm.onUnscrollableView();
            }
        }

        public static void setScrolledViewTop(Context context, int top) {
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (isInjection && sm != null) {
                sm.notifyScrollViewTop(top);
            }
        }

        public static void onOverScrolled(Context context, boolean isOverScroll) {
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (isInjection && sm != null && sm.isLongshotMoveState()) {
                StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
                ELEMENTS_OVERSCROLL.add(Element.LISTOVERSCROLL);
                ELEMENTS_NOOVERSCROLL.add(Element.WEBOVERSCROLL);
                ELEMENTS_NOOVERSCROLL.add(Element.BROWSERSCROLL);
                ELEMENTS_NOOVERSCROLL.add(Element.BROWSEROVERSCROLL);
                if (!isElement(stacks, ELEMENTS_NOOVERSCROLL)) {
                    if (isElement(stacks, ELEMENTS_OVERSCROLL)) {
                        Slog.d(TAG, "onOverScrolled:no more scroll down");
                        sm.notifyLongshotScroll(true);
                    } else {
                        sm.notifyLongshotScroll(false);
                    }
                }
                clearElements();
            }
        }

        public static void onScrollChanged(Context context, boolean canScrollVertically) {
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (isInjection && sm != null && sm.isLongshotMoveState()) {
                StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
                ELEMENTS_NOSCROLL.add(Element.MMCHANGE9);
                ELEMENTS_NOSCROLL.add(Element.MMCHANGE12);
                ELEMENTS_NOSCROLL.add(Element.MMCHANGE14);
                ELEMENTS_NOSCROLL.add(Element.MMCHANGE15);
                ELEMENTS_NOSCROLL.add(Element.CONTENTSCROLL);
                ELEMENTS_NOSCROLL.add(Element.BROWSERSCROLL);
                ELEMENTS_NOSCROLL.add(Element.QZONESCROLL);
                ELEMENTS_NOSCROLL.add(Element.WEBSCROLL);
                if (!isElement(stacks, ELEMENTS_NOSCROLL)) {
                    if (!canScrollVertically) {
                        Slog.d(TAG, "onScrollChanged:no more scroll down");
                        sm.notifyLongshotScroll(true);
                    } else {
                        sm.notifyLongshotScroll(false);
                    }
                }
                clearElements();
            }
        }

        public static boolean onAwakenScrollBars(Context context) {
            if (!isInjection) {
                return false;
            }
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (sm != null && !sm.isLongshotMoveState()) {
                StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
                ELEMENTS_OVERSCROLL.add(Element.OVERSCROLL);
                ELEMENTS_NOSCROLL.add(Element.MMAWAKEN12);
                ELEMENTS_NOSCROLL.add(Element.MMAWAKEN14);
                ELEMENTS_NOSCROLL.add(Element.MMAWAKEN15);
                ELEMENTS_SCROLL.add(Element.QQSCROLL);
                ELEMENTS_SCROLL.add(Element.SCROLL);
                if (!isElement(stacks, ELEMENTS_NOSCROLL)) {
                    if (isElement(stacks, ELEMENTS_OVERSCROLL)) {
                        Slog.d(TAG, "onAwakenScrollBars:no more scroll down");
                        sm.notifyLongshotScroll(true);
                    } else if (isElement(stacks, ELEMENTS_SCROLL)) {
                        sm.notifyLongshotScroll(false);
                    }
                }
                clearElements();
            }
            return false;
        }

        private static boolean isElement(StackTraceElement[] stacks, List<Element> elements) {
            boolean result = false;
            for (Element element : elements) {
                int pos = element.getPosition();
                if (stacks.length > pos) {
                    result = stacks[pos].toString().contains(element.getNameString());
                    if (result) {
                        break;
                    }
                }
            }
            return result;
        }

        private static void clearElements() {
            ELEMENTS_SCROLL.clear();
            ELEMENTS_NOSCROLL.clear();
            ELEMENTS_OVERSCROLL.clear();
            ELEMENTS_NOOVERSCROLL.clear();
        }

        private enum Element {
            SCROLL(5, "AbsListView.trackMotionScroll"),
            QQSCROLL(7, "tencent.widget.AbsListView.onTouchEvent"),
            MMAWAKEN12(12, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            MMAWAKEN14(14, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            MMAWAKEN15(15, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            OVERSCROLL(5, "AbsListView.onOverScrolled"),
            CONTENTSCROLL(4, "ContentView.onScrollChanged"),
            MMCHANGE9(9, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            MMCHANGE12(12, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            MMCHANGE14(14, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            MMCHANGE15(15, "tencent.mm.ui.base.MMPullDownView.dispatchTouchEvent"),
            BROWSERSCROLL(14, "oppo.browser.navigation.widget.NavigationView.dispatchTouchEvent"),
            QZONESCROLL(8, "qzone.widget.QZonePullToRefreshListView.onScrollChanged"),
            WEBSCROLL(16, "WebView$PrivateAccess.overScrollBy"),
            LISTOVERSCROLL(6, "AbsListView.onTouchEvent"),
            WEBOVERSCROLL(5, "WebView$PrivateAccess.overScrollBy"),
            BROWSEROVERSCROLL(11, "oppo.browser.navigation.widget.NavigationView.dispatchTouchEvent");
            
            private String mName = null;
            private int mPosition = -1;

            private Element(int position, String name) {
                this.mPosition = position;
                this.mName = name;
            }

            public int getPosition() {
                return this.mPosition;
            }

            public String getName() {
                return this.mName;
            }

            public String getNameString() {
                return "." + getName() + "(";
            }
        }
    }
}
