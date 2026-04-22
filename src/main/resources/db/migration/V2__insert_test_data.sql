INSERT INTO voter (document, polling_station, has_voted, has_fines, birth_date)
SELECT 
    (10000000 + gs * 13)::text AS document,

    'Mesa ' || (gs % 300) || ' - ' ||
    (
        ARRAY[
            'Bogotá','Medellín','Cali','Barranquilla','Cartagena',
            'Bucaramanga','Pereira','Manizales','Cúcuta','Santa Marta',
            'Villavicencio','Pasto','Armenia','Ibagué','Neiva',
            'Montería','Sincelejo','Valledupar','Popayán','Tunja'
        ]
    )[floor(random() * 20 + 1)],

    (random() > 0.45),   -- has_voted

    (random() > 0.7),    -- has_fines (~30%)

    CURRENT_DATE - ((18 + floor(random() * 72))::int * INTERVAL '1 year')  -- birth_date (18–90 años)

FROM generate_series(1, 10000) gs;