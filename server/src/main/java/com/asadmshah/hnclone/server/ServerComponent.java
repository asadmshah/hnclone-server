package com.asadmshah.hnclone.server;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.cache.Cache;
import com.asadmshah.hnclone.cache.CacheModule;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.common.sessions.SessionManagerModule;
import com.asadmshah.hnclone.database.DatabaseModule;
import com.asadmshah.hnclone.database.PostsDatabase;
import com.asadmshah.hnclone.database.SessionsDatabase;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.pubsub.PubSubModule;
import dagger.Component;
import org.apache.commons.configuration2.Configuration;

import javax.inject.Singleton;

@Singleton
@Component(
        modules = {
                ServerModule.class,
                DatabaseModule.class,
                SessionManagerModule.class,
                PubSubModule.class,
                CacheModule.class
        }
)
public interface ServerComponent {
    Configuration configuration();
    UsersDatabase usersDatabase();
    SessionManager sessionManager();
    PostsDatabase postsDatabase();
    SessionsDatabase sessionsDatabase();
    PubSub pubSub();
    BlockedSessionsCache blockedSessionsCache();
    Cache cache();

}
