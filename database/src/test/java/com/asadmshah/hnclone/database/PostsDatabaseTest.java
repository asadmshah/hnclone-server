package com.asadmshah.hnclone.database;

import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.User;
import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PostsDatabaseTest extends BaseDatabaseTest {

    @Before
    public void setUp() throws Exception {
        init();
    }

    @Test
    public void test1() throws Exception {
        UsersDatabase udb = new UsersDatabaseImpl(dataSource);
        PostsDatabase pdb = new PostsDatabaseImpl(dataSource);

        User user1 = udb.create("Username 1", "Password 1", "");
        assertThat(user1).isNotNull();
        User user2 = udb.create("Username 2", "Password 2", "");
        assertThat(user2).isNotNull();

        List<Post> posts = new ArrayList<>(10);
        for (int i = 0; i < 5; i++) {
            String title = String.format("Post %d Title", i);
            String text = String.format("Post %d Text", i);
            String url = String.format("Post %d URL", i);

            Post post = pdb.create(user1.getId(), title, text, url);

            assertThat(post).isNotNull();
            assertThat(post.getTitle()).isEqualTo(title);
            assertThat(post.getText()).isEqualTo(text);
            assertThat(post.getUrl()).isEqualTo(url);
            assertThat(post.getUserId()).isEqualTo(user1.getId());
            assertThat(post.getUserName()).isEqualTo(user1.getUsername());

            posts.add(post);
        }

        for (int i = 5; i < 10; i++) {
            String title = String.format("Post %d Title", i);
            String text = String.format("Post %d Text", i);
            String url = String.format("Post %d URL", i);

            Post post = pdb.create(user2.getId(), title, text, url);

            assertThat(post).isNotNull();
            assertThat(post.getUserId()).isEqualTo(user2.getId());
            assertThat(post.getUserName()).isEqualTo(user2.getUsername());

            posts.add(post);
        }

        assertThat(pdb.incrementScore(user1.getId(), posts.get(0).getId())).isEqualTo(1);
        assertThat(pdb.incrementScore(user2.getId(), posts.get(0).getId())).isEqualTo(2);
        assertThat(pdb.decrementScore(user1.getId(), posts.get(0).getId())).isEqualTo(0);
        assertThat(pdb.removeScore(user1.getId(), posts.get(0).getId())).isEqualTo(1);

        pdb.incrementScore(user1.getId(), posts.get(1).getId());
        pdb.incrementScore(user2.getId(), posts.get(1).getId());

        List<Post> postsList;

        postsList = pdb.readTop(0, 2, 0).toList().blockingGet();
        assertThat(postsList).isNotNull();
        assertThat(postsList.get(0).getId()).isEqualTo(posts.get(1).getId());
        assertThat(postsList.get(1).getId()).isEqualTo(posts.get(0).getId());

        postsList = pdb.readNew(0, 2, 0).toList().blockingGet();
        assertThat(postsList).isNotNull();
        assertThat(postsList).containsExactly(posts.get(9), posts.get(8));

        postsList = Flowable.concat(
                pdb.readTop(0, user1.getId(), 10, 0),
                pdb.readTop(0, user2.getId(), 10, 0)
        ).toList().blockingGet();
        assertThat(postsList.get(0).getId()).isEqualTo(posts.get(1).getId());
        assertThat(postsList.get(1).getId()).isEqualTo(posts.get(0).getId());

        postsList = pdb.readNew(0, user1.getId(), 10, 0).toList().blockingGet();
        assertThat(postsList).hasSize(5);
        assertThat(postsList.get(0).getId()).isEqualTo(posts.get(4).getId());

        postsList = pdb.readNew(0, user2.getId(), 10, 0).toList().blockingGet();
        assertThat(postsList).hasSize(5);
        assertThat(postsList.get(0).getId()).isEqualTo(posts.get(9).getId());

        Post post = pdb.read(0, posts.get(0).getId());
        assertThat(post).isNotNull();
        assertThat(post.getId()).isEqualTo(posts.get(0).getId());
    }

}
