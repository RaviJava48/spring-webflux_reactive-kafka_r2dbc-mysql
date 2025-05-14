CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(255) PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_date DATE NOT NULL
);