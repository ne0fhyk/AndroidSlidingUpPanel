package com.sothree.slidinguppanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.sothree.slidinguppanel.library.R;

/**
 * Created by Fredia Huya-Kouadio on 2/17/15.
 */
public abstract class SlidingPanelLayout extends ViewGroup {

    /**
     * Default anchor point height
     */
    protected static final float DEFAULT_ANCHOR_POINT = 1.0f; // In relative %

    /**
     * Default initial state for the component
     */
    protected static PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    protected static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * Default Minimum velocity that will be detected as a fling
     */
    protected static final int DEFAULT_MIN_FLING_VELOCITY = 1200; // dips per second
    /**
     * Default is set to false because that is how it was written
     */
    protected static final boolean DEFAULT_OVERLAY_FLAG = false;
    /**
     * Default is set to true for clip panel for performance reasons
     */
    protected static final boolean DEFAULT_CLIP_PANEL_FLAG = true;
    /**
     * Default attributes for layout
     */
    protected static final int[] DEFAULT_ATTRS = new int[] {
            android.R.attr.gravity
    };

    /**
     * Minimum velocity that will be detected as a fling
     */
    protected int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    protected int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    /**
     * Default paralax length of the main view
     */
    protected static final int DEFAULT_PARALAX_OFFSET = 0;

    /**
     * The paint used to dim the main layout when sliding
     */
    protected final Paint mCoveredFadePaint = new Paint();

    /**
     * Current state of the slideable view.
     */
    public enum PanelState {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        HIDDEN,
        DRAGGING
    }
    protected PanelState mSlideState = PanelState.COLLAPSED;

    /**
     * Paralax offset
     */
    protected int mParallaxOffset = -1;

    /**
     * True if the collapsed panel should be dragged up.
     */
    protected boolean mIsSliding;

    /**
     * Panel overlays the windows instead of putting it underneath it.
     */
    protected boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;

    /**
     * The main view is clipped to the main top border
     */
    protected boolean mClipPanel = DEFAULT_CLIP_PANEL_FLAG;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    protected View mDragView;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    protected int mDragViewResId = -1;

    /**
     * The child view that can slide, if any.
     */
    protected View mSlideableView;

    /**
     * The main view
     */
    protected View mMainView;

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    protected float mSlideOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    protected int mSlideRange;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    protected boolean mIsUnableToDrag;

    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    protected boolean mIsTouchEnabled;

    /**
     * Flag indicating if a drag view can have its own touch events.  If set
     * to true, a drag view can scroll horizontally and have its own click listener.
     *
     * Default is set to false.
     */
    protected boolean mIsUsingDragViewTouchEvents;

    protected float mInitialMotionX;
    protected float mInitialMotionY;
    protected float mAnchorPoint = 1.f;

    protected PanelSlideListener mPanelSlideListener;

    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    protected boolean mFirstLayout = true;

    protected final Rect mTmpRect = new Rect();

    /**
     * Drawable used to draw the shadow between panes.
     */
    protected Drawable mShadowDrawable;

    protected ViewDragHelper mDragHelper;

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a sliding pane's position changes.
         * @param panel The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        public void onPanelSlide(View panel, float slideOffset);
        /**
         * Called when a sliding panel becomes slid completely collapsed.
         * @param panel The child view that was slid to an collapsed position
         */
        public void onPanelCollapsed(View panel);

        /**
         * Called when a sliding panel becomes slid completely expanded.
         * @param panel The child view that was slid to a expanded position
         */
        public void onPanelExpanded(View panel);

        /**
         * Called when a sliding panel becomes anchored.
         * @param panel The child view that was slid to a anchored position
         */
        public void onPanelAnchored(View panel);

