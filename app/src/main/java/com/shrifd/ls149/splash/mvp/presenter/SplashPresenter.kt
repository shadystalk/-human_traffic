package com.shrifd.ls149.splash.mvp.presenter

import android.Manifest
import androidx.fragment.app.FragmentActivity
import com.jess.arms.di.scope.ActivityScope
import com.shrifd.ls149.router.RouterController
import com.shrifd.ls149.splash.mvp.contract.SplashContract
import com.tbruyelle.rxpermissions2.RxPermissions
import com.wan.commonsdk.base.activity.BaseActivityPresenter
import es.dmoral.toasty.Toasty
import javax.inject.Inject


/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 02/20/2021 13:35
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms">Star me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms/wiki">See me</a>
 * <a href="https://github.com/JessYanCoding/MVPArmsTemplate">模版请保持更新</a>
 * ================================================
 */
@ActivityScope
class SplashPresenter
@Inject
constructor(model: SplashContract.Model, var rootView: SplashContract.View) :
    BaseActivityPresenter(model, rootView) {

    override fun onCreate() {
        super.onCreate()
        requestRxPermissions()
    }

    private fun requestRxPermissions() {
        val rxPermissions = RxPermissions(rootView.getContext() as FragmentActivity)
        val disposable = rxPermissions.request(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
        )
            .subscribe {
                if (it) {
                    RouterController.LS149.jumpMainActivity(mRootView.getContext())
                } else {
                    Toasty.error(mRootView.getContext(), "权限异常").show()
                }
                rootView.killMyself()
            }
    }


}
