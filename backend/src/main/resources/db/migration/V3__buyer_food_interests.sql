-- V3: Buyer food/taste interests for personalized and cold-start item recommendations
ALTER TABLE users ADD COLUMN IF NOT EXISTS food_interests VARCHAR(500);

