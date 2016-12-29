package com.asadmshah.hnclone.common.sessions

import dagger.Module
import dagger.Provides
import org.apache.commons.configuration2.Configuration
import javax.inject.Singleton

@Module
internal class SessionManagerModule {

    @Provides
    @Singleton
    internal fun providesSessionManager(configuration: Configuration): SessionManager {
        val req = configuration.getString("auth.secret.request").toByteArray()
        val ref = configuration.getString("auth.secret.refresh").toByteArray()
        return SessionManagerImpl(req, ref)
    }

}