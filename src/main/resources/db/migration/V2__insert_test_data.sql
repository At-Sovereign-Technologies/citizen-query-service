INSERT INTO voter (document, polling_station, has_voted, has_fines)
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

    (random() > 0.7)     -- has_fines (30% aprox con multas)

FROM generate_series(1, 10000) gs;