package com.screenshotvault.ui.screens.domains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.data.repository.DomainsRepository
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.domain.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DomainsUiState(
    val isLoading: Boolean = true,
    val domains: List<DomainWithCount> = emptyList(),
    val selectedDomain: String? = null,
    val domainScreenshots: List<ScreenshotItem> = emptyList(),
)

@HiltViewModel
class DomainsViewModel @Inject constructor(
    private val domainsRepository: DomainsRepository,
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val _selectedDomain = MutableStateFlow<String?>(null)
    private val _domainScreenshots = MutableStateFlow<List<ScreenshotItem>>(emptyList())
    private val _isLoadingScreenshots = MutableStateFlow(false)

    val uiState: StateFlow<DomainsUiState> = combine(
        domainsRepository.observeDomainsWithCount(),
        _selectedDomain,
        _domainScreenshots,
        _isLoadingScreenshots,
    ) { domains, selectedDomain, screenshots, isLoadingScreenshots ->
        DomainsUiState(
            isLoading = false,
            domains = domains,
            selectedDomain = selectedDomain,
            domainScreenshots = screenshots,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DomainsUiState(),
    )

    fun selectDomain(domain: String) {
        _selectedDomain.value = domain
        loadDomainScreenshots(domain)
    }

    fun clearSelectedDomain() {
        _selectedDomain.value = null
        _domainScreenshots.value = emptyList()
    }

    private fun loadDomainScreenshots(domain: String) {
        viewModelScope.launch {
            _isLoadingScreenshots.value = true
            _domainScreenshots.value = domainsRepository.getScreenshotsForDomain(domain)
            _isLoadingScreenshots.value = false
        }
    }

    fun toggleItemSolved(id: String, solved: Boolean) {
        viewModelScope.launch {
            screenshotRepository.toggleSolved(id, solved)
            // Reload screenshots for the domain
            _selectedDomain.value?.let { loadDomainScreenshots(it) }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            screenshotRepository.deleteItem(id)
            // Reload screenshots for the domain
            _selectedDomain.value?.let { loadDomainScreenshots(it) }
        }
    }
}
