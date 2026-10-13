DO $$
DECLARE
    legacy_constraint RECORD;
BEGIN
    IF to_regclass('public.categorias') IS NULL THEN
        RETURN;
    END IF;

    FOR legacy_constraint IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_schema = tc.constraint_schema
         AND ccu.constraint_name = tc.constraint_name
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'categorias'
          AND tc.constraint_type = 'UNIQUE'
          AND ccu.column_name = 'nome'
          AND tc.constraint_name <> 'uk_categoria_nome_escopo'
    LOOP
        EXECUTE format('ALTER TABLE categorias DROP CONSTRAINT IF EXISTS %I', legacy_constraint.constraint_name);
    END LOOP;

    UPDATE categorias
    SET escopo = 'PESSOAL'
    WHERE escopo IS NULL
      AND lower(nome) IN ('despesas essenciais', 'lazer e estilo de vida');

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'categorias'::regclass
          AND conname = 'uk_categoria_nome_escopo'
    ) THEN
        ALTER TABLE categorias
            ADD CONSTRAINT uk_categoria_nome_escopo UNIQUE (nome, escopo);
    END IF;
END
$$;
