package com.sjc.baseAndroid.base;

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.databinding.ViewDataBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener
import com.scwang.smartrefresh.layout.listener.OnRefreshListener
import com.sjc.baseAndroid.KtBaseApplication
import com.sjc.baseAndroid.R
import com.sjc.baseAndroid.dialog.BaseContentDialog
import com.sjc.baseAndroid.dialog.BaseLoadingDialog
import com.sjc.baseAndroid.utils.CustomToast
import com.sjc.baseAndroid.utils.Share
import com.sjc.baseAndroid.utils.Share.Companion.clearUidOrToken
import me.goldze.mvvmhabit.base.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

open abstract class ProActivity<V: ViewDataBinding,VM : KtBaseViewModel> : BaseActivity<V, VM>(), IBaseView {
    public var contentDialog: BaseContentDialog? = null
    protected var mContext: Context? = null
    var gson: Gson? = null

    private val ERROR = 404
    private val ERROR_BUG = 405
    var handler: Handler? = null
    var token = ""

    //Layout
    protected var base_layout: View? = null

    //Toolbar
    protected var toolbar: Toolbar? = null

    //toolbar back
    protected var backLayout: LinearLayout? = null
    protected var iv_back: ImageView? = null
    protected var tv_back: TextView? = null

    //toolbar right
    protected var toolbarRight: RelativeLayout? = null
    protected var toolbarRightText: TextView? = null
    protected var toolbarRightImg: ImageView? = null

    //toolbar title
    protected var toolbarTitle: TextView? = null

    //??????
    protected var mRefreshLayout: SmartRefreshLayout? = null
    private var container: FrameLayout? = null

    //emptyLayout
    private var base_empty_layout: View? = null //?????????
    private var empty_text: TextView? = null

    //content layout
    protected var content_layout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (istransparentToolbar()) {
            setTheme(R.style.transparentTheme)
            statusLan()
            setToolBarVisible(false)
        }
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        AppManager.getAppManager().addActivity(this)
//        loadingDialog = LoadingDialog(this, "")
        mContext = this
        setContentView(initContentView(savedInstanceState))

//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;//????????????
//        this.savedInstanceStateMain = savedInstanceState
//        setStatusBar(R.color.white)
        token = Share.getToken(this)
        gson = Gson()
//        handler = ProHandler(this)
//        titleBar = findViewById(R.id.titleBar)
//        if (titleBar != null){
////            val paint: TextPaint = titleBar!!.titleView.paint
////            paint.style = Paint.Style.FILL_AND_STROKE
////            paint.strokeWidth = 2f
//            titleBar!!.onTitleBarListener = this
//        }

