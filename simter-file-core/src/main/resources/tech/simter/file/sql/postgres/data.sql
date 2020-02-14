/**
 * Data initialize script.
 * @author RJ
 * @author zh
 */
-- module version
--insert into st_kv (key, value) values ('module-version-simter-file', '${project.version}');
-- create the top node
insert into st_attachment (id, path, name, type, size, create_on, creator, modify_on, modifier, puid, upper_id)
  values ('EMPTY', 'EMPTY', 'EMPTY', ':d', 0, now(), 'script', now(), 'script', '', null);