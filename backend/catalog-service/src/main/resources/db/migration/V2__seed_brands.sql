INSERT INTO brands (id, name, slug, created_at) VALUES
    -- Acessórios
    (gen_random_uuid(), 'Recaro',      'recaro',      NOW()),
    (gen_random_uuid(), 'Thule',       'thule',       NOW()),

    -- Escapamento
    (gen_random_uuid(), 'Luzian',      'luzian',      NOW()),
    (gen_random_uuid(), 'Akrapovič',   'akrapovic',   NOW()),

    -- Freios
    (gen_random_uuid(), 'Brembo',      'brembo',      NOW()),
    (gen_random_uuid(), 'Ferodo',      'ferodo',      NOW()),

    -- Iluminação
    (gen_random_uuid(), 'OSRAM',       'osram',       NOW()),
    (gen_random_uuid(), 'Hella',       'hella',       NOW()),

    -- Limpeza
    (gen_random_uuid(), 'Vonixx',      'vonixx',      NOW()),
    (gen_random_uuid(), 'Cadillac',    'cadillac',    NOW()),

    -- Motor
    (gen_random_uuid(), 'Mahle',       'mahle',       NOW()),
    (gen_random_uuid(), 'NGK',         'ngk',         NOW()),

    -- Performance
    (gen_random_uuid(), 'K&N',         'k-n',         NOW()),
    (gen_random_uuid(), 'Metal Horse', 'metal-horse', NOW()),

    -- Pneus
    (gen_random_uuid(), 'Michelin',    'michelin',    NOW()),
    (gen_random_uuid(), 'Goodyear',    'goodyear',    NOW()),

    -- Rodas
    (gen_random_uuid(), 'BBS',         'bbs',         NOW()),
    (gen_random_uuid(), 'Enkei',       'enkei',       NOW()),

    -- Suspensão
    (gen_random_uuid(), 'Eibach',      'eibach',      NOW()),
    (gen_random_uuid(), 'D2 Racing',   'd2-racing',   NOW());