        /**
         * Called when a sliding panel becomes completely hidden.
         * @param panel The child view that was slid to a hidden position
         */
        public void onPanelHidden(View panel);
    }

    /**
     * No-op stubs for {@link PanelSlideListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }
        @Override
        public void onPanelCollapsed(View panel) {
        }
        @Override
        public void onPanelExpanded(View panel) {
        }
        @Override
        public void onPanelAnchored(View panel) {
        }
        @Override
        public void onPanelHidden(View panel) {
        }
    }


    public SlidingPanelLayout(Context context) {
        super(context);
    }

    public SlidingPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingPanelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            if (defAttrs != null) {
                int gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);
                setGravity(gravity);
            }

            defAttrs.recycle();

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingPanelLayout);

            if (ta != null) {
                mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlidingPanelLayout_umanoParalaxOffset, -1);

                mMinFlingVelocity = ta.getInt(R.styleable.SlidingPanelLayout_umanoFlingVelocity, DEFAULT_MIN_FLING_VELOCITY);
                mCoveredFadeColor = ta.getColor(R.styleable.SlidingPanelLayout_umanoFadeColor, DEFAULT_FADE_COLOR);

                mDragViewResId = ta.getResourceId(R.styleable.SlidingPanelLayout_umanoDragView, -1);

                mOverlayContent = ta.getBoolean(R.styleable.SlidingPanelLayout_umanoOverlay, DEFAULT_OVERLAY_FLAG);
                mClipPanel = ta.getBoolean(R.styleable.SlidingPanelLayout_umanoClipPanel, DEFAULT_CLIP_PANEL_FLAG);

                mAnchorPoint = ta.getFloat(R.styleable.SlidingPanelLayout_umanoAnchorPoint, DEFAULT_ANCHOR_POINT);

                mSlideState = PanelState.values()[ta.getInt(R.styleable.SlidingPanelLayout_umanoInitialState, DEFAULT_SLIDE_STATE.ordinal())];
            }

            ta.recycle();
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mParallaxOffset == -1) {
            mParallaxOffset = (int) (DEFAULT_PARALAX_OFFSET * density);
        }

        setWillNotDraw(false);

        mIsTouchEnabled = true;
    }

    /**
     * Set the Drag View after the view is inflated
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            setDragView(findViewById(mDragViewResId));
        }
    }

        public abstract void setGravity(int gravity);

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the expanded state.
     *
     * @param color An ARGB-packed color value
     */
    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        invalidate();
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     */
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * Set sliding enabled flag
     *
     * @param enabled flag value
     */
    public void setTouchEnabled(boolean enabled) {
        mIsTouchEnabled = enabled;
    }

    public boolean isTouchEnabled() {
        return mIsTouchEnabled && mSlideableView != null;
    }

    /**
     * @return The current paralax offset
     */
    public int getCurrentParalaxOffset() {
        // Clamp slide offset at zero for parallax computation;
        int offset = (int) (mParallaxOffset * Math.max(mSlideOffset, 0));
        return mIsSliding ? -offset : offset;
    }

    /**
     * Set parallax offset for the panel
     *
     * @param val A height in pixels
     */
    public void setParalaxOffset(int val) {
        mParallaxOffset = val;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * @return The current minimin fling velocity
     */
    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    /**
     * Sets the minimum fling velocity for the panel
     *
     * @param val the new value
     */
    public void setMinFlingVelocity(int val) {
        mMinFlingVelocity = val;
    }

    /**
     * Sets the panel slide listener
     *
     * @param listener
     */
    public void setPanelSlideListener(PanelSlideListener listener) {
        mPanelSlideListener = listener;
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    public void setDragView(View dragView) {
        if (mDragView != null) {
            mDragView.setOnClickListener(null);
        }
        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            mDragView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isEnabled() || !isTouchEnabled()) return;
                    if (mSlideState != PanelState.EXPANDED && mSlideState != PanelState.ANCHORED) {
                        if (mAnchorPoint < 1.0f) {
                            setPanelState(PanelState.ANCHORED);
                        } else {
                            setPanelState(PanelState.EXPANDED);
                        }
                    } else {
                        setPanelState(PanelState.COLLAPSED);
                    }
                }
            });
            ;
        }
    }

    /**
     * Change panel state to the given state with
     *
     * @param state - new panel state
     */
    public abstract void setPanelState(PanelState state);

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragViewResId The resource ID of the new drag view
     */
    public void setDragView(int dragViewResId) {
        mDragViewResId = dragViewResId;
        setDragView(findViewById(dragViewResId));
    }

    /**
     * Set an anchor point where the panel can stop during sliding
     *
     * @param anchorPoint A value between 0 and 1, determining the position of the anchor point
     *                    starting from the top of the layout.
     */
    public void setAnchorPoint(float anchorPoint) {
        if (anchorPoint > 0 && anchorPoint <= 1) {
            mAnchorPoint = anchorPoint;
        }
    }

    /**
     * Gets the currently set anchor point
     *
     * @return the currently set anchor point
     */
    public float getAnchorPoint() {
        return mAnchorPoint;
    }

    public float getSlideOffset() {
        return mSlideOffset;
    }

    /**
     * Sets whether or not the panel overlays the content
     *
     * @param overlayed
     */
    public void setOverlayed(boolean overlayed) {
        mOverlayContent = overlayed;
    }

    /**
     * Check if the panel is set as an overlay.
     */
    public boolean isOverlayed() {
        return mOverlayContent;
    }

    /**
     * Sets whether or not the main content is clipped to the top of the panel
     *
     * @param clip
     */
    public void setClipPanel(boolean clip) {
        mClipPanel = clip;
    }

    /**
     * Check whether or not the main content is clipped to the top of the panel
     */
    public boolean isClipPanel() {
        return mClipPanel;
    }

    void dispatchOnPanelSlide(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelSlide(panel, mSlideOffset);
        }
    }

    void dispatchOnPanelExpanded(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelExpanded(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelCollapsed(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelCollapsed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelAnchored(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelAnchored(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelHidden(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelHidden(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    protected static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    /**
     * Set if the drag view can have its own touch events.  If set
     * to true, a drag view can scroll horizontally and have its own click listener.
     * <p/>
     * Default is set to false.
     */
    public void setEnableDragViewTouchEvents(boolean enabled) {
        mIsUsingDragViewTouchEvents = enabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isTouchEnabled()) {
            return super.onTouchEvent(ev);
        }
        mDragHelper.processTouchEvent(ev);
        return true;
    }

    protected boolean isDragViewUnder(int x, int y) {
        if (mDragView == null) return false;
        int[] viewLocation = new int[2];
        mDragView.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + mDragView.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + mDragView.getHeight();
    }

    /**
     * Returns the current state of the panel as an enum.
     *
     * @return the current panel state
     */
    public PanelState getPanelState() {
        return mSlideState;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mSlideState = mSlideState;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSlideState = ss.mSlideState;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            a.recycle();
        }

    }

    static class SavedState extends BaseSavedState {
        PanelState mSlideState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            try {
                mSlideState = Enum.valueOf(PanelState.class, in.readString());
            } catch (IllegalArgumentException e) {
                mSlideState = PanelState.COLLAPSED;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(mSlideState.toString());
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
