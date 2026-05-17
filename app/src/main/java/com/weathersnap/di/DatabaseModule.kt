package com.weathersnap.di

import android.content.Context
import androidx.room.Room
import com.weathersnap.data.local.WeatherSnapDatabase
import com.weathersnap.data.local.dao.DraftDao
import com.weathersnap.data.local.dao.ReportDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeatherSnapDatabase =
        Room.databaseBuilder(context, WeatherSnapDatabase::class.java, "weather_snap.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideReportDao(db: WeatherSnapDatabase): ReportDao = db.reportDao()
    @Provides fun provideDraftDao(db: WeatherSnapDatabase): DraftDao = db.draftDao()
}
