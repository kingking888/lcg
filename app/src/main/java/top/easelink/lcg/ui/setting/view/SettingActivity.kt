package top.easelink.lcg.ui.setting.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tencent.bugly.beta.Beta
import kotlinx.android.synthetic.main.activity_settings.*
import timber.log.Timber
import top.easelink.framework.topbase.TopActivity
import top.easelink.lcg.R
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.service.work.SignInWorker
import top.easelink.lcg.spipedata.UserData
import top.easelink.lcg.ui.main.login.view.LoginHintDialog
import top.easelink.lcg.ui.main.logout.view.LogoutHintDialog
import top.easelink.lcg.ui.setting.viewmodel.SettingViewModel
import top.easelink.lcg.utils.clearCookies
import top.easelink.lcg.utils.showMessage


class SettingActivity : TopActivity() {

    private lateinit var mViewModel: SettingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        setContentView(R.layout.activity_settings)
        mViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
        setUp()
    }

    override fun onResume() {
        super.onResume()
        val configuration = resources.configuration
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun setUp() {
        setupToolBar()
        setupComponents()
        setupObserver()
        mViewModel.init()
    }

    private fun setupToolBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupComponents() {
        if (!UserData.isLoggedIn) {
            sync_favorites_switch.isEnabled = false
            auto_sign_switch.isEnabled = false
            AppConfig.autoSignEnable = false
            AppConfig.syncFavorites = false
            account_btn.text = getString(R.string.login_btn)
            account_btn.setOnClickListener {
                LoginHintDialog().show(supportFragmentManager, null)
            }
        } else {
            account_btn.text = getString(R.string.logout_confirm_message, UserData.username)
            account_btn.setOnClickListener {
                tryLogout()
            }
        }
        check_update_btn.setOnClickListener {
            Beta.checkUpgrade()
        }
        sync_favorites_switch.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.scheduleJob(isChecked)
        }
        auto_sign_switch.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.setSyncFavorite(isChecked)
        }
        search_engine_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                AppConfig.defaultSearchEngine = position
            }

        }
        show_search_result_in_webview.setOnCheckedChangeListener { _, isChecked ->
            AppConfig.searchResultShowInWebView = isChecked
        }
    }

    private fun setupObserver() {
        mViewModel.syncFavoriteEnable.observe(this, Observer {
            sync_favorites_switch.isChecked = it
        })
        mViewModel.autoSignInEnable.observe(this, Observer {
            auto_sign_switch.isChecked = it
        })
        mViewModel.searchEngineSelected.observe(this, Observer {
            search_engine_spinner.setSelection(it, true)
        })
        mViewModel.openSearchResultInWebView.observe(this, Observer {
            show_search_result_in_webview.isChecked = it
        })
        WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData(SignInWorker.TAG).observe(this, Observer { list ->
            list.first().let { 
                when(it.state) {
                    WorkInfo.State.ENQUEUED -> Timber.d("ENQUEUED")
                    WorkInfo.State.RUNNING -> Timber.d("RUNNING")
                    WorkInfo.State.SUCCEEDED -> Timber.d("SUCCEEDED")
                    WorkInfo.State.FAILED -> Timber.d("FAILED")
                    WorkInfo.State.BLOCKED -> Timber.d("BLOCKED")
                    WorkInfo.State.CANCELLED -> Timber.d("CANCELLED")
                }
            }
        })
    }

    private fun tryLogout() {
        LogoutHintDialog(
            positive = {
                UserData.clearAll()
                clearCookies()
                showMessage(R.string.clear_cookie)
                finish()
            },
            negative = { }
        ).show(supportFragmentManager, LogoutHintDialog::class.java.simpleName)
    }
}
