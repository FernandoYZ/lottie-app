package com.fernandoyz.lottie.presentation.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.repository.SetupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface StartState {
    data object Loading : StartState
    data object Error : StartState
    data object NeedsOnboarding : StartState
    data object SetupCompleted : StartState
}

/** Decides the start destination once, from the persisted setup. */
class StartViewModel(private val repository: SetupRepository) : ViewModel() {

    private val _startState = MutableStateFlow<StartState>(StartState.Loading)
    val startState: StateFlow<StartState> = _startState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _startState.value = StartState.Loading
        viewModelScope.launch {
            _startState.value = try {
                if (repository.initialSetup.first() == null) StartState.NeedsOnboarding else StartState.SetupCompleted
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                StartState.Error
            }
        }
    }

    companion object {
        fun factory(repository: SetupRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { StartViewModel(repository) }
        }
    }
}
