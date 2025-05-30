create table customers
(
    ID      int auto_increment
        primary key,
    name    varchar(50)  not null,
    address varchar(100) not null,
    balance double       null
);

create table deliverers
(
    ID             int auto_increment
        primary key,
    name           varchar(50)   not null,
    nbrRetards     int default 0 null,
    minutesRetards int default 0 null
);

create table ingredients
(
    id   int auto_increment
        primary key,
    name varchar(50) null
);

create table ingredients_products
(
    ID             int auto_increment
        primary key,
    ingredients_id int not null,
    product_id     int not null,
    constraint products_ingredients_id
        foreign key (ingredients_id) references ingredients (id)
);

create index ingredient_products_id
    on ingredients_products (product_id);

create table pizza_sizes
(
    id           int auto_increment
        primary key,
    size         varchar(50) not null,
    price_adjust float       not null,
    constraint chk_size_values
        check (`size` in ('naine', 'humaine', 'ogresse'))
);

create table pizzas
(
    id      int auto_increment
        primary key,
    name    varchar(50) not null,
    price   float       not null,
    size_id int         not null
);

create index products_size_id
    on pizzas (size_id);

create table subscription
(
    ID    int auto_increment
        primary key,
    name  varchar(50) not null,
    price double      not null
);

create table customers_subscriptions
(
    customer_id     int not null,
    subscription_id int not null,
    constraint customers_subscriptions_customer_id
        foreign key (customer_id) references customers (ID),
    constraint customers_subscriptions_subscription_id
        foreign key (subscription_id) references subscription (ID)
);

create table vehicles
(
    ID               int auto_increment
        primary key,
    type             varchar(50)                              not null,
    uses             int        default 0                     null,
    taken            tinyint(1) default 0                     null,
    acquisition_date datetime   default '2004-09-10 00:00:00' null,
    nbrRetard        int        default 0                     null
);

create table orders
(
    ID            int auto_increment
        primary key,
    customer_id   int                  null,
    deliverer_id  int                  null,
    vehicle_id    int                  null,
    order_price   float                null,
    order_time    datetime             not null,
    delivery_time datetime             not null,
    is_free       tinyint(1) default 0 null,
    delay_minutes int as (timestampdiff(MINUTE, `order_time`, `delivery_time`)) stored,
    constraint orders_customer_id
        foreign key (customer_id) references customers (ID),
    constraint orders_deliverer_id
        foreign key (deliverer_id) references deliverers (ID),
    constraint orders_vehicle_id
        foreign key (vehicle_id) references vehicles (ID)
);

create definer = root@`%` trigger trg_free_pizza
    before insert
    on orders
    for each row
BEGIN
    DECLARE total_orders INT;

    SELECT COUNT(*) INTO total_orders
    FROM orders
    WHERE customer_id = NEW.customer_id AND is_free = FALSE;
    
    IF total_orders >= 9 OR TIMESTAMPDIFF(MINUTE, NEW.order_time, NEW.delivery_time) > 30 THEN
        SET NEW.is_free = TRUE;
        SET NEW.order_price = 0;
    END IF;
END;

create table orders_products
(
    ID         int auto_increment
        primary key,
    order_id   int null,
    product_id int null,
    constraint orders_products_order_id
        foreign key (order_id) references orders (ID),
    constraint orders_products_product_id
        foreign key (product_id) references pizzas (id)
);

