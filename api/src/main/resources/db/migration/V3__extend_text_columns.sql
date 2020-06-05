alter table pages
    modify text MEDIUMTEXT CHARSET utf8mb4 null;
alter table documents
    modify content MEDIUMTEXT CHARSET utf8mb4 null;
