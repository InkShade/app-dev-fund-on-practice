-- Demo data, loaded on application start (disabled in tests).
-- Expiry dates are relative to today so the demo always has fresh, expiring and expired products.

INSERT INTO ingredient (name, unit, shelf_life_days) VALUES
    ('Eggs', 'PIECE', 21),
    ('Milk', 'MILLILITER', 7),
    ('Butter', 'GRAM', 30),
    ('Cheese', 'GRAM', 21),
    ('Flour', 'GRAM', 180),
    ('Sugar', 'GRAM', 365),
    ('Pasta', 'GRAM', 365),
    ('Tomatoes', 'PIECE', 7),
    ('Garlic', 'PIECE', 30),
    ('Olive oil', 'MILLILITER', 365),
    ('Cucumber', 'PIECE', 7),
    ('Onion', 'PIECE', 30),
    ('Chicken breast', 'GRAM', 3),
    ('Rice', 'GRAM', 365),
    ('Carrot', 'PIECE', 21),
    ('Potatoes', 'PIECE', 30),
    ('Banana', 'PIECE', 5),
    ('Yogurt', 'GRAM', 14),
    ('Bread', 'PIECE', 5),
    ('Oats', 'GRAM', 180),
    ('Honey', 'GRAM', 365);

INSERT INTO recipe (name, description, instructions, cooking_time_minutes) VALUES
    ('Classic omelette', 'Fluffy three-egg omelette, ready in minutes.',
     '1. Whisk the eggs with the milk and a pinch of salt.
2. Melt the butter in a pan over medium heat.
3. Pour in the eggs, stir gently and fold once set.', 10),
    ('Pancakes', 'Thin golden pancakes for a lazy weekend breakfast.',
     '1. Whisk eggs, sugar and milk.
2. Add the flour and mix into a smooth batter.
3. Melt a little butter in a pan and fry thin pancakes on both sides.', 30),
    ('Tomato pasta', 'Simple pasta with a quick garlic and tomato sauce.',
     '1. Cook the pasta in salted water.
2. Fry chopped garlic in olive oil, add diced tomatoes and simmer for 10 minutes.
3. Toss the pasta with the sauce and top with grated cheese.', 25),
    ('Greek salad', 'Fresh summer salad with cheese and olive oil.',
     '1. Cut the tomatoes, cucumber and onion into chunks.
2. Add cubed cheese.
3. Dress with olive oil, salt and pepper.', 10),
    ('Chicken and rice', 'One-pan chicken with rice and vegetables.',
     '1. Fry diced chicken in olive oil until golden.
2. Add chopped onion and carrot, cook for 5 minutes.
3. Add rice and twice as much water, cover and simmer for 20 minutes.', 40),
    ('Vegetable soup', 'Warming soup that uses up leftover vegetables.',
     '1. Dice the potatoes, carrots, onion and tomatoes.
2. Put everything in a pot with garlic, cover with water and bring to the boil.
3. Simmer for 25 minutes and season to taste.', 45),
    ('Mashed potatoes', 'Creamy mash, the perfect side dish.',
     '1. Boil peeled potatoes until soft.
2. Drain and mash with butter.
3. Stir in warm milk until creamy.', 30),
    ('Banana smoothie', 'Thick breakfast smoothie.',
     '1. Put bananas, yogurt and milk into a blender.
2. Blend until smooth and serve cold.', 5),
    ('French toast', 'Sweet fried bread, great for stale slices.',
     '1. Whisk eggs, milk and sugar.
2. Soak the bread slices in the mixture.
3. Fry in butter until golden on both sides.', 15),
    ('Cheese sandwich', 'Toasted sandwich with melted cheese and tomato.',
     '1. Butter the bread.
2. Fill with cheese and sliced tomato.
3. Toast in a pan until the cheese melts.', 10),
    ('Egg fried rice', 'Quick fried rice with vegetables.',
     '1. Fry chopped onion and carrot in olive oil.
2. Add cooked rice and fry for 3 minutes.
3. Push the rice aside, scramble the eggs and mix everything together.', 20),
    ('Yogurt parfait', 'Layered yogurt, banana and oats.',
     '1. Layer yogurt, sliced banana and oats in a glass.
2. Drizzle with honey.', 5);

INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity)
SELECT r.id, i.id, v.quantity
FROM (VALUES
    ('Classic omelette', 'Eggs', 3),
    ('Classic omelette', 'Milk', 50),
    ('Classic omelette', 'Butter', 10),
    ('Pancakes', 'Flour', 200),
    ('Pancakes', 'Milk', 300),
    ('Pancakes', 'Eggs', 2),
    ('Pancakes', 'Sugar', 20),
    ('Pancakes', 'Butter', 20),
    ('Tomato pasta', 'Pasta', 200),
    ('Tomato pasta', 'Tomatoes', 3),
    ('Tomato pasta', 'Garlic', 2),
    ('Tomato pasta', 'Olive oil', 20),
    ('Tomato pasta', 'Cheese', 30),
    ('Greek salad', 'Tomatoes', 2),
    ('Greek salad', 'Cucumber', 1),
    ('Greek salad', 'Cheese', 100),
    ('Greek salad', 'Onion', 1),
    ('Greek salad', 'Olive oil', 20),
    ('Chicken and rice', 'Chicken breast', 300),
    ('Chicken and rice', 'Rice', 150),
    ('Chicken and rice', 'Onion', 1),
    ('Chicken and rice', 'Carrot', 1),
    ('Chicken and rice', 'Olive oil', 15),
    ('Vegetable soup', 'Potatoes', 3),
    ('Vegetable soup', 'Carrot', 2),
    ('Vegetable soup', 'Onion', 1),
    ('Vegetable soup', 'Tomatoes', 2),
    ('Vegetable soup', 'Garlic', 1),
    ('Mashed potatoes', 'Potatoes', 5),
    ('Mashed potatoes', 'Milk', 100),
    ('Mashed potatoes', 'Butter', 30),
    ('Banana smoothie', 'Banana', 2),
    ('Banana smoothie', 'Milk', 250),
    ('Banana smoothie', 'Yogurt', 150),
    ('French toast', 'Bread', 4),
    ('French toast', 'Eggs', 2),
    ('French toast', 'Milk', 100),
    ('French toast', 'Sugar', 10),
    ('French toast', 'Butter', 10),
    ('Cheese sandwich', 'Bread', 2),
    ('Cheese sandwich', 'Cheese', 50),
    ('Cheese sandwich', 'Butter', 10),
    ('Cheese sandwich', 'Tomatoes', 1),
    ('Egg fried rice', 'Rice', 200),
    ('Egg fried rice', 'Eggs', 2),
    ('Egg fried rice', 'Carrot', 1),
    ('Egg fried rice', 'Onion', 1),
    ('Egg fried rice', 'Olive oil', 20),
    ('Yogurt parfait', 'Yogurt', 200),
    ('Yogurt parfait', 'Banana', 1),
    ('Yogurt parfait', 'Oats', 50),
    ('Yogurt parfait', 'Honey', 15)
) AS v (recipe_name, ingredient_name, quantity)
JOIN recipe r ON r.name = v.recipe_name
JOIN ingredient i ON i.name = v.ingredient_name;

INSERT INTO fridge_item (ingredient_id, quantity, expiry_date)
SELECT i.id, v.quantity, DATEADD(DAY, v.days_left, CURRENT_DATE)
FROM (VALUES
    ('Eggs', 6, 10),
    ('Milk', 1000, 2),
    ('Butter', 200, 20),
    ('Cheese', 150, 1),
    ('Tomatoes', 4, 3),
    ('Onion', 3, 25),
    ('Potatoes', 6, 15),
    ('Chicken breast', 400, 1),
    ('Rice', 1000, 200),
    ('Carrot', 3, 12),
    ('Banana', 2, 0),
    ('Olive oil', 500, 300),
    ('Garlic', 5, 20),
    ('Flour', 500, 100),
    ('Sugar', 500, 300),
    ('Bread', 6, -1),
    ('Yogurt', 100, -2)
) AS v (ingredient_name, quantity, days_left)
JOIN ingredient i ON i.name = v.ingredient_name;

INSERT INTO shopping_list_item (ingredient_id, quantity, purchased)
SELECT id, 250, FALSE FROM ingredient WHERE name = 'Honey';
