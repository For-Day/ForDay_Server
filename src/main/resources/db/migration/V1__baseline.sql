-- ForDay MySQL 스키마 기준선 (Flyway baseline)
--
-- 생성 근거: 2026-10-01 운영 DB(k3s forday-data/mysql-0, MySQL 8.0.37)에서
--   mysqldump --no-data --routines --triggers --skip-add-drop-table --skip-comments
-- 로 뜬 구조 덤프. 테이블 24 / 컬럼 206 / FK 25 / 인덱스 54.
--
-- 이 파일은 "현재 운영 스키마를 그대로 선언으로 옮긴 것"이다. 설계 개선(인덱스 추가,
-- 정규화, collation 통일 등)은 여기서 하지 않고 V2 이후로 분리한다.
--
-- 덤프에서 바꾼 것 두 가지:
--   1. AUTO_INCREMENT=N 테이블 옵션 제거 - 스키마가 아니라 그 시점의 카운터 값이라
--      빈 DB에 적용한 결과와 운영을 비교할 때 매번 어긋난다.
--   2. mysqldump의 /*!...*/ 조건부 주석 래퍼 제거 - 대신 아래에서 FK 검사를 명시로 끈다.
--
-- FOREIGN_KEY_CHECKS: mysqldump가 테이블을 알파벳순으로 내보내므로 아직 만들어지지
-- 않은 테이블을 참조하는 FK가 생긴다. 덤프가 조건부 주석으로 하던 일을 평문으로 옮겼다.
--
-- 제약명(FKjkwpuymqnqrn0a6kos0kaaf1h 등)은 Hibernate가 생성한 이름이다. 운영과 같은
-- 이름을 유지해야 비교가 성립하므로 바꾸지 않는다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `activity_recommend_items` (
  `activity_recommend_item_id` bigint NOT NULL AUTO_INCREMENT,
  `user_hobby_id` bigint NOT NULL,
  `content` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`activity_recommend_item_id`),
  KEY `idx_user_hobby_id` (`user_hobby_id`),
  CONSTRAINT `FKjkwpuymqnqrn0a6kos0kaaf1h` FOREIGN KEY (`user_hobby_id`) REFERENCES `user_hobbies` (`user_hobby_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `activity_record_reactions` (
  `activity_record_reaction_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `reaction_type` enum('AMAZING','AWESOME','FIGHTING','GREAT') NOT NULL,
  `read_writer` bit(1) NOT NULL,
  `activity_record_id` bigint NOT NULL,
  `reacted_user_id` char(36) NOT NULL,
  PRIMARY KEY (`activity_record_reaction_id`),
  UNIQUE KEY `uk_record_user_type` (`activity_record_id`,`reacted_user_id`,`reaction_type`),
  KEY `FKqbtt81f1283tjm4658ogax30l` (`reacted_user_id`),
  KEY `idx_reaction_record_type` (`activity_record_id`,`reaction_type`),
  CONSTRAINT `FK2aiwpi9i9853kh0q3lehg8w8e` FOREIGN KEY (`activity_record_id`) REFERENCES `activity_records` (`activity_record_id`),
  CONSTRAINT `FKqbtt81f1283tjm4658ogax30l` FOREIGN KEY (`reacted_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `activity_record_reports` (
  `activity_record_report_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `reported_record_id` bigint NOT NULL,
  `reported_user_id` char(36) NOT NULL,
  `reporter_id` char(36) NOT NULL,
  PRIMARY KEY (`activity_record_report_id`),
  KEY `FKmlb0ri62u99wcn8wcs4v75bx4` (`reported_record_id`),
  KEY `FKjnmie3xt28arejlt0wujchwba` (`reported_user_id`),
  KEY `FKgav3jt97rsdv13wtdab2jj1ym` (`reporter_id`),
  CONSTRAINT `FKgav3jt97rsdv13wtdab2jj1ym` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKjnmie3xt28arejlt0wujchwba` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKmlb0ri62u99wcn8wcs4v75bx4` FOREIGN KEY (`reported_record_id`) REFERENCES `activity_records` (`activity_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `activity_record_scraps` (
  `activity_record_scrap_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `activity_record_id` bigint NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`activity_record_scrap_id`),
  KEY `FK2btc2t1youvvb1tb7mg3su8a4` (`activity_record_id`),
  KEY `FKcabx2ga1wla1wfy7nswq041pf` (`user_id`),
  CONSTRAINT `FK2btc2t1youvvb1tb7mg3su8a4` FOREIGN KEY (`activity_record_id`) REFERENCES `activity_records` (`activity_record_id`),
  CONSTRAINT `FKcabx2ga1wla1wfy7nswq041pf` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `activity_records` (
  `activity_record_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `deleted` bit(1) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `memo` varchar(200) DEFAULT NULL,
  `sticker` varchar(20) DEFAULT NULL,
  `visibility` enum('FRIEND','PRIVATE','PUBLIC') NOT NULL,
  `user_activity_id` bigint NOT NULL,
  `user_hobby_id` bigint NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`activity_record_id`),
  KEY `FKbt5k0h762f944nki7oc935ibd` (`user_activity_id`),
  KEY `FKlhq845xxtf7gg74hi1ospm4hh` (`user_id`),
  KEY `idx_ar_user_hobby_created` (`user_hobby_id`,`user_id`,`created_at` DESC),
  CONSTRAINT `FK3tlrn8ulwlaht2iafo0kqrrs4` FOREIGN KEY (`user_hobby_id`) REFERENCES `user_hobbies` (`user_hobby_id`),
  CONSTRAINT `FKbt5k0h762f944nki7oc935ibd` FOREIGN KEY (`user_activity_id`) REFERENCES `user_activities` (`user_activity_id`),
  CONSTRAINT `FKlhq845xxtf7gg74hi1ospm4hh` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `app_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `block_enabled` bit(1) DEFAULT b'0' COMMENT '차단 여부 (0:미차단, 1:차단)',
  `latest_build` int NOT NULL COMMENT '최신 빌드 번호',
  `min_supported_build` int NOT NULL COMMENT '최소 지원 빌드 번호',
  `policy_version` int DEFAULT '1' COMMENT '정책 버전',
  `latest_version` varchar(20) NOT NULL COMMENT '최신 버전명 (예: 1.1.0)',
  `min_supported_version` varchar(20) NOT NULL COMMENT '최소 지원 버전명',
  `block_message` varchar(255) DEFAULT NULL COMMENT '차단 시 노출 문구',
  `force_message` varchar(255) DEFAULT NULL COMMENT '강제 업데이트 문구',
  `recommend_message` varchar(255) DEFAULT NULL COMMENT '권장 업데이트 문구',
  `store_url` varchar(255) DEFAULT NULL COMMENT '앱스토어/플레이스토어 주소',
  `platform` enum('ANDROID','IOS') NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `fcm_tokens` (
  `fcm_token_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `device_id` varchar(255) NOT NULL,
  `device_type` enum('ANDROID','IOS') DEFAULT NULL,
  `fcm_token` varchar(255) NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`fcm_token_id`),
  UNIQUE KEY `UKtgq9e1yv7gjnen5k9mbo2u9v` (`fcm_token`),
  KEY `FKj2kob865pl9dv5vwrs2pmshjv` (`user_id`),
  CONSTRAINT `FKj2kob865pl9dv5vwrs2pmshjv` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `friend_relations` (
  `friend_relation_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `relation_status` enum('BLOCK','FOLLOW','REPORT') NOT NULL,
  `requester_id` char(36) NOT NULL,
  `target_id` char(36) NOT NULL,
  PRIMARY KEY (`friend_relation_id`),
  KEY `idx_requester_target` (`requester_id`,`target_id`),
  KEY `idx_target_requester` (`target_id`,`requester_id`),
  CONSTRAINT `FKdat9d5o5vjnaf5rgce6p9tedb` FOREIGN KEY (`target_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKkaxr2pyaec2fv21dt9leueedk` FOREIGN KEY (`requester_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hobby_cards` (
  `user_hobby_card_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` varchar(100) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `user_hobby_id` bigint NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`user_hobby_card_id`),
  KEY `FKqg1l2g854wy5t6uy2so8pt20x` (`user_hobby_id`),
  KEY `FKdu9jg21iqxsmr982kvdvwav4x` (`user_id`),
  CONSTRAINT `FKdu9jg21iqxsmr982kvdvwav4x` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKqg1l2g854wy5t6uy2so8pt20x` FOREIGN KEY (`user_hobby_id`) REFERENCES `user_hobbies` (`user_hobby_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hobby_info` (
  `hobby_info_id` bigint NOT NULL,
  `hobby_description` varchar(50) NOT NULL,
  `hobby_name` varchar(20) NOT NULL,
  `image_code` varchar(20) NOT NULL,
  PRIMARY KEY (`hobby_info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `keyboard_keywords` (
  `keyword_id` bigint NOT NULL AUTO_INCREMENT,
  `hobby_info_id` bigint NOT NULL,
  `keyword` varchar(50) NOT NULL,
  PRIMARY KEY (`keyword_id`),
  KEY `idx_keyboard_keyword_hobby_info_id` (`hobby_info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_outbox` (
  `notification_outbox_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `failed_at` datetime(6) DEFAULT NULL,
  `notification_id` bigint NOT NULL,
  `payload` tinytext NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','PUBLISHED') NOT NULL,
  PRIMARY KEY (`notification_outbox_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notifications` (
  `dtype` varchar(31) NOT NULL,
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(500) NOT NULL,
  `type` enum('FRIEND','GROUP','RECORD_COMMENT','RECORD_REACTION') NOT NULL,
  `comment_content` varchar(255) DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `record_id` bigint DEFAULT NULL,
  `reaction_type` tinyint DEFAULT NULL,
  `receiver_id` char(36) NOT NULL,
  `sender_id` char(36) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK9kxl0whvhifo6gw4tjq36v53k` (`receiver_id`),
  KEY `FK13vcnq3ukas06ho1yrbc5lrb5` (`sender_id`),
  CONSTRAINT `FK13vcnq3ukas06ho1yrbc5lrb5` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK9kxl0whvhifo6gw4tjq36v53k` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `notifications_chk_1` CHECK ((`reaction_type` between 0 and 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `other_activities` (
  `other_activity_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `purpose` varchar(255) DEFAULT NULL,
  `time_minutes` int NOT NULL,
  `hobby_info_id` bigint NOT NULL,
  PRIMARY KEY (`other_activity_id`),
  KEY `FKcebl8r2lsl9h1fbsygcvrdkxt` (`hobby_info_id`),
  CONSTRAINT `FKcebl8r2lsl9h1fbsygcvrdkxt` FOREIGN KEY (`hobby_info_id`) REFERENCES `hobby_info` (`hobby_info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `record_images` (
  `record_image_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `image_height` bigint DEFAULT NULL,
  `image_order` int DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `image_width` bigint DEFAULT NULL,
  `thumbnail` bit(1) DEFAULT NULL,
  `activity_record_id` bigint DEFAULT NULL,
  PRIMARY KEY (`record_image_id`),
  KEY `FK3h8onx5jwx4x8vqql2mxakfkx` (`activity_record_id`),
  CONSTRAINT `FK3h8onx5jwx4x8vqql2mxakfkx` FOREIGN KEY (`activity_record_id`) REFERENCES `activity_records` (`activity_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `record_reaction_count` (
  `record_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `amazing_count` bigint DEFAULT NULL,
  `awesome_count` bigint DEFAULT NULL,
  `fighting_count` bigint DEFAULT NULL,
  `great_count` bigint DEFAULT NULL,
  `total_count` bigint DEFAULT NULL,
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `service_contact_info` (
  `info_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `company_name` varchar(100) NOT NULL,
  `contact_number` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `representative` varchar(50) NOT NULL,
  `service_name` varchar(100) NOT NULL,
  PRIMARY KEY (`info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `terms_article_items` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `item_content` text NOT NULL,
  `item_no` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `article_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKa6oj944xa61e41ueiefs3uhcj` (`article_id`),
  CONSTRAINT `FKa6oj944xa61e41ueiefs3uhcj` FOREIGN KEY (`article_id`) REFERENCES `terms_articles` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `terms_articles` (
  `article_id` bigint NOT NULL AUTO_INCREMENT,
  `clause_no` int DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `section_no` bigint NOT NULL,
  `section_title` varchar(100) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `document_id` bigint DEFAULT NULL,
  PRIMARY KEY (`article_id`),
  KEY `FK322lhmci72yjiuiiwymclwj1v` (`document_id`),
  CONSTRAINT `FK322lhmci72yjiuiiwymclwj1v` FOREIGN KEY (`document_id`) REFERENCES `terms_documents` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `terms_documents` (
  `document_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` text,
  `document_type` enum('PRIVACY','TERMS') NOT NULL,
  `effective_at` datetime(6) DEFAULT NULL,
  `is_mandatory` bit(1) NOT NULL,
  `title` varchar(100) NOT NULL,
  `version` varchar(20) NOT NULL,
  PRIMARY KEY (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_activities` (
  `user_activity_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_recommended` bit(1) NOT NULL,
  `collected_sticker_num` int DEFAULT NULL,
  `content` varchar(50) NOT NULL,
  `last_recorded_at` datetime(6) DEFAULT NULL,
  `user_hobby_id` bigint NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`user_activity_id`),
  KEY `FKbe7yq8t74yxeoarmxlxevoped` (`user_id`),
  KEY `idx_ua_hobby_perfect_sort` (`user_hobby_id`,`created_at` DESC,`last_recorded_at` DESC,`collected_sticker_num` DESC,`content`),
  CONSTRAINT `FK64n4mmwkui71gwkwxgmune06k` FOREIGN KEY (`user_hobby_id`) REFERENCES `user_hobbies` (`user_hobby_id`),
  CONSTRAINT `FKbe7yq8t74yxeoarmxlxevoped` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_hobbies` (
  `user_hobby_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `cover_image_url` varchar(255) DEFAULT NULL,
  `current_sticker_num` int DEFAULT NULL,
  `execution_count` int DEFAULT NULL,
  `goal_days` int DEFAULT NULL,
  `hobby_info_id` bigint DEFAULT NULL,
  `hobby_name` varchar(20) NOT NULL,
  `hobby_purpose` varchar(50) DEFAULT NULL,
  `hobby_time_minutes` int DEFAULT NULL,
  `status` enum('ALL','ARCHIVED','IN_PROGRESS') NOT NULL,
  `user_id` char(36) NOT NULL,
  `sequence` int DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  PRIMARY KEY (`user_hobby_id`),
  KEY `idx_hobby_user_status_created` (`user_id`,`status`,`created_at` DESC),
  CONSTRAINT `FK1aia57pgihndujoa8vm7eoccn` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_terms_consent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `age_over14consent` bit(1) NOT NULL,
  `agreement_version` varchar(20) NOT NULL,
  `private_consent` bit(1) NOT NULL,
  `record_push_consent` bit(1) NOT NULL,
  `service_consent` bit(1) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id` char(36) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `deleted` bit(1) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `hobby_card_count` int DEFAULT NULL,
  `last_activity_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `onboarding_completed` bit(1) NOT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','GUEST','USER') NOT NULL,
  `social_id` varchar(255) NOT NULL,
  `social_type` enum('APPLE','GUEST','KAKAO') NOT NULL,
  `total_collected_sticker_count` int DEFAULT NULL,
  `is_app_push_enabled` bit(1) NOT NULL,
  `is_record_push_enabled` bit(1) NOT NULL,
  `terms_consent_completed` bit(1) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_social_id` (`social_id`),
  UNIQUE KEY `uk_users_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
