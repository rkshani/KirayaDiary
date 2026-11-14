package com.kiraya.diary.di

import com.kiraya.diary.data.repository.BillRepositoryImpl
import com.kiraya.diary.data.repository.ExpenseRepositoryImpl
import com.kiraya.diary.data.repository.PropertyRepositoryImpl
import com.kiraya.diary.data.repository.RentRepositoryImpl
import com.kiraya.diary.data.repository.TenantRepositoryImpl
import com.kiraya.diary.domain.repository.BillRepository
import com.kiraya.diary.domain.repository.ExpenseRepository
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindPropertyRepository(impl: PropertyRepositoryImpl): PropertyRepository

    @Binds abstract fun bindTenantRepository(impl: TenantRepositoryImpl): TenantRepository

    @Binds abstract fun bindRentRepository(impl: RentRepositoryImpl): RentRepository

    @Binds abstract fun bindBillRepository(impl: BillRepositoryImpl): BillRepository

    @Binds abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
}
