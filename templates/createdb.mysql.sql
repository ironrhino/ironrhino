drop database if exists ironrhino;
create database ironrhino default charset utf8mb4 collate utf8mb4_unicode_ci;
-- ALTER TABLE user MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use ironrhino;

drop function if exists get_dictionary_label;
delimiter $$  
create function get_dictionary_label(items text, value varchar(255)) returns varchar(255)
begin
    declare length integer;
    declare idx integer;
    select json_length(items) into length;
    set idx = 0;
    while idx < length do
        if json_extract(items,concat('$[',idx,'].value')) = value then
                return json_unquote(json_extract(items,concat('$[',idx,'].label')));
        end if;
        set idx = idx+1;
    end while;
    return null;
end $$
delimiter ;


drop procedure if exists get_dictionary;
delimiter $$  
create procedure get_dictionary(in name varchar(255))
begin
    declare items json;
    declare length integer;
    declare idx integer;
    drop temporary table if exists temp_dictionary;
    create temporary table temp_dictionary(label varchar(255),value varchar(255));
    select t.items into items from common_dictionary t where t.name = name;
    select json_length(items) into length;
    set idx = 0;
    while idx < length do
        insert into temp_dictionary(label,value) values(json_unquote(json_extract(items,concat('$[',idx,'].label'))),json_unquote(json_extract(items,concat('$[',idx,'].value'))));
        set idx = idx+1;
    end while;
    select * from temp_dictionary;
end $$
delimiter ;

drop function if exists next_id;
delimiter $$
create function next_id() returns varchar(22) not deterministic
begin
  declare digits char(62) default "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  declare s varchar(22) default "";
  declare n numeric(39) default uuid_short();
  while n > 0 do
    set s = concat(substr(digits, (n mod 62) + 1, 1), s);
    set n = floor(n / 62);
  end while;
  while length(s) < 22 do
    if length(s) = 21 then
        set s = concat(substr(digits, floor(rand() * 8), 1), s);
    else
        set s = concat(substr(digits, floor(rand() * 63), 1), s);
    end if;
  end while;
  return s;
end$$
delimiter ;

drop function if exists next_snowflake_id;
delimiter $$
create function next_snowflake_id(worker_id int) returns bigint(20) not deterministic
begin
    declare epoch bigint(20) default 1556150400000;
    declare worker_id_length int default 8;
    declare sequence_length int default 10;
    declare current_ts bigint(20) default round(unix_timestamp(curtime(4)) * 1000);
    if @sequence > 1023 then
        while @last_ts = current_ts do
            set current_ts = round(unix_timestamp(curtime(4)) * 1000);
        end while;
    end if;
    if @last_ts = current_ts then
        set @sequence = @sequence + 1;
    else
        set @sequence = 0;
    end if;
    set @last_ts = current_ts;
return (current_ts - epoch) << (sequence_length + worker_id_length) | (worker_id << sequence_length) | @sequence;
end$$
delimiter ;

