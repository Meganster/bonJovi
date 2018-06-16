CREATE INDEX New_Posts
  ON Post (thread, parent, path, id);

CREATE INDEX Post_threadID_path_id
  ON Post (thread, path, id);

CREATE INDEX Post_threadID_created_id
  ON Post (thread, created, id);

CREATE INDEX Post_patent_threadID_id
  ON Post (parent, thread, id);

CREATE INDEX Thread_forum_created
  ON Thread (forum, created);

CREATE UNIQUE INDEX Vote_user_thread
  ON UserVoteForThreads (user_id, thread_id);

CREATE INDEX Post_threadID_path
  ON Post (thread, (path[1]));

CREATE UNIQUE INDEX Forum_slug_id
  ON Forum (slug, id);

CREATE UNIQUE INDEX Thread_slug_id
  ON Thread (slug, id);

