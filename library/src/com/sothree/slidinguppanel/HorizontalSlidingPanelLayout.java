package com.sothree.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.nineoldandroids.view.animation.AnimatorProxy;
import com.sothree.slidinguppanel.library.R;

public class HorizontalSlidingPanelLayout extends SlidingPanelLayout {

    private static final String TAG = HorizontalSlidingPanelLayout.class.getSimpleName();

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_WIDTH = 68; // dp;

    /**
     * Default height of the shadow above the peeking out panel
     */
    private static final int DEFAULT_SHADOW_WIDTH = 4; // dp;

    /**
     * The size of the overhang in pixels.
     */
    private int mPanelWidth = -1;

    /**
     * The size of the shadow in pixels.
     */
    private int mShadowWidth = -1;

    public HorizontalSlidingPanelLayout(Context context) {
        this(context, null);
    }

    public HorizontalSlidingPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalSlidingPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            mShadowDrawable = null;
            mDragHelper = null;
            return;
        }

        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            defAttrs.recycle();

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.HorizontalSlidingPanelLayout);

            if (ta != null) {
                mPanelWidth = ta.getDimensionPixelSize(R.styleable.HorizontalSlidingPanelLayout_umanoPanelWidth, -1);
                mShadowWidth = ta.getDimensionPixelSize(R.styleable.HorizontalSlidingPanelLayout_umanoShadowWidth, -1);
            }

            ta.recycle();
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelWidth == -1) {
            mPanelWidth = (int) (DEFAULT_PANEL_WIDTH * density + 0.5f);
        }
        if (mShadowWidth == -1) {
            mShadowWidth = (int) (DEFAULT_SHADOW_WIDTH * density + 0.5f);
        }
        // If the shadow height is zero, don't show the shadow
        if (mShadowWidth > 0) {
            if (mIsSliding) {
                mShadowDrawable = getResources().getDrawable(R.drawable.to_start_of_shadow);
            } else {
                mShadowDrawable = getResources().getDrawable(R.drawable.to_end_of_shadow);
            }

        } else {
            mShadowDrawable = null;
        }

        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity * density);
    }

    public void setGravity(int gravity) {
        if (gravity != Gravity.LEFT && gravity != Gravity.RIGHT) {
            throw new IllegalArgumentException("gravity must be set to either left or right");
        }
        mIsSliding = gravity == Gravity.RIGHT;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * Set the collapsed panel width in pixels
     *
     * @param val A width in pixels
     */
    public void setPanelWidth(int val) {
        mPanelWidth = val;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * @return The current shadow width
     */
    public int getShadowWidth() {
        return mShadowWidth;
    }

    /**
     * Set the shadow width
     *
     * @param val A width in pixels
     */
    public void setShadowWidth(int val) {
        mShadowWidth = val;
        if (!mFirstLayout) {
            invalidate();
        }
    }

    /**
     * @return The current collapsed panel width
     */
    public int getPanelWidth() {
        return mPanelWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2) {
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
        }

        mMainView = getChildAt(0);
        mSlideableView = getChildAt(1);
        if (mDragView == null) {
            setDragView(mSlideableView);
        }

        // If the sliding panel is not visible, then put the whole view in the hidden state
        if (mSlideableView.getVisibility() != VISIBLE) {
            mSlideState = PanelState.HIDDEN;
        }

        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // We always measure the sliding panel in order to know it's height (needed for show panel)
            if (child.getVisibility() == GONE && i == 0) {
                continue;
            }

            int width = layoutWidth;
            if (child == mMainView && !mOverlayContent && mSlideState != PanelState.HIDDEN) {
                width -= mPanelWidth;
            }

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
            } else if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);

            if (child == mSlideableView) {
                mSlideRange = mSlideableView.getMeasuredWidth() - mPanelWidth;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingStart = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    int newStart = computePanelStartPosition(0.0f) + (mIsSliding ? +mPanelWidth : -mPanelWidth);
                    mSlideOffset = computeSlideOffset(newStart);
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            // Always layout the sliding view on the first layout
            if (child.getVisibility() == GONE && (i == 0 || mFirstLayout)) {
                continue;
            }

            final int childWidth = child.getMeasuredWidth();
            int childStart = paddingStart;

            if (child == mSlideableView) {
                childStart = computePanelStartPosition(mSlideOffset);
            }

            if (!mIsSliding) {
                if (child == mMainView && !mOverlayContent) {
                    childStart = computePanelStartPosition(mSlideOffset) + mSlideableView.getMeasuredWidth();
                }
            }
            final int childTop = paddingTop;
            final int childBottom = childTop + child.getMeasuredHeight();
            final int childEnd = childStart + childWidth;

            child.layout(childStart, childTop, childEnd, childBottom);
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (w != oldw) {
            mFirstLayout = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);


        if (!isEnabled() || !isTouchEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int dragSlop = mDragHelper.getTouchSlop();

                // Handle any vertical scrolling on the drag view.
                if (mIsUsingDragViewTouchEvents && adx < dragSlop && ady > dragSlop) {
                    return super.onInterceptTouchEvent(ev);
                }

                if ((adx > dragSlop && ady > adx) || !isDragViewUnder((int) mInitialMotionX, (int) mInitialMotionY)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }
        }

        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    /*
     * Computes the start position of the panel based on the slide offset.
     */
    private int computePanelStartPosition(float slideOffset) {
        int slidingViewWidth = mSlideableView != null ? mSlideableView.getMeasuredWidth() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mIsSliding
                ? getMeasuredWidth() - getPaddingRight() - mPanelWidth - slidePixelOffset
                : getPaddingLeft() - slidingViewWidth + mPanelWidth + slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the start position of the panel
     */
    private float computeSlideOffset(int startPosition) {
        // Compute the panel start position if the panel is collapsed (offset 0)
        final int startBoundCollapsed = computePanelStartPosition(0);

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (mIsSliding
                ? (float) (startBoundCollapsed - startPosition) / mSlideRange
                : (float) (startPosition - startBoundCollapsed) / mSlideRange);
    }

    @Override
    public void setPanelState(PanelState state) {
        if (state == null || state == PanelState.DRAGGING) {
            throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
        }
        if (!isEnabled()
                || mSlideableView == null
                || state == mSlideState
                || mSlideState == PanelState.DRAGGING) return;

        if (mFirstLayout) {
            mSlideState = state;
        } else {
            if (mSlideState == PanelState.HIDDEN) {
                mSlideableView.setVisibility(View.VISIBLE);
                requestLayout();
            }
            switch (state) {
                case ANCHORED:
                    smoothSlideTo(mAnchorPoint, 0);
                    break;
                case COLLAPSED:
                    smoothSlideTo(0, 0);
                    break;
                case EXPANDED:
                    smoothSlideTo(1.0f, 0);
                    break;
                case HIDDEN:
                    int newStart = computePanelStartPosition(0.0f) + (mIsSliding ? +mPanelWidth : -mPanelWidth);
                    smoothSlideTo(computeSlideOffset(newStart), 0);
                    break;
            }
        }
    }

    @SuppressLint("NewApi")
    private void onPanelDragged(int newStart) {
        mSlideState = PanelState.DRAGGING;
        // Recompute the slide offset based on the new top position
        mSlideOffset = computeSlideOffset(newStart);
        // Update the parallax based on the new slide offset
        if (mParallaxOffset > 0 && mSlideOffset >= 0) {
            int mainViewOffset = getCurrentParalaxOffset();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mMainView.setTranslationX(mainViewOffset);
            } else {
                AnimatorProxy.wrap(mMainView).setTranslationX(mainViewOffset);
            }
        }
        // Dispatch the slide event
        dispatchOnPanelSlide(mSlideableView);
        // If the slide offset is negative, and overlay is not on, we need to increase the width of the main content
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        int defaultWidth = getWidth() - getPaddingEnd() - getPaddingStart() - mPanelWidth;

        if (mSlideOffset <= 0 && !mOverlayContent) {
            // expand the main view
            lp.width = mIsSliding ? (newStart - getPaddingEnd()) : (getWidth() - getPaddingEnd() -
                    mSlideableView.getMeasuredWidth() - newStart);
            mMainView.requestLayout();
        } else if (lp.width != defaultWidth && !mOverlayContent) {
            lp.width = defaultWidth;
            mMainView.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result;
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

        if (mSlideableView != child) { // if main view
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            canvas.getClipBounds(mTmpRect);
            if (!mOverlayContent) {
                if (mIsSliding) {
                    mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
                } else {
                    mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
                }
            }
            if (mClipPanel) {
                canvas.clipRect(mTmpRect);
            }

            result = super.drawChild(canvas, child, drawingTime);

            if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
                final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
                final int imag = (int) (baseAlpha * mSlideOffset);
                final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);
                mCoveredFadePaint.setColor(color);
                canvas.drawRect(mTmpRect, mCoveredFadePaint);
            }
        } else {
            result = super.drawChild(canvas, child, drawingTime);
        }

        canvas.restoreToCount(save);

        return result;
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity    initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!isEnabled()) {
            // Nothing to do.
            return false;
        }

        int panelStart = computePanelStartPosition(slideOffset);
        if (mDragHelper.smoothSlideViewTo(mSlideableView, panelStart, mSlideableView.getTop())) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        // draw the shadow
        if (mShadowDrawable != null) {
            final int left;
            final int right;
            final int top = mSlideableView.getTop();
            final int bottom = mSlideableView.getBottom();
            if (mIsSliding) {
                left = mSlideableView.getLeft() - mShadowWidth;
                right = mSlideableView.getRight();
            } else {
                left = mSlideableView.getRight();
                right = mSlideableView.getRight() + mShadowWidth;
            }
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }

            return child == mSlideableView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mSlideableView.getLeft());

                if (mSlideOffset == 1) {
                    if (mSlideState != PanelState.EXPANDED) {
                        updateObscuredViewVisibility();
                        mSlideState = PanelState.EXPANDED;
                        dispatchOnPanelExpanded(mSlideableView);
                    }
                } else if (mSlideOffset == 0) {
                    if (mSlideState != PanelState.COLLAPSED) {
                        mSlideState = PanelState.COLLAPSED;
                        dispatchOnPanelCollapsed(mSlideableView);
                    }
                } else if (mSlideOffset < 0) {
                    mSlideState = PanelState.HIDDEN;
                    mSlideableView.setVisibility(View.INVISIBLE);
                    dispatchOnPanelHidden(mSlideableView);
                } else if (mSlideState != PanelState.ANCHORED) {
                    updateObscuredViewVisibility();
                    mSlideState = PanelState.ANCHORED;
                    dispatchOnPanelAnchored(mSlideableView);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(left);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target = 0;

            // direction is always positive if we are sliding in the expanded direction
            float direction = mIsSliding ? -xvel : xvel;

            if (direction > 0) {
                // swipe left -> expand
                target = computePanelStartPosition(1.0f);
            } else if (direction < 0) {
                // swipe down -> collapse
                target = computePanelStartPosition(0.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= (1.f + mAnchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelStartPosition(1.0f);
            } else if (mAnchorPoint == 1 && mSlideOffset >= 0.5f) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelStartPosition(1.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint) {
                target = computePanelStartPosition(mAnchorPoint);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint / 2) {
                target = computePanelStartPosition(mAnchorPoint);
            } else {
                // settle at the bottom
                target = computePanelStartPosition(0.0f);
            }

            mDragHelper.settleCapturedViewAt(target, releasedChild.getTop());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int collapsedStart = computePanelStartPosition(0.f);
            final int expandedStart = computePanelStartPosition(1.0f);
            if (mIsSliding) {
                return Math.min(Math.max(left, expandedStart), collapsedStart);
            } else {
                return Math.min(Math.max(left, collapsedStart), expandedStart);
            }
        }
    }
}
