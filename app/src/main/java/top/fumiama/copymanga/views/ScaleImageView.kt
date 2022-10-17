package top.fumiama.copymanga.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.widget.ImageView
import top.fumiama.copymanga.ui.vm.PagesManager
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.sqrt

/**
 * 手势图片控件
 *
 * @author clifford
 */
class ScaleImageView : ImageView {
    ////////////////////////////////监听器////////////////////////////////
    /**
     * 外界点击事件
     *
     * @see .setOnClickListener
     */
    private var mOnClickListener: OnClickListener? = null

    /**
     * 外界长按事件
     *
     * @see .setOnLongClickListener
     */
    private var mOnLongClickListener: OnLongClickListener? = null
    override fun setOnClickListener(l: OnClickListener?) {
        //默认的click会在任何点击情况下都会触发，所以搞成自己的
        mOnClickListener = l
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        //默认的long click会在任何长按情况下都会触发，所以搞成自己的
        mOnLongClickListener = l
    }

    /**
     * 外层变换矩阵，如果是单位矩阵，那么图片是fit center状态
     *
     * @see .getOuterMatrix
     * @see .outerMatrixTo
     */
    private val mOuterMatrix = Matrix()

    /**
     * 矩形遮罩
     *
     * @see .getMask
     * @see .zoomMaskTo
     */
    private var mMask: RectF? = null

    /**
     * 获取当前手势状态
     *
     * @see .PINCH_MODE_FREE
     *
     * @see .PINCH_MODE_SCROLL
     *
     * @see .PINCH_MODE_SCALE
     */
    /**
     * 当前手势状态
     *
     * @see .getPinchMode
     * @see .PINCH_MODE_FREE
     *
     * @see .PINCH_MODE_SCROLL
     *
     * @see .PINCH_MODE_SCALE
     */
    var pinchMode = PINCH_MODE_FREE
        private set

    /**
     * 获取外部变换矩阵.
     *
     * 外部变换矩阵记录了图片手势操作的最终结果,是相对于图片fit center状态的变换.
     * 默认值为单位矩阵,此时图片为fit center状态.
     *
      @param matrix 用于填充结果的对象
      @return 如果传了matrix参数则将matrix填充后返回,否则new一个填充返回

    fun getOuterMatrix(matrix: Matrix?): Matrix {
        var matrix = matrix
        if (matrix == null) {
            matrix = Matrix(mOuterMatrix)
        } else {
            matrix.set(mOuterMatrix)
        }
        return matrix
    }*/

