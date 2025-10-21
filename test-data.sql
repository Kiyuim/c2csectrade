
-- =================================================================
-- RebookTrade - Test Data Import Script
-- =================================================================

USE trade;

-- Disable foreign key checks to allow clean data import
SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing test data (keep admin user with id=1)
DELETE FROM `shopping_cart` WHERE 1=1;
DELETE FROM `user_favorites` WHERE 1=1;
DELETE FROM `pms_product_media` WHERE 1=1;
DELETE FROM `pms_product` WHERE 1=1;
DELETE FROM `user_roles` WHERE user_id > 1;
DELETE FROM `users` WHERE id > 1;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- The password hash is: $2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9
INSERT INTO `users` (`id`, `username`, `display_name`, `password_hash`, `email`, `avatar_url`) VALUES
                                                                                                   (2, 'bookworm_li', '爱书的李华', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'lihua@example.com', 'https://i.pravatar.cc/150?u=lihua'),
                                                                                                   (3, 'tech_reader_zhang', '技术宅张伟', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'zhangwei@example.com', 'https://i.pravatar.cc/150?u=zhangwei'),
                                                                                                   (4, 'history_fan_wang', '历史迷王芳', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'wangfang@example.com', 'https://i.pravatar.cc/150?u=wangfang'),
                                                                                                   (5, 'sci_fi_chen', '科幻迷陈杰', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'chenjie@example.com', 'https://i.pravatar.cc/150?u=chenjie'),
                                                                                                   (6, 'art_lover_liu', '文艺青年刘洋', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'liuyang@example.com', 'https://i.pravatar.cc/150?u=liuyang'),
                                                                                                   (7, 'econ_master_zhao', '经管达人赵静', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'zhaojing@example.com', 'https://i.pravatar.cc/150?u=zhaojing'),
                                                                                                   (8, 'code_ninja_sun', '编程忍者孙强', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'sunqiang@example.com', 'https://i.pravatar.cc/150?u=sunqiang'),
                                                                                                   (9, 'lit_geek_zhou', '文学极客周敏', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'zhoumin@example.com', 'https://i.pravatar.cc/150?u=zhoumin'),
                                                                                                   (10, 'design_guru_wu', '设计大师吴磊', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'wulei@example.com', 'https://i.pravatar.cc/150?u=wulei'),
                                                                                                   (11, 'travel_bug_zheng', '旅行者郑丽', '$2a$10$e.FR524f25fG8sJ9.o.E8.U2a5.a5n..TjE2O/5g5p9', 'zhengli@example.com', 'https://i.pravatar.cc/150?u=zhengli');

-- Assign ROLE_USER to all test users
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
                                                    (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1);
-- Note: Video URLs are placeholders for demonstration purposes.
SET @last_product_id = 0;

