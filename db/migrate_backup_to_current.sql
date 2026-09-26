-- Upgrade pet_agent_db.sql to the current Django model (migrations 0005 and 0006).
-- Run this once only after importing the backup into a fresh pet_agent_java database.

ALTER TABLE pets_cartitem ADD COLUMN dog_id bigint NULL AFTER product_id;
ALTER TABLE pets_cartitem DROP FOREIGN KEY pets_cartitem_product_id_feffe9a7_fk_pets_product_id;
ALTER TABLE pets_cartitem MODIFY product_id bigint NULL;
ALTER TABLE pets_cartitem ADD CONSTRAINT pets_cartitem_product_id_feffe9a7_fk_pets_product_id
    FOREIGN KEY (product_id) REFERENCES pets_product(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE pets_cartitem ADD INDEX pets_cartitem_dog_id_java_fk (dog_id);
ALTER TABLE pets_cartitem ADD CONSTRAINT pets_cartitem_dog_id_java_fk
    FOREIGN KEY (dog_id) REFERENCES pets_dog(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE pets_cartitem ADD CONSTRAINT unique_cart_dog UNIQUE (cart_id, dog_id);

ALTER TABLE pets_orderitem ADD COLUMN dog_id bigint NULL AFTER product_id;
ALTER TABLE pets_orderitem DROP FOREIGN KEY pets_orderitem_product_id_3530eccf_fk_pets_product_id;
ALTER TABLE pets_orderitem MODIFY product_id bigint NULL;
ALTER TABLE pets_orderitem ADD CONSTRAINT pets_orderitem_product_id_3530eccf_fk_pets_product_id
    FOREIGN KEY (product_id) REFERENCES pets_product(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE pets_orderitem ADD INDEX pets_orderitem_dog_id_java_fk (dog_id);
ALTER TABLE pets_orderitem ADD CONSTRAINT pets_orderitem_dog_id_java_fk
    FOREIGN KEY (dog_id) REFERENCES pets_dog(id) ON DELETE RESTRICT ON UPDATE RESTRICT;

UPDATE pets_product
SET image_url = CASE name
    WHEN '狂犬疫苗(单针)' THEN '/static/images/products/vaccine-rabies.png'
    WHEN '犬四联疫苗(卫佳伍)' THEN '/static/images/products/vaccine-dhpp.png'
    WHEN '猫三联疫苗(妙三多)' THEN '/static/images/products/vaccine-feline.png'
    WHEN '皇家狗粮15kg' THEN '/static/images/products/food-dog.png'
    WHEN '渴望猫粮5kg' THEN '/static/images/products/food-cat.png'
    WHEN '宠物零食鸡肉干' THEN '/static/images/products/treat-chicken.png'
    WHEN '发声玩具球' THEN '/static/images/products/toy-ball.png'
    WHEN '逗猫棒(羽毛款)' THEN '/static/images/products/toy-cat-wand.png'
    WHEN '漏食球益智玩具' THEN '/static/images/products/toy-puzzle.png'
    WHEN '项圈牵引绳套装' THEN '/static/images/products/leash-collar.png'
    WHEN '宠物猫狗通用睡垫' THEN '/static/images/products/pet-bed.png'
    WHEN '宠物外出水壶' THEN '/static/images/products/water-bottle.png'
    WHEN '宠物指甲剪套装' THEN '/static/images/products/nail-clipper.png'
    WHEN '益生菌(调理肠胃)' THEN '/static/images/products/probiotics.png'
    WHEN '鱼油软胶囊(护毛)' THEN '/static/images/products/fish-oil.png'
    ELSE image_url
END;

INSERT INTO django_migrations (app, name, applied)
SELECT 'pets', '0005_cartitem_dog_orderitem_dog_alter_cartitem_product_and_more', NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM django_migrations
    WHERE app = 'pets'
      AND name = '0005_cartitem_dog_orderitem_dog_alter_cartitem_product_and_more'
);

INSERT INTO django_migrations (app, name, applied)
SELECT 'pets', '0006_update_product_images', NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM django_migrations
    WHERE app = 'pets' AND name = '0006_update_product_images'
);
