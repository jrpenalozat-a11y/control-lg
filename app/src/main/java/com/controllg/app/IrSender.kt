package com.controllg.app

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Envía códigos (NEC o RC6) por el emisor infrarrojo del celular. No usa red ni Bluetooth. */
class IrSender(context: Context) {

    private val manager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean = try {
        manager?.hasIrEmitter() == true
    } catch (e: Exception) {
        false
    }

    private val mutex = Mutex()
    private var rc6Toggle = false

    suspend fun send(code: Long, protocol: IrProtocol = IrProtocol.NEC): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val ir = manager ?: throw IllegalStateException("Sin emisor infrarrojo")
                val pattern = when (protocol) {
                    IrProtocol.NEC -> encodeNec(code)
                    IrProtocol.RC6 -> encodeRc6(code, rc6Toggle).also { rc6Toggle = !rc6Toggle }
                }
                ir.transmit(protocol.carrierHz, pattern)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
