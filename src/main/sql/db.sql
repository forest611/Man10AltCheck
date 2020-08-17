create table user_list
(
	id int auto_increment,
	player varchar(16) null,
	uuid varchar(36) null,
	address varchar(15) null,
	constraint user_list_pk
		primary key (id)
);

create index user_list_uuid_player_address_index
	on user_list (uuid, player, address);

