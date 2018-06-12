-- DROP TABLE "User" CASCADE;
-- DROP TABLE Forum CASCADE;
-- DROP TABLE Thread CASCADE;
-- DROP TABLE Post CASCADE;
-- DROP TABLE UserVoteForThreads CASCADE;
-- DROP TABLE schema_version CASCADE;
-- DROP TABLE ForumUsers CASCADE;

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE "User" (
  id SERIAL PRIMARY KEY,
  email CITEXT COLLATE "ucs_basic" UNIQUE NOT NULL,
  fullname VARCHAR(256) NOT NULL,
  nickname CITEXT COLLATE "ucs_basic" UNIQUE NOT NULL,
  about TEXT
);

CREATE TABLE Forum (
  id SERIAL PRIMARY KEY,
  title VARCHAR(256),
  posts INTEGER DEFAULT 0,
  threads INTEGER DEFAULT 0,
  slug CITEXT COLLATE "ucs_basic" NOT NULL UNIQUE,
  "user" CITEXT COLLATE "ucs_basic" NOT NULL REFERENCES "User" (nickname) ON DELETE CASCADE
);

CREATE TABLE Thread (
  id SERIAL PRIMARY KEY,
  title VARCHAR(256),
  author CITEXT COLLATE "ucs_basic" NOT NULL REFERENCES "User" (nickname) ON DELETE CASCADE,
  forum CITEXT NOT NULL REFERENCES Forum (slug) ON DELETE CASCADE,
  message TEXT,
  votes INTEGER DEFAULT 0,
  slug CITEXT UNIQUE DEFAULT NULL,
  created TIMESTAMPTZ DEFAULT now(),
  forum_id INTEGER -- нужно для инкрементов
);

CREATE TABLE Post (
  id SERIAL PRIMARY KEY,
  created TIMESTAMPTZ DEFAULT now(),
  forum CITEXT REFERENCES Forum (slug) ON DELETE CASCADE,
  thread INTEGER REFERENCES Thread (id) ON DELETE CASCADE,
  author CITEXT COLLATE "ucs_basic" REFERENCES "User" (nickname) ON DELETE CASCADE,
  parent INTEGER DEFAULT 0,
  message TEXT,
  isEdited BOOLEAN DEFAULT FALSE,
  forum_id  INTEGER,  -- нужно для инкрементов
  path INT []
);

CREATE TABLE UserVoteForThreads (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES "User" (id) ON DELETE CASCADE,
  thread_id INTEGER REFERENCES Thread (id) ON DELETE CASCADE,
  vote INTEGER DEFAULT 0,
  UNIQUE (user_id, thread_id)
);

CREATE TABLE ForumUsers (
  id  SERIAL PRIMARY KEY,
  email CITEXT COLLATE "ucs_basic" UNIQUE NOT NULL,
  fullname VARCHAR(256) NOT NULL,
  nickname CITEXT COLLATE "ucs_basic" UNIQUE NOT NULL,
  about TEXT,
  forum_id INTEGER,
  UNIQUE (forum_id, nickname)
);

-- increment thread count --
CREATE OR REPLACE FUNCTION increment_forum_threads_procedure()
  RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  new.forum_id = (SELECT id
                 FROM Forum
                 WHERE Forum.slug=new.forum);
  UPDATE Forum
  SET threads = threads + 1
  WHERE forum.id = new.forum_id;
  INSERT INTO ForumUsers(nickname, fullname, email, about, forum_id)
    (SELECT
       new.author,
       U.fullname,
       U.email,
       U.about,
       new.forum_id
     FROM "User" U
     WHERE new.author=U.nickname)
  ON CONFLICT DO NOTHING;
  RETURN new;
END;
$$;

DROP TRIGGER IF EXISTS increment_forum_threads
ON Thread;

CREATE TRIGGER increment_forum_threads
BEFORE INSERT
  ON Thread
FOR EACH ROW
EXECUTE PROCEDURE increment_forum_threads_procedure();
-- --- --

-- increment post count --
CREATE OR REPLACE FUNCTION increment_forum_posts_procedure()
  RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE Forum
  SET posts = posts + 1
  WHERE forum.id = new.forum_id;
  INSERT INTO ForumUsers(nickname, fullname, email, about, forum_id)
    (SELECT
       new.author,
       U.fullname,
       U.email,
       U.about,
       new.forum_id
     FROM "User" U
     WHERE new.author=U.nickname)
  ON CONFLICT DO NOTHING;
  RETURN new;
END;
$$;

DROP TRIGGER IF EXISTS increment_forum_posts
ON Post;

CREATE TRIGGER increment_forum_posts
BEFORE INSERT
  ON Post
FOR EACH ROW
EXECUTE PROCEDURE increment_forum_posts_procedure();
-- --- --

-- Vote part --
CREATE OR REPLACE FUNCTION vote()
  RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF (TG_OP = 'INSERT')
  THEN
    UPDATE Thread
    SET votes = votes + new.vote
    WHERE Thread.id = new.thread_id;
    RETURN new;
  ELSE
    IF new.vote != old.vote
    THEN
      UPDATE Thread
      SET votes = votes + (new.vote * 2)
      WHERE Thread.id = new.thread_id;
      RETURN new;
    END IF;
    RETURN new;
  END IF;
END;
$$;

DROP TRIGGER IF EXISTS vote_trigger
ON UserVoteForThreads;

CREATE TRIGGER vote_trigger
  AFTER INSERT OR UPDATE
  ON UserVoteForThreads
  FOR EACH ROW
EXECUTE PROCEDURE vote();
-- --- --

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

