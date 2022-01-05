CREATE TABLE IF NOT EXISTS blog (
    id              int,
    tstamp          int,           -- in Unix time
    title           varchar(128),
    author          varchar(64),
    contents        varchar,
    org_hash        varchar(32)
);

CREATE TABLE IF NOT EXISTS tag (
    blog_id         int,
    tag             varchar
);
