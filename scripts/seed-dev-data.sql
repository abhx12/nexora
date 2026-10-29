-- Seed script for Local Development Environment (nexora_db on port 5431)
-- Provides rich, realistic silk trading & weaving data for development and testing.

DO $$
DECLARE
    v_owner_id UUID;
    v_pty_balaji UUID := '11111111-1111-1111-1111-111111111101';
    v_pty_murugan UUID := '11111111-1111-1111-1111-111111111102';
    v_pty_lakshmi UUID := '11111111-1111-1111-1111-111111111103';
    v_pty_kanchi UUID := '11111111-1111-1111-1111-111111111104';
    v_pty_venkat UUID := '11111111-1111-1111-1111-111111111105';
    v_pty_ramu UUID := '11111111-1111-1111-1111-111111111106';
    v_pty_kumar UUID := '11111111-1111-1111-1111-111111111107';
    v_pty_annapoorna UUID := '11111111-1111-1111-1111-111111111108';
    v_pty_cub UUID := '11111111-1111-1111-1111-111111111109';
    v_pty_sundaram UUID := '11111111-1111-1111-1111-111111111110';

    v_acc_cash UUID := '22222222-2222-2222-2222-222222222201';
    v_acc_cub UUID := '22222222-2222-2222-2222-222222222202';
    v_acc_sbi UUID := '22222222-2222-2222-2222-222222222203';

    v_pur_1 UUID := '33333333-3333-3333-3333-333333333301';
    v_rec_1 UUID := '44444444-4444-4444-4444-444444444401';
    v_ord_1 UUID := '55555555-5555-5555-5555-555555555501';
    v_ord_2 UUID := '55555555-5555-5555-5555-555555555502';
    v_loan_1 UUID := '66666666-6666-6666-6666-666666666601';
