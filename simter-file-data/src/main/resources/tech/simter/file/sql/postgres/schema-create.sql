/**
 * Create table script.
 * @author RJ
 */
create table st_attachment (
  id   varchar(36) primary key,
  path varchar(255) not null,
  name varchar(255) not null,
  ext varchar(10) not null,
  size integer not null,
  uploadOn timestampz not null,
  uploader varchar(255) not null,
  puid varchar(36) not null default '0',
  subgroup integer not null default 0
);
comment on table st_attachment is 'The meta information of the upload file';
comment on column st_attachment.id is 'UUID';
comment on column st_attachment.path is 'The relative path that store the actual physical file';
comment on column st_attachment.name is 'File name without extension';
comment on column st_attachment.ext is 'File extension without dot symbol';
comment on column st_attachment.size is 'The byte unit file length';
comment on column st_attachment.uploadOn is 'Upload time';
comment on column st_attachment.uploader is 'The account do the upload';
comment on column st_attachment.puid is 'The unique id of the parent module';
comment on column st_attachment.subgroup is 'The subgroup of the parent module';