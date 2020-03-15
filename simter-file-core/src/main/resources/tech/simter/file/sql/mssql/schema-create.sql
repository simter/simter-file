create table st_file (
  id        varchar(36)    not null,
  module    varchar(255)   not null,
  name      varchar(100)   not null,
  type      varchar(10)    not null,
  size      bigint         not null,
  path      varchar(255)   not null,
  creator   varchar(50)    not null,
  create_on datetimeoffset not null,
  modifier  varchar(50)    not null,
  modify_on datetimeoffset not null,
  primary key (id)
);