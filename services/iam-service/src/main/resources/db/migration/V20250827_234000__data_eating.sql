-- Insert allergies
INSERT INTO public.user_allergies (user_id, allergy) VALUES
     (1, 'Tôm'),
     (2, 'Sữa bò'),
     (3, 'Lạc (đậu phộng)'),
     (4, 'Trứng gà'),
     (5, 'Cua biển'),
     (6, 'Đậu nành'),
     (7, 'Mè (vừng)'),
     (8, 'Cá biển'),
     (9, 'Tôm'),
     (10, 'Sữa bò'),
     (11, 'Lạc (đậu phộng)'),
     (12, 'Cua biển'),
     (13, 'Trứng gà'),
     (14, 'Cá biển'),
     (15, 'Mè (vừng)'),
     (16, 'Đậu nành'),
     (17, 'Sữa bò'),
     (18, 'Tôm'),
     (19, 'Trứng gà'),
     (20, 'Lạc (đậu phộng)');

-- Một số người có thêm dị ứng thứ 2
INSERT INTO public.user_allergies (user_id, allergy) VALUES
     (2, 'Trứng gà'),
     (5, 'Lạc (đậu phộng)'),
     (9, 'Sữa bò'),
     (13, 'Tôm'),
     (15, 'Cua biển');

-- Insert eating preferences
INSERT INTO public.user_eating_preferences (user_id, preference) VALUES
    (1, 'Ăn chay 1 phần'),
    (2, 'Ít dầu mỡ'),
    (3, 'Thích đồ cay'),
    (4, 'Ăn nhiều rau'),
    (5, 'Ưa thích hải sản'),
    (6, 'Thích đồ ngọt'),
    (7, 'Ăn uống lành mạnh'),
    (8, 'Ăn chay trường'),
    (9, 'Thích đồ nướng'),
    (10, 'Ưa thích món truyền thống'),
    (11, 'Ít đường'),
    (12, 'Ăn nhiều rau'),
    (13, 'Thích đồ cay'),
    (14, 'Ăn chay 1 phần'),
    (15, 'Ưa thích hải sản'),
    (16, 'Ít dầu mỡ'),
    (17, 'Thích đồ ngọt'),
    (18, 'Ăn uống lành mạnh'),
    (19, 'Ăn nhiều rau'),
    (20, 'Thích đồ nướng');

-- Một số người có thêm sở thích thứ 2
INSERT INTO public.user_eating_preferences (user_id, preference) VALUES
     (1, 'Thích đồ cay'),
     (4, 'Ít đường'),
     (7, 'Thích đồ nướng'),
     (9, 'Ít dầu mỡ'),
     (12, 'Thích đồ ngọt'),
     (16, 'Ưa thích món truyền thống'),
     (18, 'Thích đồ cay');
