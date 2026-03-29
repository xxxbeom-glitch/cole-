package com.aptox.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱 카테고리 조회 단일 진입점.
 *
 * 우선순위:
 * 1순위: DataStore manual_category_overrides (사용자 수동 수정)
 * 2순위: DataStore AI 분류 캐시 (AppCategoryCacheRepository)
 * 3순위: "기타"
 */
class AppCategoryRepository(private val context: Context) {

    private val manualRepo = AppCategoryManualOverrideRepository(context)
    private val cacheRepo = AppCategoryCacheRepository(context)

    /**
     * 단일 앱의 카테고리 반환.
     * 수동 오버라이드 → AI 캐시 → "기타" 순으로 적용.
     */
    suspend fun getCategory(packageName: String): String = withContext(Dispatchers.IO) {
        manualRepo.get(packageName)
            ?: cacheRepo.getCache()[packageName]
            ?: "기타"
    }

    /**
     * 전체 앱 카테고리 맵 반환 (수동 오버라이드 우선 병합).
     * key: packageName, value: categoryString
     */
    suspend fun getAllCategories(): Map<String, String> = withContext(Dispatchers.IO) {
        val aiCache = cacheRepo.getCache()
        val manualOverrides = manualRepo.getAll()
        // AI 캐시 기반으로 시작하고 수동 오버라이드로 덮어씀
        aiCache + manualOverrides
    }
}
