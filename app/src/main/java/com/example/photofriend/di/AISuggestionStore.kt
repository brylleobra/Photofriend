package com.example.photofriend.di

import com.example.photofriend.domain.model.AISuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AISuggestionStore @Inject constructor() {

    private val _suggestion = MutableStateFlow<AISuggestion?>(null)
    val suggestion: StateFlow<AISuggestion?> = _suggestion.asStateFlow()

    fun store(suggestion: AISuggestion) {
        _suggestion.value = suggestion
    }

    fun clear() {
        _suggestion.value = null
    }
}
