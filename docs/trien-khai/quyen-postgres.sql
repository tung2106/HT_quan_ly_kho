-- Chạy bằng chủ schema sau khi Flyway đã tạo bảng. Role mới chưa có mật khẩu.
-- Đặt mật khẩu bằng lệnh tương tác \password trong psql trước khi sử dụng.
BEGIN;
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kho_app') THEN
        CREATE ROLE kho_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'kho_backup') THEN
        CREATE ROLE kho_backup LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    END IF;
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO kho_app, kho_backup', current_database());
END $$;
GRANT USAGE ON SCHEMA public TO kho_app, kho_backup;
GRANT SELECT ON public.v_stock TO kho_app;
GRANT SELECT, INSERT, UPDATE ON public.app_user, public.product, public.batch,
    public.supplier, public.purchase_order, public.outbound_order TO kho_app;
GRANT SELECT, INSERT ON public.purchase_item, public.outbound_item, public.stock_transaction,
    public.cash_payment TO kho_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.category, public.unit_conversion TO kho_app;
GRANT USAGE ON SEQUENCE public.app_user_id_seq, public.category_id_seq, public.product_id_seq,
    public.batch_id_seq, public.supplier_id_seq, public.purchase_order_id_seq,
    public.purchase_item_id_seq, public.outbound_order_id_seq, public.outbound_item_id_seq,
    public.stock_transaction_id_seq, public.cash_payment_id_seq, public.unit_conversion_id_seq,
    public.purchase_lot_code_seq TO kho_app;
DO $$
BEGIN
    IF to_regclass('public.login_rate_limit') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.login_rate_limit TO kho_app;
    END IF;
END $$;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO kho_backup;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO kho_backup;
-- Chạy script bằng đúng role chạy migration để quyền đọc theo các bảng mới.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO kho_backup;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO kho_backup;
COMMIT;
