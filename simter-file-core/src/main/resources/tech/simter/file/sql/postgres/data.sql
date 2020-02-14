-- create the top node
insert into st_attachment (id, path, name, type, size, create_on, creator, modify_on, modifier, puid, upper_id)
  values ('EMPTY', 'EMPTY', 'EMPTY', ':d', 0, now(), 'script', now(), 'script', '', null);