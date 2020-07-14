package sdr.driver.cp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import sdr.driver.cp.test.buddy.Buddy
import org.junit.rules.ExternalResource

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class BuddyRule private constructor(private val id: String = "one") : ExternalResource(), ServiceConnection {

    private var _buddy: Buddy? = null
    val buddy: Buddy
        get() =
            if (Looper.myLooper() == Looper.getMainLooper())
                throw Exception("buddy must be get() off the main thread")
            else synchronized(this) {
                _buddy ?: run {
                    (this as Object).wait(ConnectTimeout)
                    _buddy ?: throw Exception("no connection to buddy $id service")
                }
            }

    override fun before() {
        val bound = InstrumentationRegistry.getInstrumentation().context.bindService(
            Intent().apply {
                component = ComponentName(
                    "sdr.driver.cp.test.buddy.$id",
                    "sdr.driver.cp.test.buddy.MainService"
                )
            },
            this,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
        if (!bound)
            throw Exception("binding to buddy $id service failed")
    }

    override fun after() {
        InstrumentationRegistry.getInstrumentation().context.unbindService(this)
    }

    @Synchronized
    override fun onServiceDisconnected(name: ComponentName) {
        _buddy = null
    }

    @Synchronized
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        _buddy = Buddy.Stub.asInterface(service)
        (this as Object).notifyAll()
    }

    companion object {
        const val ConnectTimeout = 10_000L

        fun one() = BuddyRule("one")
        fun two() = BuddyRule("two")
    }
}