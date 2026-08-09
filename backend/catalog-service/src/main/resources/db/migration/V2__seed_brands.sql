INSERT INTO brands (id, name, slug, created_at) VALUES
    -- Acessórios
    (gen_random_uuid(), 'Sparco',      'sparco',      NOW()),
    (gen_random_uuid(), 'Metal Horse', 'metal-horse', NOW()),

    -- Escapamento
    (gen_random_uuid(), 'Pro Line',    'pro-line',    NOW()),

    -- Freios
    (gen_random_uuid(), 'Brembo',      'brembo',      NOW()),

    -- Iluminação
    (gen_random_uuid(), 'OSRAM',       'osram',       NOW()),
    (gen_random_uuid(), 'Philips',     'philips',     NOW()),

    -- Limpeza
    (gen_random_uuid(), 'Vonixx',      'vonixx',      NOW()),
    (gen_random_uuid(), 'Cadillac',    'cadillac',    NOW()),

    -- Motor
    (gen_random_uuid(), 'NGK',         'ngk',         NOW()),
    (gen_random_uuid(), 'Bosch',       'bosch',       NOW()),
    (gen_random_uuid(), 'AFP',         'afp',         NOW()),

    -- Performance
    (gen_random_uuid(), 'K&N',         'k-n',         NOW()),
    (gen_random_uuid(), 'FuelTech',    'fueltech',    NOW()),

    -- Pneus
    (gen_random_uuid(), 'Michelin',    'michelin',    NOW()),
    (gen_random_uuid(), 'Goodyear',    'goodyear',    NOW()),

    -- Rodas
    (gen_random_uuid(), 'BBS',         'bbs',         NOW()),
    (gen_random_uuid(), 'Enkei',       'enkei',       NOW()),
    (gen_random_uuid(), 'OZ Racing',   'oz-racing',   NOW()),

    -- Suspensão
    (gen_random_uuid(), 'Eibach',      'eibach',      NOW()),
    (gen_random_uuid(), 'D2 Racing',   'd2-racing',   NOW());
