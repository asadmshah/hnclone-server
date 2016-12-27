package com.asadmshah.hnclone.server;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.common.sessions.SessionManagerModule;
import com.asadmshah.hnclone.server.database.DatabaseModule;
import com.asadmshah.hnclone.server.database.PostsDatabase;
import com.asadmshah.hnclone.server.database.UsersDatabase;
import dagger.Component;
import org.apache.commons.configuration2.Configuration;

import javax.inject.Singleton;

@Singleton
@Component(
        modules = {
                ServerModule.class,
                DatabaseModule.class,
                SessionManagerModule.class,
        }
)
public interface ServerComponent {
    Configuration configuration();
    UsersDatabase usersDatabase();
    SessionManager sessionManager();
    PostsDatabase postsDatabase();
}
