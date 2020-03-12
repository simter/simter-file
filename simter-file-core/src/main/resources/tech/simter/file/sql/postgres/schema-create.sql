create table st_file_store (
  id        varchar(36)  primary key,
  module    varchar(255) not null,
  name      varchar(100) not null,
  type      varchar(10)  not null,
  size      bigint       not null,
  path      varchar(255) not null,
  creator   varchar(50)  not null,
  create_on timestamp with time zone not null,
  modifier  varchar(50)  not null,
  modify_on timestamp with time zone not null
);
comment on table st_file_store            is 'file information';
comment on column st_file_store.module    is 'business module identity, must start end and separate with "/" symbol';
comment on column st_file_store.name      is 'File name without extension';
comment on column st_file_store.type      is 'file extension without dot symbol';
comment on column st_file_store.size      is 'byte unit file length';
comment on column st_file_store.path      is 'relative path that store the physical file';
comment on column st_file_store.creator   is 'creator identity';
comment on column st_file_store.create_on is 'created datetime';
comment on column st_file_store.modifier  is 'last modifier identity';
comment on column st_file_store.modify_on is 'last modified datetime';
