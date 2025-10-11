INSERT INTO notifications (created_at, updated_at, created_by, updated_by, is_deleted, title, body, path, avatar_url, post_image_url, type)
VALUES
    (now(), now(), 'System', 'System', false, 'Bình luận mới', 'Người dùng A đã bình luận vào công thức của bạn', '/posts/1', 'https://img.com/u1.png', 'https://img.com/post1.png', 'COMMENT'),
    (now(), now(), 'System', 'System', false, 'Phản ứng mới', 'Người dùng B đã thả cảm xúc vào công thức của bạn', '/posts/2', 'https://img.com/u2.png', 'https://img.com/post2.png', 'REACTION'),
    (now(), now(), 'System', 'System', false, 'Người theo dõi mới', 'Người dùng C đã theo dõi bạn', '/users/3', 'https://img.com/u3.png', null, 'FOLLOW'),
    (now(), now(), 'System', 'System', false, 'Thông báo hệ thống', 'Hệ thống vừa cập nhật tính năng mới', '/System/update', null, null, 'SYSTEM'),
    (now(), now(), 'System', 'System', false, 'Lưu công thức', 'Bạn đã lưu công thức "Phở bò" vào danh sách mua sắm', '/recipes/5', null, 'https://img.com/post5.png', 'SAVE_RECIPE'),
    (now(), now(), 'System', 'System', false, 'Chia sẻ công thức', 'Người dùng D đã chia sẻ công thức của bạn', '/recipes/6', 'https://img.com/u4.png', 'https://img.com/post6.png', 'SHARE_RECIPE'),
    (now(), now(), 'System', 'System', false, 'Chào mừng', 'Chào mừng bạn quay lại EEFOOD, chúc bạn một ngày tốt lành!', '/welcome', null, null, 'SYSTEM'),
    (now(), now(), 'System', 'System', false, 'Công thức trong ngày', 'Gợi ý hôm nay: Bánh mì kẹp thịt gà thơm ngon', '/recipes/7', null, 'https://img.com/post7.png', 'SYSTEM'),
    (now(), now(), 'System', 'System', false, 'Phản ứng mới', 'Người dùng E đã thả tim vào công thức của bạn', '/posts/8', 'https://img.com/u5.png', 'https://img.com/post8.png', 'REACTION'),
    (now(), now(), 'System', 'System', false, 'Bình luận mới', 'Người dùng F đã bình luận: "Công thức này rất ngon!"', '/posts/9', 'https://img.com/u6.png', 'https://img.com/post9.png', 'COMMENT');

-- ================================
-- Dữ liệu mẫu cho notifications_recipient (10 rows)
-- ================================
INSERT INTO notifications_recipient (created_at, updated_at, created_by, updated_by, is_deleted, user_id, notification_id, is_read, read_at)
VALUES
    (now(), now(), 'System', 'System', false,1, 1, false, null),
    (now(), now(), 'System', 'System', false,1, 10, true, now()),
    (now(), now(), 'System', 'System', false,1, 2, false, null),
    (now(), now(), 'System', 'System', false,1, 3, true, now()),
    (now(), now(), 'System', 'System', false,1, 4, false, null),
    (now(), now(), 'System', 'System', false,1, 5, true, now()),
    (now(), now(), 'System', 'System', false,1, 6, false, null),
    (now(), now(), 'System', 'System', false,1, 7, true, now()),
    (now(), now(), 'System', 'System', false,1, 8, false, null),
    (now(), now(), 'System', 'System', false,1, 9, false, null);

-- ================================
-- Dữ liệu mẫu cho notifications_setting (10 rows)
-- ================================
INSERT INTO notifications_setting (created_at, updated_at, created_by, updated_by, is_deleted, user_id, type, enabled)
VALUES
    (now(), now(), 'System', 'System', false, 1, 'COMMENT', true),
    (now(), now(), 'System', 'System', false, 1, 'REACTION', true),
    (now(), now(), 'System', 'System', false, 1, 'FOLLOW', true),
    (now(), now(), 'System', 'System', false, 1, 'COMMENT', false),
    (now(), now(), 'System', 'System', false, 1, 'REACTION', true),
    (now(), now(), 'System', 'System', false, 1, 'FOLLOW', false),
    (now(), now(), 'System', 'System', false, 1, 'SAVE_RECIPE', true),
    (now(), now(), 'System', 'System', false, 1, 'SHARE_RECIPE', true),
    (now(), now(), 'System', 'System', false, 1, 'WELCOME', true),
    (now(), now(), 'System', 'System', false, 1, 'RECIPE_OF_THE_DAY', false);