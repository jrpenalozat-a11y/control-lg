package com.controllg.app

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Envía códigos NEC por el emisor infrarrojo del celular. No usa red ni Bluetooth. */
class IrSender(context: Context) {

    private val manager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean = try {
        manager?.hasIrEmitter() == true
    } catch (e: Exception) {
        false
    }

    private val mutex = Mutex()

    suspend fun send(code: Long): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val ir = manager ?: throw IllegalStateException("Sin emisor infrarrojo")
                ir.transmit(NEC_CARRIER_HZ, encodeNec(code))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