        // ?????????ui
        initView()
        // ???????????????
        initData()
        // ???????????????
        initListener()
    }

    /**
     * @?????????????????????????????????????????????????????????false????????????
     */
    protected open fun istransparentToolbar(): Boolean {
        return true
    }

    /**
     * @?????????????????????????????????
     */
    open fun statusLan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val decorView = window.decorView
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = option
            window.statusBarColor = Color.TRANSPARENT
            //4.4???5.0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val localLayoutParams =
                    window.attributes
            localLayoutParams.flags =
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or localLayoutParams.flags
        }
        //android6.0?????????????????????????????????????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
    }

    /**
     * @???????????????????????????Toolbar
     */
    open fun setToolBarVisible(isVisible: Boolean) {
        if (this.toolbar != null) {
            if (isVisible) {
                toolbar!!.setVisibility(View.VISIBLE)
            } else {
                toolbar!!.setVisibility(View.GONE)
            }
        }
    }

    override fun setContentView(layoutResID: Int) {
        base_layout = LayoutInflater.from(this).inflate(R.layout.base_act_layout, null)

        toolbar = base_layout!!.findViewById(R.id.toolbar)
        toolbar!!.setBackgroundColor(Color.parseColor("#ffffff"))
        //back
        backLayout = base_layout!!.findViewById(R.id.toolbar_back)
        iv_back = base_layout!!.findViewById(R.id.iv_back)
        iv_back!!.setImageResource(R.mipmap.back)
        tv_back = base_layout!!.findViewById(R.id.tv_back)
        tv_back!!.setVisibility(View.GONE)
        //right
        toolbarRight = base_layout!!.findViewById(R.id.right_layout)
        toolbarRightImg = base_layout!!.findViewById(R.id.toolbar_right_img)
        toolbarRightImg!!.setVisibility(View.GONE)
        toolbarRightText = base_layout!!.findViewById(R.id.toolbar_right_text)
        toolbarRightText!!.setVisibility(View.GONE)
        //title
        toolbarTitle = base_layout!!.findViewById(R.id.toolbar_title)
        toolbarTitle!!.setTextColor(Color.parseColor("#333333"))
        //empty
        base_empty_layout = base_layout!!.findViewById(R.id.empty_layout)
        base_empty_layout!!.setVisibility(View.GONE)
        empty_text = base_layout!!.findViewById(R.id.base_empty_text)
        empty_text!!.setText("?????????~_~")
        //content
        initRefreshLayout()
        container = base_layout!!.findViewById(R.id.container)
        content_layout = LayoutInflater.from(this).inflate(layoutResID, null)
        content_layout!!.setVisibility(View.VISIBLE)

        container!!.addView(content_layout, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        backLayout!!.setOnClickListener(View.OnClickListener { finish() })
        iv_back!!.setOnClickListener(View.OnClickListener { finish() })

        setContentView(base_layout)
    }

    /* ?????? */
    private fun initRefreshLayout() {
        mRefreshLayout = base_layout!!.findViewById(R.id.refreshLayout)
        mRefreshLayout!!.setDisableContentWhenRefresh(true) //????????????????????????????????????
        mRefreshLayout!!.setEnableOverScrollBounce(true) //????????????????????????
        mRefreshLayout!!.setEnableRefresh(false) //??????????????????
        mRefreshLayout!!.setEnableLoadMore(false) //??????????????????
        mRefreshLayout!!.setEnableAutoLoadMore(false)
    }

    /**
     * @author cx
     * @time 2018/5/8 0008 ?????? 2:26
     * @?????????????????????toolbar
     */

    public fun hideToolbar() {
        this.toolbar!!.setVisibility(View.GONE);
    }

    /**
     * ??????toolBar????????????
     *
     * @param color
     */
    public fun setToolBarColor(color: Int) {
        if (this.toolbar != null) {
            this.toolbar!!.setBackgroundColor(color);
        }
    }

    /**
     * ??????toolBar????????????
     *
     * @param res
     */
    public fun setToolBarRes(res: Int) {
        if (this.toolbar != null) {
            this.toolbar!!.setBackgroundResource(res);
        }
    }

    /**
     * ????????????????????????
     *
     * @param isVisible
     */
    public fun setBackBtnVisible(isVisible: Boolean) {
        if (this.backLayout != null) {
            if (isVisible) {
                this.backLayout!!.setVisibility(View.VISIBLE);
            } else {
                this.backLayout!!.setVisibility(View.GONE);
            }
        }
    }

    /**
     * ????????????Layout
     *
     * @return
     */
    public fun getToolbarBack(): LinearLayout? {
        return this.backLayout;
    }

    /**
     * ??????????????????
     *
     * @param s
     */
    public fun setBackText(s: String) {
        if (this.tv_back != null) {
            this.tv_back!!.setText(s);
            this.tv_back!!.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ??????????????????
     *
     * @param resId
     */
    public fun setBackBtnDrawable(resId: Int) {
        if (this.iv_back != null) {
            this.iv_back!!.setImageResource(resId);
            this.iv_back!!.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ??????right
     */
    public fun showRightView() {
        this.toolbarRight!!.setVisibility(View.VISIBLE);
    }

    /**
     * ??????right
     */
    public fun hidRightView() {
        this.toolbarRight!!.setVisibility(View.GONE);
    }

    /**
     * ??????right text
     *
     * @param s
     */
    public fun setRightTextView(s: String) {
        this.toolbarRightText!!.setVisibility(View.VISIBLE);
        this.toolbarRightText!!.setText(s);
    }

    /**
     * ??????right text
     *
     * @param color
     */
    public fun setRightTextColor(color: Int) {
        this.toolbarRightText!!.setTextColor(color);
    }

    /**
     * ??????title
     *
     * @param title
     */
    @Override
    public fun setToolbarTitle(title: CharSequence) {
        if (toolbarTitle != null) {
            toolbarTitle!!.setText(title);
        }
    }

    /**
     * ??????right img
     *
     * @return
     */
    public fun setRightImagViewDrawable(resId: Int) {
        this.toolbarRightImg!!.setImageResource(resId);
        this.toolbarRightImg!!.setVisibility(View.VISIBLE);
    }

    /**
     * ??????????????????
     */
    public fun hideRightImagView() {
        this.toolbarRightImg!!.setVisibility(View.GONE);
    }

    /* ?????????????????? */
    public fun setContentBackgroundColor(color: Int) {
        this.mRefreshLayout!!.setBackgroundColor(color);
    }

    /* ???????????????????????? */
    public fun setEnableRefresh(isEnable: Boolean) {
        this.mRefreshLayout!!.setEnableRefresh(isEnable);
    }

    /* ?????????????????????????????? */
    public fun setEnableLoadMore(isEnable: Boolean) {
        this.mRefreshLayout!!.setEnableLoadMore(isEnable);
    }

    /* ???????????????????????????????????? */
    public fun setEnableAutoLoadMore(isEnable: Boolean) {
        this.mRefreshLayout!!.setEnableAutoLoadMore(isEnable);
    }

    /* ?????????????????? */
    public fun setOnRefreshListener(listener: OnRefreshListener) {
        this.mRefreshLayout!!.setOnRefreshListener(listener);
    }

    /* ???????????????????????? */
    public fun setOnLoadMoreListener(listener: OnLoadMoreListener) {
        this.mRefreshLayout!!.setOnLoadMoreListener(listener);
    }

    /* ???????????? */
    public fun finishRefresh() {
        this.mRefreshLayout!!.finishRefresh();
    }

    /* ???????????? */
    public fun finishLoadMore() {
        this.mRefreshLayout!!.finishLoadMore();
    }

    /* ??????????????????????????????????????? */
     fun finishLoadMoreWithNoMoreData() {
        this.mRefreshLayout!!.finishLoadMoreWithNoMoreData();
    }

    /* ??????????????????????????????????????? */
    public fun resetNoMoreData() {
        this.mRefreshLayout!!.resetNoMoreData();
    }

    //?????????????????????Psuedo ID
    open fun getUniquePsuedoID(): String? {
        var serial: String? = null
        val m_szDevIDShort =
                "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 + Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 + Build.TYPE.length % 10 + Build.USER.length % 10 //13 ???
        try {
            serial = Build::class.java.getField("SERIAL")[null].toString()
            //API>=9 ??????serial???
            return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        } catch (exception: Exception) { //serial?????????????????????
            serial = "serial" // ?????????????????????
        }
        //?????????????????????????????????15?????????
        return UUID(m_szDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }

    fun longToTime(l: Long, type: String): String {
        val format = SimpleDateFormat(type, Locale.CHINA)

        return format.format(Date(l * 1000))

    }

    val isDebug: Boolean = true

    fun log(message: String) {
        if (isDebug) {
            Log.i("toby", message)
        }

    }

    fun setAlpha(f: Float) {
        val manager = window.attributes
        manager.alpha = f
        window.attributes = manager
    }

    protected abstract fun initView()

    protected abstract fun initListener()

    protected fun hideIMM() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    override fun onDestroy() {
        AppManager.getAppManager().finishActivity(this)
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            hideSoftInput(v, ev)

            return super.dispatchTouchEvent(ev)
        }
        // ????????????????????????????????????????????????TouchEvent???
        return if (window.superDispatchTouchEvent(ev)) {
            true
        } else onTouchEvent(ev)
    }

    private fun hideSoftInput(v: View?, ev: MotionEvent) {
        var v = v
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm != null && imm.isActive) {
            if (isShouldHideInput(v, ev)) {
                if (v == null) {
                    v = window.decorView
                }
                imm.hideSoftInputFromWindow(v!!.windowToken, 0)
            }
        }
    }

    /**
     * ?????????????????????????????????
     * ??????EditText?????????????????????????????????????????????
     *
     * @param v
     * @param event
     * @return
     */
    fun isShouldHideInput(v: View?, event: MotionEvent): Boolean {
        return if (v is EditText) {
            !isPointInView(v, event.x.toInt(), event.y.toInt())
        } else true
    }

    /**
     * ????????????????????????
     */
    fun checkPhotoPremission(): Boolean {
        val code = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return code == PackageManager.PERMISSION_GRANTED
    }

    /**
     * ?????????????????????
     */
    fun checkCameraPremission(): Boolean {
        val code = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        return code == PackageManager.PERMISSION_GRANTED
    }

    /**
     * ????????????(x, y)?????????view??????????????????
     *
     * @param view
     * @param x
     * @param y
     * @return
     */
    fun isPointInView(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        // ???????????????????????????????????????window?????????activity???????????????Screen???Window?????????
        //        view.getLocationOnScreen(location);
        view.getLocationInWindow(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.measuredWidth
        val bottom = top + view.measuredHeight
        return (y >= top && y <= bottom
                && x >= left && x <= right)
    }

    protected fun openActivity(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    protected fun openActivity(cls: Class<*>, intentName: String, intentValue: String) {
        startActivity(Intent(this, cls).putExtra(intentName, intentValue))
    }

    companion object {

//        class ProHandler(proActivity: ProActivity) : Handler(Looper.getMainLooper()) {
//            val weak: WeakReference<ProActivity> = WeakReference(proActivity)
//            val a = weak.get()
//            override fun handleMessage(msg: Message) {
//                super.handleMessage(msg)
//                when (msg!!.what) {
//
//                    a!!.ERROR -> {
////                        a.loadingDialog!!.dismiss()
//                        a.hideLoading()
//                        a.showToastShort("????????????????????????????????????!")
//                    }
//
//                    a.ERROR_BUG -> {
////                        a.loadingDialog!!.dismiss()
//                        a.hideLoading()
//                        a.showToastShort("??????????????????????????????")
//                    }
//                    else -> {
//                        val str = msg.obj as String
//                        if (str.contains("code")) {
//                            val json = JSONObject(str)
//                            val msg1 = json.optString("msg")
//                            val code = json.optString("code")
//                            if (code == "501") {
//                                a.showToastShort(msg1)
//
//                                Share.clearUidOrToken(a)
//                                /// ??????app
////                                val intent = Intent(a, CodeActivity::class.java)
////                                a.startActivity(intent)
////                                AppManager.getAppManager().finishAllActivity()
//                            }
//                            if (msg1 == "???????????????????????????" || msg1 == "????????????token") {
//                                a.showToastShort(msg1)
//
//                                Share.clearUidOrToken(a)
//                                /// ??????app
////                                val intent = Intent(a, CodeActivity::class.java)
////                                a.startActivity(intent)
////                                AppManager.getAppManager().finishAllActivity()
//                            } else {
//                                a.handler(msg)
//                            }
//                        } else {
//                            a.handler(msg)
//                        }
//
//                    }
//                }
//            }
//        }
    }

    fun showToastShort(s: String) {

        CustomToast.showToast(this, Gravity.CENTER, 0, s)

    }



    private var dialog_tips: Dialog? = null

    /**
     */
    private fun showContentDialog( title: String, content: String, click: View.OnClickListener) {
        if (contentDialog == null) {

            contentDialog =
                BaseContentDialog("",content, click)
        }
        contentDialog!!.show(supportFragmentManager, javaClass.name)
    }
    private fun dismissContentDialog(){
        if (contentDialog !== null && contentDialog!!.isAdded){
            contentDialog!!.dismiss()
        }

    }


     fun getContext(): Context? {
        return mContext;
    }

    /**
     * ?????????????????????
     * ??????????????????????????????
     * @param msg
     */
     fun showLoading(msg: String?) {
        BaseLoadingDialog.show(this, msg, true, true)
    }

    /**
     * ?????????????????????
     * ??????????????????????????????
     * @param msg
     */
    public fun showLoading(msg: String?, isTouchOut: Boolean, isCancel: Boolean) {
        BaseLoadingDialog.show(this, msg, isTouchOut, isCancel)
    }

     fun hideLoading() {
        BaseLoadingDialog.dismiss(this)
        finishRefresh()
        finishLoadMore()
    }

     fun showToast(s: CharSequence?) {
        showToastShort(s.toString())
    }

     fun showNullLayout() {
        base_empty_layout!!.visibility = View.VISIBLE
        content_layout!!.visibility = View.GONE
    }

     fun hideNullLayout() {
        base_empty_layout!!.visibility = View.GONE
        content_layout!!.visibility = View.VISIBLE
    }

     fun showErrorLayout(listener: View.OnClickListener?) {
    }

     fun hideErrorLayout() {
    }

     fun onLoginOut() {
        clearUidOrToken(mContext!!)
//        val intent = Intent(this, CodeActivity::class.java)
//        this.startActivity(intent)
        AppManager.getAppManager().finishAllActivity()
    }

    /* ????????? */
    fun goToLogin() {
//        var intent = Intent(this, CodeActivity::class.java)
//        startActivityForResult(intent, 9999)
    }

    fun getMinUid(): Int {
        return Share.getUid(KtBaseApplication().getAppContext()!!)
    }

    /* ???????????? */
    fun onHideSoftInput(v: View) {
        val imm = mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
    /**
     * ??????????????????
     * @param s
     * @return
     */
    fun stringToFormat(s: String): String {
        if (s == null || s.isEmpty()) {
            return "0.00"
        }
        val format = DecimalFormat("#0.00")
        return format.format(s.toDouble())
    }
}
