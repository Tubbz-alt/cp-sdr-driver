package sdr.driver.cp.test.buddy

import android.app.Service
import android.content.Intent

class MainService : Service() {

    private val binder = Endpoints(this)

    override fun onBind(intent: Intent?) = binder
}
