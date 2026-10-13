package com.hananakick.arrrr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hananakick.arrrr.ReaderApplication
import com.hananakick.arrrr.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ReaderApplication
    val library = app.repository.library
    val ready = app.repository.ready
    val refreshing = app.repository.refreshing
    val settings = app.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())
    private val messages = Channel<String>(Channel.BUFFERED)
    val notices = messages.receiveAsFlow()

    fun updateSettings(change: (ReaderSettings) -> ReaderSettings) = launchAction { app.settings.update(change) }
    fun markRead(id: String) = launchAction { app.repository.markRead(id) }
    fun setSaved(id: String, saved: Boolean) = launchAction { app.repository.setSaved(id, saved) }
    fun markAllRead() = launchAction { app.repository.markAllRead() }
    fun unsubscribe(source: FeedSource) = launchAction {
        app.repository.unsubscribe(source.id)
        messages.send("구독을 해제했습니다. 저장한 글은 유지됩니다.")
    }
    fun editSource(source: FeedSource, change: (FeedSource) -> FeedSource) = launchAction { app.repository.editSource(source.id, change) }
    suspend fun addFeed(url: String, name: String, folder: String): kotlin.Result<FeedSource> = try {
        kotlin.Result.success(app.repository.addFeed(url, name, folder))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (e: Exception) { kotlin.Result.failure(e) }

    fun refresh() = launchAction {
        val failures=app.repository.refresh(settings.value, manual=true)
        if (failures>0) messages.send("${failures}개 피드를 확인하지 못했습니다. 구독 화면에서 다시 확인하세요.")
    }
    /** Called only from repeatOnLifecycle(RESUMED); leaving the screen cancels this job. */
    suspend fun whileVisible() {
        val onOpen=settings.value.refreshOnOpen
        if (onOpen) app.repository.refresh(settings.value)
        settings.map { it.foregroundMinutes to it.wifiOnly }.distinctUntilChanged().collectLatest { (minutes, _) ->
            if (minutes==0) return@collectLatest
            while (currentCoroutineContext().isActive) {
                delay(minutes * 60_000L)
                app.repository.refresh(settings.value)
            }
        }
    }
    private fun launchAction(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) { messages.send(e.message ?: "작업을 완료하지 못했습니다.") }
    }
}
