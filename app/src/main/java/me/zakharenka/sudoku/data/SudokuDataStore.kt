package me.zakharenka.sudoku.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.zakharenka.sudoku.game.SudokuData

/**
 * One atomic JSON file in filesDir. All reads and writes share one mutex and
 * run on Dispatchers.IO, so there is a single writer at a time.
 */
class SudokuDataStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()

    suspend fun load(): SudokuData = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.exists()) {
                return@withLock SudokuData()
            }

            val raw = runCatching {
                AtomicFile(file).openRead().use { it.readBytes().decodeToString() }
            }.getOrNull() ?: return@withLock SudokuData()

            when (val decoded = SudokuDataCodec.decode(raw)) {
                is DataDecodeResult.Success -> decoded.data
                is DataDecodeResult.Failure -> SudokuData()
            }
        }
    }

    suspend fun save(data: SudokuData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            write(data)
        }
    }

    private fun write(data: SudokuData) {
        val payload = SudokuDataCodec.encode(data).toByteArray()
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()

        try {
            stream.write(payload)
            atomicFile.finishWrite(stream)
        } catch (exception: IOException) {
            atomicFile.failWrite(stream)
            throw exception
        }
    }

    private companion object {
        const val FILE_NAME = "sudoku_data.json"
    }
}
