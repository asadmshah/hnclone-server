package com.asadmshah.hnclone.common.sessions

import dagger.Module
import dagger.Provides
import org.apache.commons.configuration2.Configuration
import javax.inject.Singleton

@Module
internal class SessionManagerModule {

    @Provides
    @Singleton
    fun providesSessionManager(configuration: Configuration): SessionManager {
        return SessionManagerImpl.create(configuration)
    }

}