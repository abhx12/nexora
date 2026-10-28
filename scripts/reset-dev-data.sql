-- Reset script for Local Development Environment (nexora_db)
-- Truncates all business transaction tables back to an empty, fresh state.
-- Preserves system settings, schema, roles, permissions, and the owner user.

DO $$
BEGIN
    TRUNCATE TABLE 
      parties, party_roles, party_phone_numbers,
      inventory_balances, material_movements,
      purchases, material_receipts, supplier_returns,
      customer_orders, order_items, customer_returns,
      financial_accounts, financial_transactions, payments, expenses, loans, chits,
      account_transfers, opening_obligations, customer_payment_allocations,
      supplier_payment_allocations, idempotency_keys, audit_log,
      production_batches, production_order_items,
      outsourcing_jobs, outsourcing_order_items, manufacturer_payment_allocations,
      work_records, worker_payment_allocations, notifications, documents,
      business_number_sequences, refresh_tokens CASCADE;

    -- Reset sequence counters if sequences exist
    ALTER SEQUENCE IF EXISTS business_number_seq RESTART WITH 1;
END $$;
