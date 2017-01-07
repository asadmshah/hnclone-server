package com.asadmshah.hnclone.server;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.common.sessions.SessionManagerModule;
import com.asadmshah.hnclone.database.DatabaseModule;
import com.asadmshah.hnclone.database.PostsDatabase;
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
                PubSubModule.class
        }
)
public interface ServerComponent {
    Configuration configuration();
    UsersDatabase usersDatabase();
    SessionManager sessionManager();
    PostsDatabase postsDatabase();
    PubSub pubSub();
}
