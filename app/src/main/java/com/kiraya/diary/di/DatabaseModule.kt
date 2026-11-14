package com.kiraya.diary.di

import android.content.Context
import androidx.room.Room
import com.kiraya.diary.data.local.KirayaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KirayaDatabase =
        Room.databaseBuilder(context, KirayaDatabase::class.java, KirayaDatabase.NAME).build()

    @Provides fun providePropertyDao(db: KirayaDatabase) = db.propertyDao()

    @Provides fun provideUnitDao(db: KirayaDatabase) = db.unitDao()

    @Provides fun provideTenantDao(db: KirayaDatabase) = db.tenantDao()

    @Provides fun provideLeaseDao(db: KirayaDatabase) = db.leaseDao()

    @Provides fun provideRentEntryDao(db: KirayaDatabase) = db.rentEntryDao()

    @Provides fun providePaymentDao(db: KirayaDatabase) = db.paymentDao()

    @Provides fun provideBillDao(db: KirayaDatabase) = db.billDao()

    @Provides fun provideExpenseDao(db: KirayaDatabase) = db.expenseDao()
}
