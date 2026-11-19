-- Payment and lifecycle details for checkout. Card numbers are never stored:
-- only the method and a simulated gateway reference.
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NULL;
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP(6) NULL;
ALTER TABLE orders ADD COLUMN cancelled_at TIMESTAMP(6) NULL;
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
