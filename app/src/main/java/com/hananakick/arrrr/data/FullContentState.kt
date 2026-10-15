package com.hananakick.arrrr.data

sealed interface FullContentState {
    data object Loading : FullContentState
    data class Ready(val html: String) : FullContentState
    data class Unavailable(val explanation: String) : FullContentState
}