BEGIN
    -- 1. Ensure Owner exists
    SELECT id INTO v_owner_id FROM users WHERE username = 'owner' LIMIT 1;
    IF v_owner_id IS NULL THEN
        INSERT INTO users (id, username, password_hash, display_name, status, failed_login_attempts, created_at, updated_at, version)
        VALUES (gen_random_uuid(), 'owner', '$2a$10$wKqKj0K9N0eWfF4D5pP9h.Y5m8S7G8tJ6vK8q7Z0yH0kP2eF3gJ6u', 'Owner', 'ACTIVE', 0, NOW(), NOW(), 0)
        RETURNING id INTO v_owner_id;

        INSERT INTO user_roles (user_id, role_id)
        SELECT v_owner_id, id FROM roles WHERE name = 'OWNER';
    END IF;

    -- 2. Clean business tables first
    TRUNCATE TABLE 
      parties, party_roles, party_phone_numbers,
      inventory_balances, material_movements,
      purchases, material_receipts, supplier_returns,
      customer_orders, order_items,
      financial_accounts, financial_transactions, payments, expenses, loans, chits,
      account_transfers, opening_obligations, customer_payment_allocations,
      supplier_payment_allocations, idempotency_keys, audit_log,
      production_batches, production_order_items,
      outsourcing_jobs, outsourcing_order_items, manufacturer_payment_allocations,
      work_records, worker_payment_allocations, notifications, documents,
      business_number_sequences, refresh_tokens, inventory_thresholds CASCADE;

    -- 3. Insert Parties
    INSERT INTO parties (id, party_code, name, address_line, area, city, district, state, pincode, status, default_credit_days, customer_credit_days, created_at, updated_at, created_by, version) VALUES
    (v_pty_balaji, 'PTY-2026-0001', 'Sri Balaji Silk Traders', '14 Big Weaver Street', 'Gandhi Nagar', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631501', 'ACTIVE', 30, 30, NOW(), NOW(), v_owner_id, 0),
    (v_pty_murugan, 'PTY-2026-0002', 'Murugan Raw Silk Enterprise', '108 Silk Market Road', 'Market Ward', 'Salem', 'Salem', 'Tamil Nadu', '636001', 'ACTIVE', 15, 15, NOW(), NOW(), v_owner_id, 0),
    (v_pty_lakshmi, 'PTY-2026-0003', 'Sri Lakshmi Weaving Works', '22 Loom Colony', 'Pillayar Palayam', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631502', 'ACTIVE', 20, 20, NOW(), NOW(), v_owner_id, 0),
    (v_pty_kanchi, 'PTY-2026-0004', 'Kanchi Silks Emporium', '55 M.G. Road', 'Central Bazaar', 'Chennai', 'Chennai', 'Tamil Nadu', '600017', 'ACTIVE', 30, 45, NOW(), NOW(), v_owner_id, 0),
    (v_pty_venkat, 'PTY-2026-0005', 'Venkateshwara Handlooms', '8 North Mada Street', 'Sannathi', 'Arani', 'Tiruvannamalai', 'Tamil Nadu', '632301', 'ACTIVE', 15, 15, NOW(), NOW(), v_owner_id, 0),
    (v_pty_ramu, 'PTY-2026-0006', 'Ramu (Rolling Operator)', '3 Cross Street', 'Weaver Nagar', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631501', 'ACTIVE', NULL, NULL, NOW(), NOW(), v_owner_id, 0),
    (v_pty_kumar, 'PTY-2026-0007', 'Kumar (Warping Master)', '17 Sivan Temple Street', 'Weaver Nagar', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631501', 'ACTIVE', NULL, NULL, NOW(), NOW(), v_owner_id, 0),
    (v_pty_annapoorna, 'PTY-2026-0008', 'Annapoorna Warping Mills', '92 Industrial Estate', 'Sipcot', 'Ranipet', 'Ranipet', 'Tamil Nadu', '632403', 'ACTIVE', 10, 10, NOW(), NOW(), v_owner_id, 0),
    (v_pty_cub, 'PTY-2026-0009', 'City Union Bank', 'Main Branch', 'Nellukkara Street', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631502', 'ACTIVE', NULL, NULL, NOW(), NOW(), v_owner_id, 0),
    (v_pty_sundaram, 'PTY-2026-0010', 'Shri R. Sundaram Finance', '74 Gandhi Road', 'Commercial Area', 'Kanchipuram', 'Kanchipuram', 'Tamil Nadu', '631501', 'ACTIVE', NULL, NULL, NOW(), NOW(), v_owner_id, 0);

    -- 4. Party Roles
    INSERT INTO party_roles (party_id, role) VALUES
    (v_pty_balaji, 'CUSTOMER'),
    (v_pty_murugan, 'SUPPLIER'),
    (v_pty_lakshmi, 'CUSTOMER'),
    (v_pty_lakshmi, 'MANUFACTURER'),
    (v_pty_kanchi, 'CUSTOMER'),
    (v_pty_venkat, 'MANUFACTURER'),
    (v_pty_ramu, 'WORKER'),
    (v_pty_kumar, 'WORKER'),
    (v_pty_annapoorna, 'MANUFACTURER'),
    (v_pty_cub, 'LENDER'),
    (v_pty_sundaram, 'LENDER');

    -- 5. Party Phone Numbers
    INSERT INTO party_phone_numbers (id, party_id, phone_number, label, is_primary) VALUES
    (gen_random_uuid(), v_pty_balaji, '+91 98401 23456', 'Office', true),
    (gen_random_uuid(), v_pty_murugan, '+91 94432 11223', 'Shop', true),
    (gen_random_uuid(), v_pty_lakshmi, '+91 98840 55667', 'Master', true),
    (gen_random_uuid(), v_pty_kanchi, '+91 98410 99887', 'Billing', true),
    (gen_random_uuid(), v_pty_venkat, '+91 97890 33445', 'Weaving', true),
    (gen_random_uuid(), v_pty_ramu, '+91 95001 77889', 'Mobile', true),
    (gen_random_uuid(), v_pty_kumar, '+91 98421 44556', 'Mobile', true),
    (gen_random_uuid(), v_pty_annapoorna, '+91 94440 66778', 'Mill', true),
    (gen_random_uuid(), v_pty_sundaram, '+91 94433 88990', 'Manager', true);

    -- 6. Financial Accounts
    INSERT INTO financial_accounts (id, account_name, account_type, bank_name, masked_account_number, status, created_at, updated_at, created_by, version) VALUES
    (v_acc_cash, 'Main Cash Drawer', 'CASH', NULL, NULL, 'ACTIVE', NOW(), NOW(), v_owner_id, 0),
    (v_acc_cub, 'CUB Current Account', 'BANK', 'City Union Bank', 'XXXXXX1048', 'ACTIVE', NOW(), NOW(), v_owner_id, 0),
    (v_acc_sbi, 'SBI Business Account', 'BANK', 'State Bank of India', 'XXXXXX5521', 'ACTIVE', NOW(), NOW(), v_owner_id, 0);

    -- 7. Initial Cash/Bank balances via financial transactions
    INSERT INTO financial_transactions (id, financial_account_id, transaction_date, transaction_type, direction, amount, reference_number, description, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), v_acc_cash, CURRENT_DATE, 'OPENING_BALANCE', 'IN', 45000.00, 'OB-CASH-01', 'Opening Cash in Hand', NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), v_acc_cub, CURRENT_DATE, 'OPENING_BALANCE', 'IN', 285000.00, 'OB-CUB-01', 'Opening Bank Balance CUB', NOW(), NOW(), v_owner_id, 0);

    -- 8. Inventory Balances
    INSERT INTO inventory_balances (id, product_type, location, quantity_kg, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), 'RAW_SILK', 'RAW_STOCK', 55.500, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'SILK_WARP', 'FINISHED_STOCK', 32.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'KORA_WARP', 'FINISHED_STOCK', 18.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'SILK_WARP', 'INTERNAL_WIP', 15.000, NOW(), NOW(), v_owner_id, 0);

    -- 8b. Inventory Thresholds (Minimum stock safety levels in kg)
    INSERT INTO inventory_thresholds (id, product_type, location, min_threshold_kg, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), 'RAW_SILK', 'RAW_STOCK', 50.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'VUDA_WARP', 'RAW_STOCK', 20.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'SILK_WARP', 'INTERNAL_WIP', 30.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'KORA_WARP', 'INTERNAL_WIP', 20.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'SILK_WARP', 'EXTERNAL_WIP', 20.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'KORA_WARP', 'EXTERNAL_WIP', 10.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'SILK_WARP', 'FINISHED_STOCK', 25.000, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'KORA_WARP', 'FINISHED_STOCK', 15.000, NOW(), NOW(), v_owner_id, 0);

    -- 9. Purchase record
    INSERT INTO purchases (id, purchase_number, supplier_id, product_type, purchase_date, ordered_weight_kg, rate_per_kg, payment_type, credit_days, due_date, bill_reference, status, notes, created_at, updated_at, created_by, version) VALUES
    (v_pur_1, 'PUR-2026-0001', v_pty_murugan, 'RAW_SILK', CURRENT_DATE - 3, 50.000, 4500.00, 'CREDIT', 15, CURRENT_DATE + 12, 'INV-MR-8821', 'FULLY_RECEIVED', 'Good quality Malda lot', NOW(), NOW(), v_owner_id, 0);

    INSERT INTO material_receipts (id, receipt_number, purchase_id, receipt_date, received_weight_kg, accepted_weight_kg, rejected_weight_kg, notes, created_at, updated_at, created_by, version) VALUES
    (v_rec_1, 'REC-2026-0001', v_pur_1, CURRENT_DATE - 2, 50.000, 50.000, 0.000, 'Weighed on electronic platform scale', NOW(), NOW(), v_owner_id, 0);

    -- 10. Customer Orders
    INSERT INTO customer_orders (id, order_number, customer_id, order_date, required_date, status, notes, created_at, updated_at, created_by, version) VALUES
    (v_ord_1, 'ORD-2026-0001', v_pty_balaji, CURRENT_DATE - 5, CURRENT_DATE + 10, 'IN_PROGRESS', 'Zari border traditional bridal sarees', NOW(), NOW(), v_owner_id, 0),
    (v_ord_2, 'ORD-2026-0002', v_pty_kanchi, CURRENT_DATE - 2, CURRENT_DATE + 15, 'CONFIRMED', 'Kora silk party-wear sarees', NOW(), NOW(), v_owner_id, 0);

    INSERT INTO order_items (id, order_id, line_no, product_type, required_weight_kg, rate_per_kg, amount, warp_specification, status, notes, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), v_ord_1, 1, 'SILK_WARP', 25.000, 5800.00, 145000.00, '120 Reed, 4000 ends, Pure Silk', 'IN_PROGRESS', 'Red with Gold Zari', NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), v_ord_2, 1, 'KORA_WARP', 15.000, 4200.00, 63000.00, '100 Reed Kora Warp', 'PENDING', 'Natural Cream', NOW(), NOW(), v_owner_id, 0);

    -- 11. Loans
    INSERT INTO loans (id, loan_number, party_id, loan_type, principal_amount, repaid_before_go_live, interest_notes, interest_rate_percent, start_date, status, notes, created_at, updated_at, created_by, version) VALUES
    (v_loan_1, 'LN-2026-0001', v_pty_sundaram, 'PRIVATE', 100000.00, 0.00, 'Monthly interest 1.5%', 18.00, CURRENT_DATE - 30, 'ACTIVE', 'Working capital for raw silk procurement', NOW(), NOW(), v_owner_id, 0);

    -- 12. Workforce records
    INSERT INTO work_records (id, worker_id, work_date, work_type, hours, quantity_kg, rate, amount, status, notes, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), v_pty_ramu, CURRENT_DATE - 1, 'ROLLING', 8.00, NULL, 65.00, 520.00, 'RECORDED', 'Rolling work for batch 1', NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), v_pty_kumar, CURRENT_DATE - 1, 'WARPING', NULL, 15.000, 80.00, 1200.00, 'RECORDED', 'Warping master piece-rate', NOW(), NOW(), v_owner_id, 0);

    -- 13. Number sequences
    INSERT INTO business_number_sequences (id, prefix, seq_year, next_value, created_at, updated_at, created_by, version) VALUES
    (gen_random_uuid(), 'PTY', 2026, 11, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'PUR', 2026, 2, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'REC', 2026, 2, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'ORD', 2026, 3, NOW(), NOW(), v_owner_id, 0),
    (gen_random_uuid(), 'LN', 2026, 2, NOW(), NOW(), v_owner_id, 0);

    -- 14. Default System Settings
    UPDATE system_settings SET setting_value = 'Sri Balaji Silk Traders' WHERE category = 'BUSINESS' AND setting_key = 'name';
    UPDATE system_settings SET setting_value = '+91 98401 23456' WHERE category = 'BUSINESS' AND setting_key = 'phone';
    UPDATE system_settings SET setting_value = 'Kanchipuram' WHERE category = 'BUSINESS' AND setting_key = 'city';
END $$;
