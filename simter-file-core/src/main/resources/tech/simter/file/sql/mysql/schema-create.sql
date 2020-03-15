create table st_file (
  id        varchar(36)  primary key,
  module    varchar(255) not null comment 'business module identity, must start end and separate with "/" symbol',
  name      varchar(100) not null comment 'File name without extension',
  type      varchar(10)  not null comment 'file extension without dot symbol',
  size      bigint       not null comment 'byte unit file length',
  path      varchar(255) not null comment 'relative path that store the physical file',
  creator   varchar(50)  not null comment 'creator identity',
  create_on timestamp    not null comment 'created datetime',
  modifier  varchar(50)  not null comment 'last modifier identity',
  modify_on timestamp    not null comment 'last modified datetime'
) comment = 'file information';
