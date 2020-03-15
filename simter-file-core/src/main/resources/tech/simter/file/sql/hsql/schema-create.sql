create table st_file (
  id        varchar(36)  primary key,
  module    varchar(255) not null,
  name      varchar(100) not null,
  type      varchar(10)  not null,
  size      bigint       not null,
  path      varchar(255) not null,
  creator   varchar(50)  not null,
  create_on timestamp    not null,
  modifier  varchar(50)  not null,
  modify_on timestamp    not null
);
comment on table st_file            is 'file information';
comment on column st_file.module    is 'business module identity, must start end and separate with "/" symbol';
comment on column st_file.name      is 'File name without extension';
comment on column st_file.type      is 'file extension without dot symbol';
comment on column st_file.size      is 'byte unit file length';
comment on column st_file.path      is 'relative path that store the physical file';
comment on column st_file.creator   is 'creator identity';
comment on column st_file.create_on is 'created datetime';
comment on column st_file.modifier  is 'last modifier identity';
comment on column st_file.modify_on is 'last modified datetime';
