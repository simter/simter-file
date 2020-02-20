create table st_attachment (
  id        varchar(36)  primary key,
  path      varchar(255) not null,
  name      varchar(255) not null,
  type      varchar(10)  not null,
  size      integer      not null,
  create_on timestamp with time zone not null,
  creator   varchar(255) not null,
  modify_on timestamp with time zone not null,
  modifier  varchar(255) not null,
  puid      varchar(36),
  upper_id  varchar(36)
);
comment on table st_attachment            is 'The meta information of the upload file';
comment on column st_attachment.id        is 'UUID';
comment on column st_attachment.path      is 'The relative path that store the actual physical file';
comment on column st_attachment.name      is 'File name without extension';
comment on column st_attachment.type      is 'If it is a file, the type is file extension without dot symbol and if it is a folder, the type is ":d"';
comment on column st_attachment.size      is 'The byte unit file length';
comment on column st_attachment.create_on is 'Created time';
comment on column st_attachment.creator   is 'The account do the created';
comment on column st_attachment.modify_on is 'Last modify time';
comment on column st_attachment.modifier  is 'The account do the last modify';
comment on column st_attachment.puid      is 'The unique id of the parent module';
comment on column st_attachment.upper_id  is 'The upperId of the parent module';