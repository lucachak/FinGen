DO $$
BEGIN
    IF to_regclass('public.configuracoes_financeiras') IS NOT NULL THEN
        ALTER TABLE configuracoes_financeiras
            ADD COLUMN IF NOT EXISTS possui_negocio BOOLEAN;

        UPDATE configuracoes_financeiras
        SET possui_negocio = FALSE
        WHERE possui_negocio IS NULL;

        ALTER TABLE configuracoes_financeiras
            ALTER COLUMN possui_negocio SET DEFAULT FALSE,
            ALTER COLUMN possui_negocio SET NOT NULL;

        ALTER TABLE configuracoes_financeiras
            ADD COLUMN IF NOT EXISTS nome_negocio VARCHAR(255);
    END IF;
END
$$;