    /**
     * 获取内部变换矩阵.
     *
     * 内部变换矩阵是原图到fit center状态的变换,当原图尺寸变化或者控件大小变化都会发生改变
     * 当尚未布局或者原图不存在时,其值无意义.所以在调用前需要确保前置条件有效,否则将影响计算结果.
     *
     * @param matrix 用于填充结果的对象
     * @return 如果传了matrix参数则将matrix填充后返回,否则new一个填充返回
     */
    private fun getInnerMatrix(matrix: Matrix?): Matrix {
        val m = matrix?:Matrix().let {
            it.reset()
            it
        }
        if (isReady) {
            val imgX = drawable.intrinsicWidth.toFloat()
            val imgY = drawable.intrinsicHeight.toFloat()
            //原图大小
            val tempSrc = rectFTake(0f, 0f, imgX, imgY)
            //layoutParams.height = (imgY / imgX * width + 0.5).toInt()
            //invalidate()
            //控件大小
            val tempDst = rectFTake(0f, 0f, width.toFloat(), height.toFloat())
            //计算fit center矩阵
            m.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER)
            //释放临时对象
            rectFGiven(tempDst)
            rectFGiven(tempSrc)
        }
        return m
    }

    fun setHeight2FitImgWidth(){
        if(matrix != null && drawable != null && layoutParams != null){
            matrix.reset()
            val imgX = drawable.intrinsicWidth.toFloat()
            val imgY = drawable.intrinsicHeight.toFloat()
            //Log.d("MySIV", "ix: $imgX, iy: $imgY, w: $width, h: $height")
            //原图大小
            val tempSrc = rectFTake(0f, 0f, imgX, imgY)
            layoutParams.height = (imgY / imgX * width + 0.5).toInt()
            invalidate()
            //控件大小
            val tempDst = rectFTake(0f, 0f, width.toFloat(), height.toFloat())
            //计算fit center矩阵
            matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER)
            //释放临时对象
            rectFGiven(tempDst)
            rectFGiven(tempSrc)
        }
    }

    /**
     * 获取图片总变换矩阵.
     *
     * 总变换矩阵为内部变换矩阵x外部变换矩阵,决定了原图到所见最终状态的变换
     * 当尚未布局或者原图不存在时,其值无意义.所以在调用前需要确保前置条件有效,否则将影响计算结果.
     *
     * @param matrix 用于填充结果的对象
     * @return 如果传了matrix参数则将matrix填充后返回,否则new一个填充返回
     *
     * @see .getOuterMatrix
     * @see .getInnerMatrix
     */
    private fun getCurrentImageMatrix(matrix: Matrix): Matrix {
        //获取内部变换矩阵
        val m = getInnerMatrix(matrix)
        //乘上外部变换矩阵
        m.postConcat(mOuterMatrix)
        return m
    }

    /**
     * 获取当前变换后的图片位置和尺寸
     *
     * 当尚未布局或者原图不存在时,其值无意义.所以在调用前需要确保前置条件有效,否则将影响计算结果.
     *
     * @param rectF 用于填充结果的对象
     * @return 如果传了rectF参数则将rectF填充后返回,否则new一个填充返回
     *
     * @see .getCurrentImageMatrix
     */
    private fun getImageBound(rectF: RectF?): RectF {
        var rf = rectF
        if (rf == null) rf = RectF() else rf.setEmpty()
        if (isReady) {
            //申请一个空matrix
            val matrix = matrixTake()
            //获取当前总变换矩阵
            getCurrentImageMatrix(matrix)
            //对原图矩形进行变换得到当前显示矩形
            rf[0f, 0f, drawable.intrinsicWidth.toFloat()] = drawable.intrinsicHeight.toFloat()
            matrix.mapRect(rf)
            //释放临时matrix
            matrixGiven(matrix)
        }
        return rf
    }

    /**
     * 获取当前设置的mask
     *
     * @return 返回当前的mask对象副本,如果当前没有设置mask则返回null

    val mask: RectF?
        get() = if (mMask != null) {
            RectF(mMask)
        } else {
            null
        }*/

    /**
     * 与ViewPager结合的时候使用
     * @param direction
     * @return
     */
    override fun canScrollHorizontally(direction: Int): Boolean {
        if (pinchMode == PINCH_MODE_SCALE) {
            return true
        }
        val bound = getImageBound(null)
        if (bound.isEmpty) {
            return false
        }
        return if (direction > 0) {
            bound.right > width
        } else {
            bound.left < 0
        }
    }

    /**
     * 与ViewPager结合的时候使用
     * @param direction
     * @return
     */
    override fun canScrollVertically(direction: Int): Boolean {
        if (pinchMode == PINCH_MODE_SCALE) {
            return true
        }
        val bound = getImageBound(null)
        if (bound.isEmpty) {
            return false
        }
        return if (direction > 0) {
            bound.bottom > height
        } else {
            bound.top < 0
        }
    }
    ////////////////////////////////公共状态设置////////////////////////////////
    /**
     * 执行当前outerMatrix到指定outerMatrix渐变的动画
     *
     * 调用此方法会停止正在进行中的手势以及手势动画.
     * 当duration为0时,outerMatrix值会被立即设置而不会启动动画.
     *
      @param endMatrix 动画目标矩阵
      @param duration 动画持续时间
     *
      @see .getOuterMatrix

    fun outerMatrixTo(endMatrix: Matrix?, duration: Long) {
        if (endMatrix == null) {
            return
        }
        //将手势设置为PINCH_MODE_FREE将停止后续手势的触发
        pinchMode = PINCH_MODE_FREE
        //停止所有正在进行的动画
        cancelAllAnimator()
        //如果时间不合法立即执行结果
        if (duration <= 0) {
            mOuterMatrix.set(endMatrix)
            dispatchOuterMatrixChanged()
            invalidate()
        } else {
            //创建矩阵变化动画
            mScaleAnimator = ScaleAnimator(mOuterMatrix, endMatrix, duration)
            mScaleAnimator!!.start()
        }
    }*/

    /**
     * 执行当前mask到指定mask的变化动画
     *
     * 调用此方法不会停止手势以及手势相关动画,但会停止正在进行的mask动画.
     * 当前mask为null时,则不执行动画立即设置为目标mask.
     * 当duration为0时,立即将当前mask设置为目标mask,不会执行动画.
     *
      @param mask 动画目标mask
      @param duration 动画持续时间
     *
      @see .getMask

    fun zoomMaskTo(mask: RectF?, duration: Long) {
        if (mask == null) {
            return
        }
        //停止mask动画
        if (mMaskAnimator != null) {
            mMaskAnimator!!.cancel()
            mMaskAnimator = null
        }
        //如果duration为0或者之前没有设置过mask,不执行动画,立即设置
        if (duration <= 0 || mMask == null) {
            if (mMask == null) {
                mMask = RectF()
            }
            mMask!!.set(mask)
            invalidate()
        } else {
            //执行mask动画
            mMaskAnimator = MaskAnimator(mMask!!, mask, duration)
            mMaskAnimator!!.start()
        }
    }*/

    /**
     * 重置所有状态
     *
     * 重置位置到fit center状态,清空mask,停止所有手势,停止所有动画.
     * 但不清空drawable,以及事件绑定相关数据.

    fun reset() {
        //重置位置到fit
        mOuterMatrix.reset()
        dispatchOuterMatrixChanged()
        //清空mask
        mMask = null
        //停止所有手势
        pinchMode = PINCH_MODE_FREE
        mLastMovePoint[0f] = 0f
        mScaleCenter[0f] = 0f
        mScaleBase = 0f
        //停止所有动画
        if (mMaskAnimator != null) {
            mMaskAnimator!!.cancel()
            mMaskAnimator = null
        }
        cancelAllAnimator()
        //重绘
        invalidate()
    }*/
    ////////////////////////////////对外广播事件////////////////////////////////
    /**
     * 外部矩阵变化事件通知监听器
     */
    interface OuterMatrixChangedListener {
        /**
         * 外部矩阵变化回调
         *
         * 外部矩阵的任何变化后都收到此回调.
         * 外部矩阵变化后,总变化矩阵,图片的展示位置都将发生变化.
         *
         * @param pinchImageView
         *
         * @see .getOuterMatrix
         * @see .getCurrentImageMatrix
         * @see .getImageBound
         */
        fun onOuterMatrixChanged(pinchImageView: ScaleImageView?)
    }

    /**
     * 所有OuterMatrixChangedListener监听列表
     *
     * @see .addOuterMatrixChangedListener
     * @see .removeOuterMatrixChangedListener
     */
    private var mOuterMatrixChangedListeners: MutableList<OuterMatrixChangedListener>? =
        null

    /**
     * 当mOuterMatrixChangedListeners被锁定不允许修改时,临时将修改写到这个副本中
     *
     * @see .mOuterMatrixChangedListeners
     */
    private var mOuterMatrixChangedListenersCopy: MutableList<OuterMatrixChangedListener>? =
        null

    /**
     * mOuterMatrixChangedListeners的修改锁定
     *
     * 当进入dispatchOuterMatrixChanged方法时,被加1,退出前被减1
     *
     * @see .dispatchOuterMatrixChanged
     * @see .addOuterMatrixChangedListener
     * @see .removeOuterMatrixChangedListener
     */
    private var mDispatchOuterMatrixChangedLock = 0

    /**
     * 添加外部矩阵变化监听
     *
      @param listener

    fun addOuterMatrixChangedListener(listener: OuterMatrixChangedListener?) {
        if (listener == null) {
            return
        }
        //如果监听列表没有被修改锁定直接将监听添加到监听列表
        if (mDispatchOuterMatrixChangedLock == 0) {
            if (mOuterMatrixChangedListeners == null) {
                mOuterMatrixChangedListeners =
                    ArrayList()
            }
            mOuterMatrixChangedListeners!!.add(listener)
        } else {
            //如果监听列表修改被锁定,那么尝试在监听列表副本上添加
            //监听列表副本将会在锁定被解除时替换到监听列表里
            if (mOuterMatrixChangedListenersCopy == null) {
                mOuterMatrixChangedListenersCopy = if (mOuterMatrixChangedListeners != null) {
                    ArrayList(
                        mOuterMatrixChangedListeners!!
                    )
                } else {
                    ArrayList()
                }
            }
            mOuterMatrixChangedListenersCopy!!.add(listener)
        }
    }*/

    /**
     * 删除外部矩阵变化监听
     *
      @param listener

    fun removeOuterMatrixChangedListener(listener: OuterMatrixChangedListener?) {
        if (listener == null) {
            return
        }
        //如果监听列表没有被修改锁定直接在监听列表数据结构上修改
        if (mDispatchOuterMatrixChangedLock == 0) {
            if (mOuterMatrixChangedListeners != null) {
                mOuterMatrixChangedListeners!!.remove(listener)
            }
        } else {
            //如果监听列表被修改锁定,那么就在其副本上修改
            //其副本将会在锁定解除时替换回监听列表
            if (mOuterMatrixChangedListenersCopy == null) {
                if (mOuterMatrixChangedListeners != null) {
                    mOuterMatrixChangedListenersCopy =
                        ArrayList(
                            mOuterMatrixChangedListeners!!
                        )
                }
            }
            if (mOuterMatrixChangedListenersCopy != null) {
                mOuterMatrixChangedListenersCopy!!.remove(listener)
            }
        }
    }*/

    /**
     * 触发外部矩阵修改事件
     *
     * 需要在每次给外部矩阵设置值时都调用此方法.
     *
     * @see .mOuterMatrix
     */
    private fun dispatchOuterMatrixChanged() {
        if (mOuterMatrixChangedListeners == null) {
            return
        }
        //增加锁
        //这里之所以用计数器做锁定是因为可能在锁定期间又间接调用了此方法产生递归
        //使用boolean无法判断递归结束
        mDispatchOuterMatrixChangedLock++
        //在列表循环过程中不允许修改列表,否则将引发崩溃
        for (listener in mOuterMatrixChangedListeners!!) {
            listener.onOuterMatrixChanged(this)
        }
        //减锁
        mDispatchOuterMatrixChangedLock--
        //如果是递归的情况,mDispatchOuterMatrixChangedLock可能大于1,只有减到0才能算列表的锁定解除
        if (mDispatchOuterMatrixChangedLock == 0) {
            //如果期间有修改列表,那么副本将不为null
            if (mOuterMatrixChangedListenersCopy != null) {
                //将副本替换掉正式的列表
                mOuterMatrixChangedListeners = mOuterMatrixChangedListenersCopy
                //清空副本
                mOuterMatrixChangedListenersCopy = null
            }
        }
    }
    ////////////////////////////////用于重载定制////////////////////////////////

    /**
     * 计算双击之后图片接下来应该被缩放的比例
     *
     * 如果值大于getMaxScale或者小于fit center尺寸，则实际使用取边界值.
     * 通过覆盖此方法可以定制不同的图片被双击时使用不同的放大策略.
     *
     * @param innerScale 当前内部矩阵的缩放值
     * @param outerScale 当前外部矩阵的缩放值
     * @return 接下来的缩放比例
     *
     * @see .doubleTap
     * @see .getMaxScale
     */
    private fun calculateNextScale(
        innerScale: Float,
        outerScale: Float
    ): Float {
        val currentScale = innerScale * outerScale
        Log.d("MySIV", "current scale: $currentScale, max scale: $maxScale")
        return if (currentScale < maxScale * 0.9f) maxScale
        else innerScale
    }

    ////////////////////////////////初始化////////////////////////////////
    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        initView()
    }

    private fun initView() {
        //强制设置图片scaleType为matrix
        super.setScaleType(ScaleType.MATRIX)
    }

    //不允许设置scaleType，只能用内部设置的matrix
    override fun setScaleType(scaleType: ScaleType) {}

    ////////////////////////////////绘制////////////////////////////////
    override fun onDraw(canvas: Canvas) {
        try {
            //在绘制前设置变换矩阵
            if (isReady) {
                val matrix = matrixTake()
                imageMatrix = getCurrentImageMatrix(matrix)
                matrixGiven(matrix)
            }
            //对图像做遮罩处理
            if (mMask != null) {
                canvas.save()
                canvas.clipRect(mMask!!)
                super.onDraw(canvas)
                canvas.restore()
            } else {
                super.onDraw(canvas)
            }
        }catch (e:Exception){
            e.printStackTrace()
            ViewMangaActivity.va?.get()?.toolsBox?.toastError("图片加载错误，请尝试下载后使用较低图片质量查看", false)
        }
    }
    ////////////////////////////////有效性判断////////////////////////////////
    /**
     * 判断当前情况是否能执行手势相关计算
     *
     * 包括:是否有图片,图片是否有尺寸,控件是否有尺寸.
     *
     * @return 是否能执行手势相关计算
     */
    private val isReady: Boolean
        get() = drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0 && width > 0 && height > 0
    ////////////////////////////////手势动画处理////////////////////////////////
    /**
     * 在单指模式下:
     * 记录上一次手指的位置,用于计算新的位置和上一次位置的差值.
     *
     * 双指模式下:
     * 记录两个手指的中点,作为和mScaleCenter绑定的点.
     * 这个绑定可以保证mScaleCenter无论如何都会跟随这个中点.
     *
      @see .mScaleCenter
     *
      @see .scale
      @see .scaleEnd
     */
    private val mLastMovePoint = PointF()

    /**
     * 缩放模式下图片的缩放中点.
     *
     * 为其指代的点经过innerMatrix变换之后的值.
     * 其指代的点在手势过程中始终跟随mLastMovePoint.
     * 通过双指缩放时,其为缩放中心点.
     *
     * @see .saveScaleContext
     * @see .mLastMovePoint
     *
     * @see .scale
     */
    private val mScaleCenter = PointF()

    /**
     * 缩放模式下的基础缩放比例
     *
     * 为外层缩放值除以开始缩放时两指距离.
     * 其值乘上最新的两指之间距离为最新的图片缩放比例.
     *
     * @see .saveScaleContext
     * @see .scale
     */
    private var mScaleBase = 0f

    /**
     * 图片缩放动画
     *
     * 缩放模式把图片的位置大小超出限制之后触发.
     * 双击图片放大或缩小时触发.
     * 手动调用outerMatrixTo触发.
     *
     * @see .scaleEnd
     * @see .doubleTap
     * @see .outerMatrixTo
     */
    private var mScaleAnimator: ScaleAnimator? = null

    /**
     * 滑动产生的惯性动画
     *
     * @see .fling
     */
    private var mFlingAnimator: FlingAnimator? = null

    /**
     * 常用手势处理
     *
     * 在onTouchEvent末尾被执行.
     */
    @ExperimentalStdlibApi
    private val mGestureDetector =
        GestureDetector(this.context, object : SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                //只有在单指模式结束之后才允许执行fling
                if (pinchMode == PINCH_MODE_FREE && !(mScaleAnimator != null && mScaleAnimator!!.isRunning)) {
                    //parent.requestDisallowInterceptTouchEvent(true) //触摸事件请求拦截
                    fling(velocityX, velocityY)
                    //parent.requestDisallowInterceptTouchEvent(false) //触摸事件请求取消拦截
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                //触发长按
                if (mOnLongClickListener != null) {
                    mOnLongClickListener!!.onLongClick(this@ScaleImageView)
                }
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                //当手指快速第二次按下触发,此时必须是单指模式才允许执行doubleTap
                if (pinchMode == PINCH_MODE_SCROLL && !(mScaleAnimator != null && mScaleAnimator!!.isRunning)) {
                    doubleTap(e.x, e.y)
                }
                return true
            }

            var v :WeakReference<ViewMangaActivity>? = null
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                if(v == null) {
                    v = ViewMangaActivity.va
                    v?.let { pm = PagesManager(it) }
                }
                //触发点击
                if (mOnClickListener != null) {
                    mOnClickListener!!.onClick(this@ScaleImageView)
                }
                (event.x / width).let {
                    when {
                        it <= 1.0 / 3.0 -> pm?.toPreviousPage()
                        it <= 2.0 / 3.0 -> pm?.manageInfo()
                        else -> pm?.toNextPage()
                    }
                }
                return true
            }
        })
    private val isBig: Boolean
        get() = getMatrixScale(mOuterMatrix)[0] > 1f

    @OptIn(ExperimentalStdlibApi::class)
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        val action = event.action and MotionEvent.ACTION_MASK
        Log.d("MySi", "Outer Scale: ${getMatrixScale(mOuterMatrix)[0]}")
        //最后一个点抬起或者取消，结束所有模式
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            //如果之前是缩放模式,还需要触发一下缩放结束动画
            if (pinchMode == PINCH_MODE_SCALE) {
                scaleEnd()
            }
            pinchMode = PINCH_MODE_FREE
            parent.requestDisallowInterceptTouchEvent(false) //触摸事件请求取消拦截
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            //多个手指情况下抬起一个手指,此时需要是缩放模式才触发
            if (pinchMode == PINCH_MODE_SCALE) {
                //抬起的点如果大于2，那么缩放模式还有效，但是有可能初始点变了，重新测量初始点
                if (event.pointerCount > 2) {
                    //如果还没结束缩放模式，但是第一个点抬起了，那么让第二个点和第三个点作为缩放控制点
                    if (event.action shr 8 == 0) {
                        saveScaleContext(event.getX(1), event.getY(1), event.getX(2), event.getY(2))
                        //如果还没结束缩放模式，但是第二个点抬起了，那么让第一个点和第三个点作为缩放控制点
                    } else if (event.action shr 8 == 1) {
                        saveScaleContext(event.getX(0), event.getY(0), event.getX(2), event.getY(2))
                    }
                }
                //如果抬起的点等于2,那么此时只剩下一个点,也不允许进入单指模式,因为此时可能图片没有在正确的位置上
            }
            //第一个点按下，开启滚动模式，记录开始滚动的点
        } else if (action == MotionEvent.ACTION_DOWN) {
            //在矩阵动画过程中不允许启动滚动模式
            if (!(mScaleAnimator != null && mScaleAnimator!!.isRunning)) {
                //停止所有动画
                cancelAllAnimator()
                //切换到滚动模式
                pinchMode = PINCH_MODE_SCROLL
                //保存触发点用于move计算差值
                mLastMovePoint[event.x] = event.y
            }
            //非第一个点按下，关闭滚动模式，开启缩放模式，记录缩放模式的一些初始数据
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            //停止所有动画
            cancelAllAnimator()
            //切换到缩放模式
            pinchMode = PINCH_MODE_SCALE
            //保存缩放的两个手指
            saveScaleContext(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!(mScaleAnimator != null && mScaleAnimator!!.isRunning)) {
                //在滚动模式下移动
                if (pinchMode == PINCH_MODE_SCROLL) {
                    //每次移动产生一个差值累积到图片位置上
                    scrollBy(event.x - mLastMovePoint.x, event.y - mLastMovePoint.y)
                    //记录新的移动点
                    mLastMovePoint[event.x] = event.y
                    if (isBig)
                        parent.requestDisallowInterceptTouchEvent(true) //触摸事件请求拦截
                    //在缩放模式下移动
                } else if (pinchMode == PINCH_MODE_SCALE && event.pointerCount > 1) {
                    //两个缩放点间的距离
                    val distance = getDistance(
                        event.getX(0),
                        event.getY(0),
                        event.getX(1),
                        event.getY(1)
                    )
                    //保存缩放点中点
                    val lineCenter = getCenterPoint(
                        event.getX(0),
                        event.getY(0),
                        event.getX(1),
                        event.getY(1)
                    )
                    mLastMovePoint[lineCenter[0]] = lineCenter[1]
                    //处理缩放
                    scale(mScaleCenter, mScaleBase, distance, mLastMovePoint)
                }
            }
        }
        //无论如何都处理各种外部手势
        mGestureDetector.onTouchEvent(event)
        return true
    }

    /**
     * 让图片移动一段距离
     *
     * 不能移动超过可移动范围,超过了就到可移动范围边界为止.
     *
     * @param xDiff 移动距离
     * @param yDiff 移动距离
     * @return 是否改变了位置
     */
    private fun scrollBy(xDiff: Float, yDiff: Float): Boolean {
        var xDiff = xDiff
        var yDiff = yDiff
        if (!isReady) {
            return false
        }
        //原图方框
        val bound = rectFTake()
        getImageBound(bound)
        //控件大小
        val displayWidth = width.toFloat()
        val displayHeight = height.toFloat()
        //如果当前图片宽度小于控件宽度，则不能移动
        when {
            bound.right - bound.left < displayWidth -> {
                xDiff = 0f
                //如果图片左边在移动后超出控件左边
            }
            bound.left + xDiff > 0 -> {
                //如果在移动之前是没超出的，计算应该移动的距离
                xDiff = if (bound.left < 0) {
                    -bound.left
                    //否则无法移动
                } else {
                    0f
                }
                //如果图片右边在移动后超出控件右边
            }
            bound.right + xDiff < displayWidth -> {
                //如果在移动之前是没超出的，计算应该移动的距离
                xDiff = if (bound.right > displayWidth) {
                    displayWidth - bound.right
                    //否则无法移动
                } else {
                    0f
                }
            }
            //以下同理
            //应用移动变换
            //触发重绘
            //检查是否有变化
        }
        //以下同理
        when {
            bound.bottom - bound.top < displayHeight -> {
                yDiff = 0f
            }
            bound.top + yDiff > 0 -> {
                yDiff = if (bound.top < 0) {
                    -bound.top
                } else {
                    0f
                }
            }
            bound.bottom + yDiff < displayHeight -> {
                yDiff = if (bound.bottom > displayHeight) {
                    displayHeight - bound.bottom
                } else {
                    0f
                }
            }
            //应用移动变换
            //触发重绘
            //检查是否有变化
        }
        rectFGiven(bound)
        //应用移动变换
        mOuterMatrix.postTranslate(xDiff, yDiff)
        dispatchOuterMatrixChanged()
        //触发重绘
        invalidate()
        //检查是否有变化
        return xDiff != 0f || yDiff != 0f
    }

    /**
     * 记录缩放前的一些信息
     *
     * 保存基础缩放值.
     * 保存图片缩放中点.
     *
     * @param x1 缩放第一个手指
     * @param y1 缩放第一个手指
     * @param x2 缩放第二个手指
     * @param y2 缩放第二个手指
     */
    private fun saveScaleContext(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ) {
        //记录基础缩放值,其中图片缩放比例按照x方向来计算
        //理论上图片应该是等比的,x和y方向比例相同
        //但是有可能外部设定了不规范的值.
        //但是后续的scale操作会将xy不等的缩放值纠正,改成和x方向相同
        mScaleBase =
            getMatrixScale(mOuterMatrix)[0] / getDistance(x1, y1, x2, y2)
        //两手指的中点在屏幕上落在了图片的某个点上,图片上的这个点在经过总矩阵变换后和手指中点相同
        //现在我们需要得到图片上这个点在图片是fit center状态下在屏幕上的位置
        //因为后续的计算都是基于图片是fit center状态下进行变换
        //所以需要把两手指中点除以外层变换矩阵得到mScaleCenter
        val center = inverseMatrixPoint(
            getCenterPoint(
                x1,
                y1,
                x2,
                y2
            ), mOuterMatrix
        )
        mScaleCenter[center[0]] = center[1]
    }

    /**
     * 对图片按照一些手势信息进行缩放
     *
     * @param scaleCenter mScaleCenter
     * @param scaleBase mScaleBase
     * @param distance 手指两点之间距离
     * @param lineCenter 手指两点之间中点
     *
     * @see .mScaleCenter
     *
     * @see .mScaleBase
     */
    private fun scale(
        scaleCenter: PointF,
        scaleBase: Float,
        distance: Float,
        lineCenter: PointF
    ) {
        if (!isReady) {
            return
        }
        //计算图片从fit center状态到目标状态的缩放比例
        val scale = scaleBase * distance
        val matrix = matrixTake()
        //按照图片缩放中心缩放，并且让缩放中心在缩放点中点上
        matrix.postScale(scale, scale, scaleCenter.x, scaleCenter.y)
        //让图片的缩放中点跟随手指缩放中点
        matrix.postTranslate(lineCenter.x - scaleCenter.x, lineCenter.y - scaleCenter.y)
        //应用变换
        mOuterMatrix.set(matrix)
        matrixGiven(matrix)
        dispatchOuterMatrixChanged()
        //重绘
        invalidate()
    }

    /**
     * 双击后放大或者缩小
     *
     * 将图片缩放比例缩放到nextScale指定的值.
     * 但nextScale值不能大于最大缩放值不能小于fit center情况下的缩放值.
     * 将双击的点尽量移动到控件中心.
     *
     * @param x 双击的点
     * @param y 双击的点
     *
     * @see .calculateNextScale
     * @see .getMaxScale
     */
    private fun doubleTap(x: Float, y: Float) {
        if (!isReady) {
            return
        }
        //获取第一层变换矩阵
        val innerMatrix = matrixTake()
        getInnerMatrix(innerMatrix)
        //当前总的缩放比例
        val innerScale = getMatrixScale(innerMatrix)[0]
        val outerScale = getMatrixScale(mOuterMatrix)[0]
        val currentScale = innerScale * outerScale
        //控件大小
        val displayWidth = width.toFloat()
        val displayHeight = height.toFloat()
        //最大放大大小
        val maxScale = maxScale
        //接下来要放大的大小
        var nextScale = calculateNextScale(innerScale, outerScale)
        //如果接下来放大大于最大值或者小于fit center值，则取边界
        Log.d("MySIV", "Next scale: $nextScale, Max scale: $maxScale, Inner scale: $innerScale")
        if (nextScale > maxScale) {
            nextScale = maxScale
        }
        if (nextScale < innerScale) {
            nextScale = innerScale
        }
        //开始计算缩放动画的结果矩阵
        val animEnd = matrixTake(mOuterMatrix)
        //计算还需缩放的倍数
        animEnd.postScale(nextScale / currentScale, nextScale / currentScale, x, y)
        //将放大点移动到控件中心
        animEnd.postTranslate(displayWidth / 2f - x, displayHeight / 2f - y)
        //得到放大之后的图片方框
        val testMatrix = matrixTake(innerMatrix)
        testMatrix.postConcat(animEnd)
        val testBound = rectFTake(
            0f,
            0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )
        testMatrix.mapRect(testBound)
        //修正位置
        var postX = 0f
        var postY = 0f
        when {
            testBound.right - testBound.left < displayWidth -> {
                postX = displayWidth / 2f - (testBound.right + testBound.left) / 2f
            }
            testBound.left > 0 -> {
                postX = -testBound.left
            }
            testBound.right < displayWidth -> {
                postX = displayWidth - testBound.right
            }
            //应用修正位置
            //清理当前可能正在执行的动画
            //启动矩阵动画
            //清理临时变量
        }
        when {
            testBound.bottom - testBound.top < displayHeight -> {
                postY = displayHeight / 2f - (testBound.bottom + testBound.top) / 2f
            }
            testBound.top > 0 -> {
                postY = -testBound.top
            }
            testBound.bottom < displayHeight -> {
                postY = displayHeight - testBound.bottom
            }
            //应用修正位置
            //清理当前可能正在执行的动画
            //启动矩阵动画
            //清理临时变量
        }
        //应用修正位置
        animEnd.postTranslate(postX, postY)
        //清理当前可能正在执行的动画
        cancelAllAnimator()
        //启动矩阵动画
        mScaleAnimator = ScaleAnimator(mOuterMatrix, animEnd)
        mScaleAnimator!!.start()
        //清理临时变量
        rectFGiven(testBound)
        matrixGiven(testMatrix)
        matrixGiven(animEnd)
        matrixGiven(innerMatrix)
    }

    /**
     * 当缩放操作结束动画
     *
     * 如果图片超过边界,找到最近的位置动画恢复.
     * 如果图片缩放尺寸超过最大值或者最小值,找到最近的值动画恢复.
     */
    private fun scaleEnd() {
        if (!isReady) {
            return
        }
        //是否修正了位置
        var change = false
        //获取图片整体的变换矩阵
        val currentMatrix = matrixTake()
        getCurrentImageMatrix(currentMatrix)
        //整体缩放比例
        val currentScale =
            getMatrixScale(currentMatrix)[0]
        //第二层缩放比例
        val outerScale = getMatrixScale(mOuterMatrix)[0]
        //控件大小
        val displayWidth = width.toFloat()
        val displayHeight = height.toFloat()
        //最大缩放比例
        val maxScale = maxScale
        //比例修正
        var scalePost = 1f
        //位置修正
        var postX = 0f
        var postY = 0f
        //如果整体缩放比例大于最大比例，进行缩放修正
        if (currentScale > maxScale) {
            scalePost = maxScale / currentScale
        }
        //如果缩放修正后整体导致第二层缩放小于1（就是图片比fit center状态还小），重新修正缩放
        if (outerScale * scalePost < 1f) {
            scalePost = 1f / outerScale
        }
        //如果缩放修正不为1，说明进行了修正
        if (scalePost != 1f) {
            change = true
        }
        //尝试根据缩放点进行缩放修正
        val testMatrix = matrixTake(currentMatrix)
        testMatrix.postScale(scalePost, scalePost, mLastMovePoint.x, mLastMovePoint.y)
        val testBound = rectFTake(
            0f,
            0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )
        //获取缩放修正后的图片方框
        testMatrix.mapRect(testBound)
        //检测缩放修正后位置有无超出，如果超出进行位置修正//计算结束矩阵
        //清理当前可能正在执行的动画
        //启动矩阵动画
        //清理临时变量
        //清理临时变量
        //如果位置修正不为0，说明进行了修正
        //只有有执行修正才执行动画
        //如果位置修正不为0，说明进行了修正
        //只有有执行修正才执行动画
        when {
            testBound.right - testBound.left < displayWidth -> {
                postX = displayWidth / 2f - (testBound.right + testBound.left) / 2f
            }
            testBound.left > 0 -> {
                postX = -testBound.left
            }
            testBound.right < displayWidth -> {
                postX = displayWidth - testBound.right
            }
            //计算结束矩阵
            //清理当前可能正在执行的动画
            //启动矩阵动画
            //清理临时变量
            //清理临时变量
        }
        //计算结束矩阵
        //清理当前可能正在执行的动画
        //启动矩阵动画
        //清理临时变量
        //清理临时变量
        when {
            testBound.bottom - testBound.top < displayHeight -> {
                postY = displayHeight / 2f - (testBound.bottom + testBound.top) / 2f
            }
            testBound.top > 0 -> {
                postY = -testBound.top
            }
            testBound.bottom < displayHeight -> {
                postY = displayHeight - testBound.bottom
            }
            //如果位置修正不为0，说明进行了修正
            //只有有执行修正才执行动画
        }
        //如果位置修正不为0，说明进行了修正
        if (postX != 0f || postY != 0f) {
            change = true
        }
        //只有有执行修正才执行动画
        if (change) {
            //计算结束矩阵
            val animEnd = matrixTake(mOuterMatrix)
            animEnd.postScale(scalePost, scalePost, mLastMovePoint.x, mLastMovePoint.y)
            animEnd.postTranslate(postX, postY)
            //清理当前可能正在执行的动画
            cancelAllAnimator()
            //启动矩阵动画
            mScaleAnimator = ScaleAnimator(mOuterMatrix, animEnd)
            mScaleAnimator!!.start()
            //清理临时变量
            matrixGiven(animEnd)
        }
        //清理临时变量
        rectFGiven(testBound)
        matrixGiven(testMatrix)
        matrixGiven(currentMatrix)
    }

    /**
     * 执行惯性动画
     *
     * 动画在遇到不能移动就停止.
     * 动画速度衰减到很小就停止.
     *
     * 其中参数速度单位为 像素/秒
     *
     * @param vx x方向速度
     * @param vy y方向速度
     */
    private fun fling(vx: Float, vy: Float) {
        if (!isReady) {
            return
        }
        //清理当前可能正在执行的动画
        cancelAllAnimator()
        //创建惯性动画
        //FlingAnimator单位为 像素/帧,一秒60帧
        mFlingAnimator = FlingAnimator(vx / 60f, vy / 60f)
        mFlingAnimator!!.start()
    }

    /**
     * 停止所有手势动画
     */
    private fun cancelAllAnimator() {
        if (mScaleAnimator != null) {
            mScaleAnimator!!.cancel()
            mScaleAnimator = null
        }
        if (mFlingAnimator != null) {
            mFlingAnimator!!.cancel()
            mFlingAnimator = null
        }
    }

    /**
     * 惯性动画
     *
     * 速度逐渐衰减,每帧速度衰减为原来的FLING_DAMPING_FACTOR,当速度衰减到小于1时停止.
     * 当图片不能移动时,动画停止.
     */
    private inner class FlingAnimator(vectorX: Float, vectorY: Float) :
        ValueAnimator(), AnimatorUpdateListener {
        /**
         * 速度向量
         */
        private val mVector: FloatArray
        override fun onAnimationUpdate(animation: ValueAnimator) {
            //移动图像并给出结果
            val result = scrollBy(mVector[0], mVector[1])
            //衰减速度
            mVector[0] *= FLING_DAMPING_FACTOR
            mVector[1] *= FLING_DAMPING_FACTOR
            //速度太小或者不能移动了就结束
            if (!result || getDistance(
                    0f,
                    0f,
                    mVector[0],
                    mVector[1]
                ) < 1f
            ) {
                animation.cancel()
            }
        }

        /**
         * 创建惯性动画
         *
         * 参数单位为 像素/帧
         *
          @param vectorX 速度向量
          @param vectorY 速度向量
         */
        init {
            setFloatValues(0f, 1f)
            duration = 1000000
            addUpdateListener(this)
            mVector = floatArrayOf(vectorX, vectorY)
        }
    }

    /**
     * 缩放动画
     *
     * 在给定时间内从一个矩阵的变化逐渐动画到另一个矩阵的变化
     */
    private inner class ScaleAnimator @JvmOverloads constructor(
        start: Matrix,
        end: Matrix,
        duration: Long = SCALE_ANIMATOR_DURATION.toLong()
    ) :
        ValueAnimator(), AnimatorUpdateListener {
        /**
         * 开始矩阵
         */
        private val mStart = FloatArray(9)

        /**
         * 结束矩阵
         */
        private val mEnd = FloatArray(9)

        /**
         * 中间结果矩阵
         */
        private val mResult = FloatArray(9)
        override fun onAnimationUpdate(animation: ValueAnimator) {
            //获取动画进度
            val value = animation.animatedValue as Float
            //根据动画进度计算矩阵中间插值
            for (i in 0..8) {
                mResult[i] = mStart[i] + (mEnd[i] - mStart[i]) * value
            }
            //设置矩阵并重绘
            mOuterMatrix.setValues(mResult)
            dispatchOuterMatrixChanged()
            invalidate()
        }
        /**
         * 构建一个缩放动画
         *
         * 从一个矩阵变换到另外一个矩阵
         *
         * @param start 开始矩阵
         * @param end 结束矩阵
         * @param duration 动画时间
         */
        /**
         * 构建一个缩放动画
         *
         * 从一个矩阵变换到另外一个矩阵
         *
          @param start 开始矩阵
          @param end 结束矩阵
         */
        init {
            setFloatValues(0f, 1f)
            setDuration(duration)
            addUpdateListener(this)
            start.getValues(mStart)
            end.getValues(mEnd)
        }
    }
    ////////////////////////////////防止内存抖动复用对象////////////////////////////////
    /**
     * 对象池
     *
     * 防止频繁new对象产生内存抖动.
     * 由于对象池最大长度限制,如果吞度量超过对象池容量,仍然会发生抖动.
     * 此时需要增大对象池容量,但是会占用更多内存.
     *
     * @param <T> 对象池容纳的对象类型
    </T> */
    private abstract class ObjectsPool<T>(
        /**
         * 对象池的最大容量
         */
        private val mSize: Int
    ) {

        /**
         * 对象池队列
         */
        private val mQueue: Queue<T>

        /**
         * 获取一个空闲的对象
         *
         * 如果对象池为空,则对象池自己会new一个返回.
         * 如果对象池内有对象,则取一个已存在的返回.
         * take出来的对象用完要记得调用given归还.
         * 如果不归还,让然会发生内存抖动,但不会引起泄漏.
         *
         * @return 可用的对象
         *
         * @see .given
         */
        fun take(): T {
            //如果池内为空就创建一个
            return if (mQueue.size == 0) {
                newInstance()
            } else {
                //对象池里有就从顶端拿出来一个返回
                resetInstance(mQueue.poll()?: newInstance())
            }
        }

        /**
         * 归还对象池内申请的对象
         *
         * 如果归还的对象数量超过对象池容量,那么归还的对象就会被丢弃.
         *
         * @param obj 归还的对象
         *
         * @see .take
         */
        fun given(obj: T?) {
            //如果对象池还有空位子就归还对象
            if (obj != null && mQueue.size < mSize) {
                mQueue.offer(obj)
            }
        }

        /**
         * 实例化对象
         *
         * @return 创建的对象
         */
        protected abstract fun newInstance(): T

        /**
         * 重置对象
         *
         * 把对象数据清空到就像刚创建的一样.
         *
         * @param obj 需要被重置的对象
         * @return 被重置之后的对象
         */
        protected abstract fun resetInstance(obj: T): T

        /**
         * 创建一个对象池
         *
          @param size 对象池最大容量
         */
        init {
            mQueue = LinkedList()
        }
    }

    /**
     * 矩阵对象池
     */
    private class MatrixPool(size: Int) :
        ObjectsPool<Matrix?>(size) {
        override fun newInstance(): Matrix {
            return Matrix()
        }

        override fun resetInstance(obj: Matrix?): Matrix? {
            obj?.reset()
            return obj
        }
    }

    /**
     * 矩形对象池
     */
    private class RectFPool(size: Int) : ObjectsPool<RectF?>(size) {
        override fun newInstance(): RectF {
            return RectF()
        }

        override fun resetInstance(obj: RectF?): RectF? {
            obj?.setEmpty()
            return obj
        }
    }
    ////////////////////////////////数学计算工具类////////////////////////////////
    /**
     * 数学计算工具类
     */
    companion object {
        /**
         * 矩阵对象池
         */
        private val mMatrixPool = MatrixPool(16)

        /**
         * 获取矩阵对象
         */
        fun matrixTake() = mMatrixPool.take()!!

        /**
         * 获取某个矩阵的copy
         */
        fun matrixTake(matrix: Matrix?): Matrix {
            val result = mMatrixPool.take()!!
            if (matrix != null) {
                result.set(matrix)
            }
            return result
        }

        /**
         * 归还矩阵对象
         */
        fun matrixGiven(matrix: Matrix) {
            mMatrixPool.given(matrix)
        }

        /**
         * 矩形对象池
         */
        private val mRectFPool = RectFPool(16)

        /**
         * 获取矩形对象
         */
        fun rectFTake(): RectF {
            return mRectFPool.take()!!
        }

        /**
         * 按照指定值获取矩形对象
         */
        fun rectFTake(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): RectF {
            val result = mRectFPool.take()!!
            result[left, top, right] = bottom
            return result
        }

        /**
         * 获取某个矩形的副本

        fun rectFTake(rectF: RectF?): RectF {
            val result = mRectFPool.take()!!
            if (rectF != null) {
                result.set(rectF)
            }
            return result
        }*/

        /**
         * 归还矩形对象
         */
        fun rectFGiven(rectF: RectF) {
            mRectFPool.given(rectF)
        }

        /**
         * 获取两点之间距离
         *
         * @param x1 点1
         * @param y1 点1
         * @param x2 点2
         * @param y2 点2
         * @return 距离
         */
        fun getDistance(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ): Float {
            val x = x1 - x2
            val y = y1 - y2
            return sqrt(x * x + y * y.toDouble()).toFloat()
        }

        /**
         * 获取两点的中点
         *
         * @param x1 点1
         * @param y1 点1
         * @param x2 点2
         * @param y2 点2
         * @return float[]{x, y}
         */
        fun getCenterPoint(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float
        ): FloatArray {
            return floatArrayOf((x1 + x2) / 2f, (y1 + y2) / 2f)
        }

        /**
         * 获取矩阵的缩放值
         *
         * @param matrix 要计算的矩阵
         * @return float[]{scaleX, scaleY}
         */
        fun getMatrixScale(matrix: Matrix?): FloatArray {
            return if (matrix != null) {
                val value = FloatArray(9)
                matrix.getValues(value)
                floatArrayOf(value[0], value[4])
            } else {
                FloatArray(2)
            }
        }

        /**
         * 计算点除以矩阵的值
         *
         * matrix.mapPoints(unknownPoint) -> point
         * 已知point和matrix,求unknownPoint的值.
         *
         * @param point
         * @param matrix
         * @return unknownPoint
         */
        fun inverseMatrixPoint(
            point: FloatArray?,
            matrix: Matrix?
        ): FloatArray {
            return if (point != null && matrix != null) {
                val dst = FloatArray(2)
                //计算matrix的逆矩阵
                val inverse = matrixTake()
                matrix.invert(inverse)
                //用逆矩阵变换point到dst,dst就是结果
                inverse.mapPoints(dst, point)
                //清除临时变量
                matrixGiven(inverse)
                dst
            } else {
                FloatArray(2)
            }
        }
        ////////////////////////////////配置参数////////////////////////////////
        /**
         * 图片缩放动画时间
         */
        const val SCALE_ANIMATOR_DURATION = 200

        /**
         * 惯性动画衰减参数
         */
        const val FLING_DAMPING_FACTOR = 0.9f

        /**
         * 获取图片最大可放大的比例
         *
         * 如果放大大于这个比例则不被允许.
         * 在双手缩放过程中如果图片放大比例大于这个值,手指释放将回弹到这个比例.
         * 在双击放大过程中不允许放大比例大于这个值.
         * 覆盖此方法可以定制不同情况使用不同的最大可放大比例.
         *
         * @return 缩放比例
         *
         * @see .scaleEnd
         * @see .doubleTap
         */
        /**
         * 图片最大放大比例
         */
        const val maxScale:Float = 2.5f
        ////////////////////////////////公共状态获取////////////////////////////////
        /**
         * 手势状态：自由状态
         *
         * @see .getPinchMode
         */
        const val PINCH_MODE_FREE = 0

        /**
         * 手势状态：单指滚动状态
         *
         * @see .getPinchMode
         */
        const val PINCH_MODE_SCROLL = 1

        /**
         * 手势状态：双指缩放状态
         *
         * @see .getPinchMode
         */
        const val PINCH_MODE_SCALE = 2

        var pm:PagesManager? = null
    }
}