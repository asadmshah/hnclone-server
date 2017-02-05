package com.asadmshah.hnclone.database;

import com.asadmshah.hnclone.models.Comment;
import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.User;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommentsDatabaseTest extends BaseDatabaseTest {

    @Before
    public void setUp() throws Exception {
        init();
    }

    @Test
    public void test1() throws Exception {
        UsersDatabase udb = new UsersDatabaseImpl(dataSource);
        PostsDatabase pdb = new PostsDatabaseImpl(dataSource);
        CommentsDatabase cdb = new CommentsDatabaseImpl(dataSource);

        User user1 = udb.create("Username 1", "Password 1", "");
        assertThat(user1).isNotNull();
        User user2 = udb.create("Username 2", "Password 2", "");
        assertThat(user2).isNotNull();
        User user3 = udb.create("Username 3", "Password 3", "");
        assertThat(user3).isNotNull();
        User user4 = udb.create("Username 4", "Password 4", "");
        assertThat(user4).isNotNull();

        Post post1 = pdb.create(user1.getId(), "Post Title", "Post Text", "Post URL");
        assertThat(post1).isNotNull();

        assertThat(cdb.readComments(user1.getId(), post1.getId()).toList().blockingGet()).isEmpty();

        Comment comment1 = cdb.create(user3.getId(), post1.getId(), "Comment 1");
        assertThat(comment1).isNotNull();

        Comment comment2 = cdb.create(user2.getId(), post1.getId(), comment1.getId(), "Comment 2");
        assertThat(comment2).isNotNull();
        Comment comment3 = cdb.create(user3.getId(), post1.getId(), comment2.getId(), "Comment 3");
        assertThat(comment3).isNotNull();
        Comment comment4 = cdb.create(user2.getId(), post1.getId(), comment3.getId(), "Comment 4");
        assertThat(comment4).isNotNull();
        Comment comment5 = cdb.create(user1.getId(), post1.getId(), comment1.getId(), "Comment 5");
        assertThat(comment5).isNotNull();
        assertThat(cdb.incrementScore(user1.getId(), post1.getId(), comment5.getId())).isEqualTo(1);
        Comment comment6 = cdb.create(user3.getId(), post1.getId(), comment5.getId(), "Comment 6");
        assertThat(comment6).isNotNull();
        Comment comment7 = cdb.create(user1.getId(), post1.getId(), comment6.getId(), "Comment 7");
        assertThat(comment7).isNotNull();
        Comment comment8 = cdb.create(user4.getId(), post1.getId(), comment5.getId(), "Comment 8");
        assertThat(comment8).isNotNull();
        assertThat(cdb.incrementScore(user4.getId(), post1.getId(), comment8.getId())).isEqualTo(1);
        Comment comment9 = cdb.create(user1.getId(), post1.getId(), comment8.getId(), "Comment 9");
        assertThat(comment9).isNotNull();

        Comment comment10 = cdb.create(user1.getId(), post1.getId(), "Comment 10");
        assertThat(comment10).isNotNull();
        assertThat(cdb.incrementScore(user1.getId(), post1.getId(), comment10.getId())).isEqualTo(1);
        Comment comment11 = cdb.create(user2.getId(), post1.getId(), comment10.getId(), "Comment 11");
        assertThat(comment11).isNotNull();
        Comment comment12 = cdb.create(user1.getId(), post1.getId(), comment11.getId(), "Comment 12");
        assertThat(comment12).isNotNull();
        Comment comment13 = cdb.create(user2.getId(), post1.getId(), comment12.getId(), "Comment 13");
        assertThat(comment13).isNotNull();
        Comment comment14 = cdb.create(user3.getId(), post1.getId(), comment11.getId(), "Comment 14");
        assertThat(comment14).isNotNull();
        assertThat(cdb.decrementScore(user3.getId(), post1.getId(), comment14.getId())).isEqualTo(-1);
        Comment comment15 = cdb.create(user3.getId(), post1.getId(), comment10.getId(), "Comment 15");
        assertThat(comment15).isNotNull();
        assertThat(cdb.decrementScore(user3.getId(), post1.getId(), comment15.getId())).isEqualTo(-1);
        Comment comment16 = cdb.create(user4.getId(), post1.getId(), comment11.getId(), "Comment 16");
        assertThat(comment16).isNotNull();
        Comment comment17 = cdb.create(user1.getId(), post1.getId(), comment16.getId(), "Comment 17");
        assertThat(comment17).isNotNull();
        Comment comment18 = cdb.create(user2.getId(), post1.getId(), "Comment 18");
        assertThat(comment18).isNotNull();
        assertThat(cdb.decrementScore(user2.getId(), post1.getId(), comment18.getId())).isEqualTo(-1);

        List<Integer> commentsList1 = cdb.readComments(user1.getId(), post1.getId(), comment1.getId())
                .map(Comment::getId)
                .toList()
                .blockingGet();
        assertThat(commentsList1).containsExactly(1, 5, 8, 9, 6, 7, 2, 3, 4).inOrder();

        List<Integer> commentsList2 = cdb.readComments(user1.getId(), post1.getId())
                .map(Comment::getId)
                .toList()
                .blockingGet();
        assertThat(commentsList2).containsExactly(10, 11, 12, 13, 16, 17, 14, 15, 1, 5, 8, 9, 6, 7, 2, 3, 4, 18).inOrder();
    }

}