-- Product 1
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (2,'深入理解计算机系统（原书第3版）','计算机系统经典教材，被誉为程序员的百科全书，讲解计算机底层工作原理。',85.00,32,9,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m7.ddimg.cn/25/22/25289437-1_l_1533628187.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',2,1); -- TODO: replace with real book video


-- Product 2
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (3,'算法导论（原书第3版）','MIT经典教材，全面讲解算法设计与分析，适合计算机专业学生与工程师。',96.00,28,8,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m2.ddimg.cn/1/22/23235342-1_l_2.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',2,1); -- TODO: replace with real book video


-- Product 3
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (8,'代码整洁之道','软件工程经典著作，讲解如何写出可维护、清晰的代码。几乎全新。',39.00,44,10,'上海',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m2.ddimg.cn/33/25/20785032-1_l_1.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',2,1); -- TODO: replace with real book video


-- Product 4
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (5,'三体（全集）','刘慈欣科幻巨作，开创中国科幻新时代，全套三册。',55.00,35,9,'杭州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m9.ddimg.cn/31/3/23575429-1_l_2.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book video


-- Product 5
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (8,'Java核心技术 卷I（原书第12版）','Java领域权威参考书，涵盖核心语法与面向对象编程。',110.00,22,10,'深圳',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m2.ddimg.cn/8/23/29363092-1_l_1647501378.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',2,1); -- TODO: replace with real book video


-- Product 6
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (2,'Python编程：从入门到实践（第2版）','Python入门经典，包含丰富项目案例，适合初学者。',45.00,25,9,'成都',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m0.ddimg.cn/9/2/28512190-1_l_1572242509.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',2,1); -- TODO: replace with real book video


-- Product 7
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (9,'设计模式：可复用面向对象软件的基础','面向对象设计的经典之作，讲解23种设计模式。',30.00,15,8,'南京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m4.ddimg.cn/53/26/9215204-1_l.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',2,1); -- TODO: replace with real book video


-- Product 8
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (4,'人类简史：从动物到上帝','尤瓦尔·赫拉利作品，讲述人类从智人到现代文明的发展历程。',42.00,17,9,'武汉',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m1.ddimg.cn/1/33/23434211-1_l_2.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4',2,1); -- TODO: replace with real book video


-- Product 9
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (10,'设计师要懂心理学','从心理学角度解读用户体验与设计原则，几乎全新。',25.00,19,10,'上海',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m3.ddimg.cn/33/3/23257033-1_l_2.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4',2,1); -- TODO: replace with real book video


-- Product 10
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (3,'Effective Java（中文版 第3版）','深入讲解90条高效Java编程经验，Java开发者必读。',50.00,27,9,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                                                                  (@last_product_id,'https://img3m7.ddimg.cn/29/3/27871537-1_l_1559717371.jpg',1,0),
                                                                                  (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book vide

-- Product 11
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (7,'经济学原理（第8版）','曼昆经典教材，系统讲解微观与宏观经济学基础。',79.00,34,9,'苏州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m8.ddimg.cn/89/9/29227218-1_l_1678775072.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',2,1); -- TODO: replace with real book video


-- Product 12
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (6,'自控力','斯坦福大学经典心理学课程，讲述如何战胜拖延与冲动。',33.50,29,9,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/54/13/23203492-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',2,1); -- TODO: replace with real book video


-- Product 13
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (9,'原则（Ray Dalio）','桥水基金创始人瑞·达利欧总结的成功法则，职场与人生指南。',72.00,25,10,'深圳',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m0.ddimg.cn/28/3/25249090-1_l_1546315997.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',2,1); -- TODO: replace with real book video


-- Product 14
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (4,'艺术的故事','恩斯特·贡布里希经典艺术史读物，图文并茂，通俗易懂。',88.00,16,9,'杭州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m1.ddimg.cn/4/13/25227371-1_l_1533629127.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',2,1); -- TODO: replace with real book video


-- Product 15
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (11,'未来简史','赫拉利畅销作品，探讨人类未来的技术与伦理。',49.00,40,9,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m9.ddimg.cn/91/16/23960849-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',2,1); -- TODO: replace with real book video


-- Product 16
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (10,'小王子（纪念版）','经典法语童话，富含哲理与想象力，收藏级版本。',25.00,23,10,'西安',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m5.ddimg.cn/29/12/23568025-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',2,1); -- TODO: replace with real book video


-- Product 17
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (2,'活着（余华）','当代文学经典，讲述平凡人生的坚韧与悲怆。',32.00,20,8,'南京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m0.ddimg.cn/10/2/23856440-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4',2,1); -- TODO: replace with real book video


-- Product 18
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (3,'时间简史','霍金经典科普著作，探索宇宙的起源与命运。',48.00,26,9,'上海',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m7.ddimg.cn/8/10/23836707-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book video


-- Product 19
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (8,'程序员修炼之道','软件开发的哲学与实践指南。',59.00,33,8,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m0.ddimg.cn/65/33/23306850-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',2,1); -- TODO: replace with real book video


-- Product 20
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (5,'深入理解Java虚拟机（第3版）','JVM底层机制详解，适合中高级Java工程师。',76.00,42,9,'成都',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m7.ddimg.cn/62/2/29358907-1_l_1647496503.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',2,1); -- TODO: replace with real book video


-- Product 21
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (7,'乌合之众：大众心理研究','揭示群体行为与社会心理机制的经典之作。',29.00,24,8,'苏州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m8.ddimg.cn/65/9/23368358-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',2,1); -- TODO: replace with real book video


-- Product 22
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (6,'计算机网络（第8版）','谢希仁编著，讲解网络体系结构与协议。',55.00,18,8,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m4.ddimg.cn/13/8/25335354-1_l_1533629051.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',2,1); -- TODO: replace with real book video


-- Product 23
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (9,'机器学习（周志华）','系统介绍机器学习基本原理与算法。',82.00,30,9,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m8.ddimg.cn/59/2/24026558-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',2,1); -- TODO: replace with real book video


-- Product 24
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (4,'计算机程序的构造和解释（SICP）','MIT计算机科学入门经典教材。',69.00,20,9,'杭州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/7/18/23496302-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book video


-- Product 25
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (10,'思考，快与慢','诺贝尔经济学奖得主丹尼尔·卡尼曼力作，探讨思维系统与决策偏差。',45.00,15,9,'上海',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/8/6/23287102-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',2,1);
-- Product 26
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (2,'暗时间','刘慈欣科幻短篇集，探索科技与人性。',38.00,22,8,'南京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m1.ddimg.cn/12/9/23376488-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',2,1); -- TODO: replace with real book video


-- Product 27
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (3,'黑客与画家','Paul Graham作品，讲述技术、创业与思维方式。',41.00,19,9,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m5.ddimg.cn/11/8/23456533-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',2,1); -- TODO: replace with real book video


-- Product 28
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (5,'浪潮之巅','吴军著作，讲述IT行业发展与科技趋势。',50.00,28,9,'成都',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m4.ddimg.cn/14/5/23294567-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',2,1); -- TODO: replace with real book video


-- Product 29
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (6,'程序员的数学','软件工程与计算机数学基础入门教材，适合初中高级程序员。',36.00,31,8,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m7.ddimg.cn/9/12/23309865-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',2,1); -- TODO: replace with real book video


-- Product 30
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (7,'Python深度学习','Francois Chollet作品，讲解Keras与深度学习实战。',92.00,27,10,'苏州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/5/8/23458322-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book video


-- Product 31
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (8,'人月神话','软件工程经典，讲述项目管理与团队协作的经验教训。',44.00,25,9,'深圳',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m0.ddimg.cn/6/7/23458912-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',2,1); -- TODO: replace with real book video


-- Product 32
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (9,'编程珠玑','Jon Bentley经典编程书籍，包含算法与性能优化技巧。',65.00,18,10,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m9.ddimg.cn/7/6/23298765-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',2,1); -- TODO: replace with real book video


-- Product 33
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (2,'深入浅出Node.js','Node.js编程实战书籍，适合后端开发者快速入门。',39.00,30,8,'南京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m5.ddimg.cn/6/11/23498765-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',2,1); -- TODO: replace with real book video


-- Product 34
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (3,'深入浅出React','React框架权威指南，前端开发者必读。',48.00,26,9,'上海',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m4.ddimg.cn/9/10/23212345-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',2,1); -- TODO: replace with real book video


-- Product 35
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (4,'深入浅出Vue.js','Vue.js前端实战教程，项目驱动教学。',52.00,28,9,'杭州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m0.ddimg.cn/5/9/23312345-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1); -- TODO: replace with real book video


-- Product 36
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (5,'计算机组成原理（王道版）','王道教材，详细讲解计算机硬件与汇编原理。',68.00,24,9,'成都',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/7/8/23367890-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',2,1); -- TODO: replace with real book video


-- Product 37
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (6,'代码大全（第2版）','Steve McConnell著作，软件工程经典，讲解高质量代码实践。',55.00,33,10,'北京',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m1.ddimg.cn/8/7/23456789-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',2,1); -- TODO: replace with real book video


-- Product 38
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (7,'黑客攻防技术宝典','网络安全经典教材，讲解攻击与防御方法。',62.00,19,9,'苏州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m9.ddimg.cn/9/6/23245678-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',2,1); -- TODO: replace with real book video


-- Product 39
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (8,'深度学习（Ian Goodfellow）','深度学习领域权威教材，覆盖神经网络、卷积与生成模型。',98.00,21,10,'深圳',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m2.ddimg.cn/7/3/23423456-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',2,1); -- TODO: replace with real book video


-- Product 40
INSERT INTO `pms_product`
(`user_id`,`name`,`description`,`price`,`stock`,`condition_level`,`location`,`status`)
VALUES
    (9,'深入理解Linux内核','Linux内核源码解析，操作系统与内核开发必读。',85.00,29,9,'广州',1);
SET @last_product_id=LAST_INSERT_ID();
INSERT INTO `pms_product_media`(`product_id`,`url`,`media_type`,`sort_order`) VALUES
                                    (@last_product_id,'https://img3m5.ddimg.cn/7/4/23398765-1_l_2.jpg',1,0),
                                    (@last_product_id,'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',2,1);
