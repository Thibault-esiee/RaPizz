
-- ALTER TABLE customers AUTO_INCREMENT = 1;
-- ALTER TABLE subscription AUTO_INCREMENT = 1;
-- ALTER TABLE deliverers AUTO_INCREMENT = 1;
-- ALTER TABLE vehicles AUTO_INCREMENT = 1;
-- ALTER TABLE ingredients AUTO_INCREMENT = 1;
-- ALTER TABLE pizzas AUTO_INCREMENT = 1;
-- ALTER TABLE pizza_sizes AUTO_INCREMENT = 1;
-- ALTER TABLE orders AUTO_INCREMENT = 1;

INSERT INTO customers (name, address, balance) VALUES
('Alice Dupont', '12 rue des Lilas, Paris', 50.00),
('Bruno Martin', '45 avenue des Champs, Lyon', 35.00),
('Carla Noël', '8 impasse du Four, Lille', 10.00),
('David Moreau', '15 rue de la Paix, Paris', 60.00),
('Emma Leroy', '22 avenue Victor Hugo, Marseille', 45.00);

INSERT INTO subscription (name, price) VALUES
('Standard', 10.00),
('Premium', 20.00);

INSERT INTO customers_subscriptions (customer_id, subscription_id) VALUES
(1, 1),
(2, 2),
(3, 1),
(4, 2),
(5, 1);

INSERT INTO pizza_sizes (size, price_adjust) VALUES
('naine', -0.33),
('humaine', 0.00),
('ogresse', 0.33);

INSERT INTO ingredients (name) VALUES
('Fromage'),
('Tomate'),
('Jambon'),
('Champignons'),
('Ananas'),
('Poivrons'),
('Olives'),
('Basilic');

INSERT INTO pizzas (name, price, size_id) VALUES
('Reine', 12.00, 2),
('Hawaïenne', 13.00, 2),
('4 Fromages', 11.00, 2),
('Margherita', 10.00, 2),
('Pepperoni', 14.00, 2),
('Végétarienne', 13.50, 2);

-- Reine (id pizza 1)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 1), -- Fromage
(2, 1), -- Tomate
(3, 1), -- Jambon
(4, 1); -- Champignons

-- Hawaïenne (id pizza 2)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 2), -- Fromage
(2, 2), -- Tomate
(3, 2), -- Jambon
(5, 2); -- Ananas

-- 4 Fromages (id pizza 3)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 3); -- Fromage

-- Margherita (id pizza 4)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 4), -- Fromage
(2, 4), -- Tomate
(8, 4); -- Basilic

-- Pepperoni (id pizza 5)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 5), -- Fromage
(2, 5), -- Tomate
(6, 5); -- Poivrons (à défaut de pepperoni)

-- Végétarienne (id pizza 6)
INSERT INTO ingredients_products (ingredients_id, product_id) VALUES
(1, 6), -- Fromage
(2, 6), -- Tomate
(4, 6), -- Champignons
(6, 6), -- Poivrons
(7, 6), -- Olives
(8, 6); -- Basilic

INSERT INTO vehicles (type, acquisition_date) VALUES
('Moto', '2020-01-01'),
('Voiture', '2021-06-15');

INSERT INTO deliverers (name) VALUES
('Luc'),
('Emma');

INSERT INTO orders (
    customer_id, deliverer_id, vehicle_id, order_price,
    order_time, delivery_time
) VALUES
(1, 1, 1, 12.00, '2025-05-29 18:00:00', '2025-05-29 18:45:00');

INSERT INTO orders (
    customer_id, deliverer_id, vehicle_id, order_price,
    order_time, delivery_time
) VALUES
(2, 2, 2, 13.00, '2025-05-29 19:00:00', '2025-05-29 19:25:00');

INSERT INTO orders (
    customer_id, deliverer_id, vehicle_id, order_price,
    order_time, delivery_time
) VALUES
(3, 1, 1, 11.00, '2025-05-29 20:00:00', '2025-05-29 20:20:00');

INSERT INTO orders_products (order_id, product_id) VALUES
(1, 1),
(2, 2),
(3, 3);