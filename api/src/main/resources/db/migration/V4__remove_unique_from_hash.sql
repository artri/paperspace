create table binaries_dg_tmp
(
    id           VARCHAR(36)   NOT NULL PRIMARY KEY,
    created_at   DATETIME      NOT NULL,
    storage_path VARCHAR(2048) NOT NULL UNIQUE,
    hash         VARCHAR(32)   NOT NULL,
    mime_type    VARCHAR(255)  not null,
    length       BIGINT,
    state        VARCHAR(255) default 'PROCESSED'
);

insert into binaries_dg_tmp(id, created_at, storage_path, hash, mime_type, length, state)
select id, created_at, storage_path, hash, mime_type, length, state
from binaries;

drop table binaries;

alter table binaries_dg_tmp
    rename to binaries;

