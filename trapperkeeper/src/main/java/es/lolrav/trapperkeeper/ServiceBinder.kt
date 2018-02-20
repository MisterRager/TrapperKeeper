package es.lolrav.trapperkeeper

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import java.lang.RuntimeException

class ServiceBinder<B : Binder>(private val binderClass: Class<B>) : ServiceConnection, ObservableOnSubscribe<B> {
    private var emitter: ObservableEmitter<B>? = null

    override fun onServiceDisconnected(name: ComponentName?) {
        emitter?.onError(RuntimeException("Service ${binderClass.simpleName} Disconnected?"))
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null && binderClass.isInstance(service)) {
            emitter?.onNext(binderClass.cast(service))
        }
    }

    override fun subscribe(emitter: ObservableEmitter<B>) {
        this.emitter?.onError(RuntimeException("Re-Observing a busy ServiceBinder!"))
        this.emitter = emitter
    }

    companion object {
        inline fun <reified B : Binder> bind(context: Context, launch: Intent): Observable<B> =
                Observable.using(
                        { ServiceBinder(B::class.java) },
                        { con: ServiceBinder<B> ->
                            Observable.create(con)
                                    .doOnSubscribe { context.bindService(launch, con, BIND_AUTO_CREATE) }
                        },
                        context::unbindService)
    }
}