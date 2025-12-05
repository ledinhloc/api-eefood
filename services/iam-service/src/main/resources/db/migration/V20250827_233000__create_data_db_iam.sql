INSERT INTO public.users (
    created_by,
    is_deleted,
    updated_at,
    updated_by,
    address,
    auth_id,
    avatar_url,
    background_url,
    dob,
    email,
    gender,
    provider,
    role,
    username
)
VALUES
    ('system', false, now(), 'system', '{"city":"Hà Nội","street":"12 Nguyễn Trãi"}', NULL,
     'https://m.yodycdn.com/blog/anh-dai-dien-hai-yodyvn3-b3a8cf32-e08a-47fc-a741-71626aadc4de.jpg',
     'https://cellphones.com.vn/sforum/wp-content/uploads/2024/04/anh-bia-facebook-41.jpg',
     '2004-05-12', 'ledinhloc7@gmail.com', 'MALE', 'NORMAL', 'USER', 'Le Dinh Loc'),

    ('system', false, now(), 'system', '{"city":"Hồ Chí Minh","street":"45 Lê Lợi"}', NULL,
     'https://hinhnenpowerpoint.app/wp-content/uploads/2025/08/anh-avatar-hai-huoc-17.jpg',
     'https://cellphones.com.vn/sforum/wp-content/uploads/2024/04/anh-bia-facebook-41.jpg',
     '2004-11-23', 'akhoa2109@gmail.com', 'MALE', 'NORMAL', 'USER', 'Nguyen Hoang Anh Khoa'),

    ('system', false, now(), 'system', '{"city":"Đà Nẵng","street":"78 Hải Phòng"}', NULL,
     'https://banobagi.vn/wp-content/uploads/2025/05/avatar-hai-1-1.jpg',
     'https://hoanghamobile.com/tin-tuc/wp-content/uploads/2023/07/anh-bia-dep-10.jpg',
     '1998-03-14', 'minhquan98@gmail.com', 'MALE', 'NORMAL', 'ADMIN', 'Minh Quân'),

    ('system', false, now(), 'system', '{"city":"Huế","street":"21 Hùng Vương"}', NULL,
     'https://avatarngau.sbs/wp-content/uploads/2025/05/avatar-hai-huoc-vo-tri-0.jpg',
     'https://hoanghamobile.com/tin-tuc/wp-content/uploads/2023/07/anh-bia-dep-10.jpg',
     '1990-07-30', 'thuydung90@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Thúy Dung'),

    ('system', false, now(), 'system', '{"city":"Cần Thơ","street":"5 Nguyễn Văn Linh"}', NULL,
     'https://hinhnenpowerpoint.app/wp-content/uploads/2025/08/anh-avatar-hai-huoc-18.jpg',
     'https://cdn2.fptshop.com.vn/unsafe/Uploads/images/tin-tuc/184494/Originals/anh-bia-nam-21.jpg',
     '1988-12-01', 'anhkhoa88@gmail.com', 'MALE', 'GOOGLE', 'USER', 'Anh Khoa'),

    ('system', false, now(), 'system', '{"city":"Hải Phòng","street":"33 Điện Biên Phủ"}', NULL,
     'https://demoda.vn/wp-content/uploads/2022/02/avatar-hai.jpg',
     'https://cdn2.fptshop.com.vn/unsafe/Uploads/images/tin-tuc/184494/Originals/anh-bia-nam-21.jpg',
     '1997-08-18', 'lanphuong97@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Lan Phương'),

    ('system', false, now(), 'system', '{"city":"Quảng Ninh","street":"9 Trần Phú"}', NULL,
     'https://img.tripi.vn/cdn-cgi/image/width=700,height=700/https://gcs.tripi.vn/public-tripi/tripi-feed/img/482740BRi/anh-mo-ta.png',
     'https://phanmemmkt.vn/wp-content/uploads/2024/09/12.jpg',
     '1996-06-10', 'ductruong96@gmail.com', 'MALE', 'NORMAL', 'USER', 'Đức Trường'),

    ('system', false, now(), 'system', '{"city":"Nam Định","street":"101 Hàng Bông"}', NULL,
     'https://cdn2.fptshop.com.vn/unsafe/800x0/avatar_hai_1_79e52b6bf6.jpg',
     'https://phanmemmkt.vn/wp-content/uploads/2024/09/12.jpg',
     '1991-02-27', 'hoamai91@gmail.com', 'FEMALE', 'GOOGLE', 'USER', 'Hoa Mai'),

    ('system', false, now(), 'system', '{"city":"Bình Định","street":"25 Lê Duẩn"}', NULL,
     'https://hinhcute.net/wp-content/uploads/2025/08/buc-anh-avatar-hai-huoc-cuoi-te-tua-10-05-2025.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-47.jpg',
     '1994-09-15', 'hoanganh94@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Hoàng Anh'),

    ('system', false, now(), 'system', '{"city":"Khánh Hòa","street":"88 Trần Quang Khải"}', NULL,
     'https://hoangnguyen.edu.vn/uploads/blog/2024/11/20/ef0316957fe781848d494fe9dd2453532d80808c-1732075937.webp',
     NULL,
     '1993-04-05', 'tienphat93@gmail.com', 'MALE', 'GOOGLE', 'ADMIN', 'Tiến Phát'),

    ('system', false, now(), 'system', '{"city":"Đắk Lắk","street":"12 Lý Thường Kiệt"}', NULL,
     'https://img.tripi.vn/cdn-cgi/image/width=700,height=700/https://gcs.tripi.vn/public-tripi/tripi-feed/img/483123BEp/anh-mo-ta.png',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-47.jpg',
     '1989-10-22', 'vanlam89@gmail.com', 'MALE', 'NORMAL', 'USER', 'Văn Lâm'),

    ('system', false, now(), 'system', '{"city":"Nghệ An","street":"77 Quang Trung"}', NULL,
     'https://bom.edu.vn/public/upload/2024/12/avatar-vo-tri-meme-7.webp',
     NULL,
     '1995-01-19', 'lethao95@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Lệ Thảo'),

    ('system', false, now(), 'system', '{"city":"Thanh Hóa","street":"30 Bà Triệu"}', NULL,
     'https://image.dienthoaivui.com.vn/x,webp,q90/https://dashboard.dienthoaivui.com.vn/uploads/dashboard/editor_upload/anh-meme-16.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-39.jpg',
     '1999-06-09', 'sondinh99@gmail.com', 'MALE', 'GOOGLE', 'USER', 'Sơn Đình'),

    ('system', false, now(), 'system', '{"city":"An Giang","street":"55 Lê Lai"}', NULL,
     'https://hanhtrinhdelta.edu.vn/wp-content/uploads/2025/08/ve-mat-mac-ke-doi-nhung-van-cuc-ky-cute.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-29.jpg',
     '1992-12-31', 'huongtran92@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Hương Trần'),

    ('system', false, now(), 'system', '{"city":"Kiên Giang","street":"60 Nguyễn Huệ"}', NULL,
     'https://tte.edu.vn/public/upload/2025/01/avatar-hai14.webp',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-13.jpg',
     '1997-03-02', 'trieuphu97@gmail.com', 'MALE', 'GOOGLE', 'USER', 'Triệu Phú'),

    ('system', false, now(), 'system', '{"city":"Vũng Tàu","street":"101 Võ Thị Sáu"}', NULL,
     'https://i.pinimg.com/236x/90/12/f9/9012f922e53ab28a1554d6874f24b221.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-14.jpg',
     '1990-05-25', 'thuytrang90@gmail.com', 'FEMALE', 'NORMAL', 'USER', 'Thùy Trang'),

    ('system', false, now(), 'system', '{"city":"Hà Nội","street":"14 Láng Hạ"}', NULL,
     'https://cdn11.dienmaycholon.vn/filewebdmclnew/public/userupload/files/Image%20FP_2024/meme-meo-4.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-8.jpg',
     '1987-09-09', 'longnguyen87@gmail.com', 'MALE', 'GOOGLE', 'USER', 'Nguyễn Long'),

    ('system', false, now(), 'system', '{"city":"Hà Nam","street":"27 Trần Hưng Đạo"}', NULL,
     'https://freenice.net/wp-content/uploads/2021/08/anh-dai-dien-hai-huoc-cute.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-5.jpg',
     '1998-11-11', 'minhchau98@gmail.com', 'FEMALE', 'NORMAL', 'ADMIN', 'Minh Châu'),

    ('system', false, now(), 'system', '{"city":"Lâm Đồng","street":"19 Phan Đình Phùng"}', NULL,
     'https://tamkytourism.com/wp-content/uploads/2025/02/avatar-vo-tri-10.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-1-1.jpg',
     '1996-07-01', 'duchoang96@gmail.com', 'MALE', 'NORMAL', 'USER', 'Đức Hoàng'),

    ('system', false, now(), 'system', '{"city":"Quảng Nam","street":"22 Lý Tự Trọng"}', NULL,
     'https://tamkytourism.com/wp-content/uploads/2025/02/avatar-vo-tri-13.jpg',
     'https://cdn.chanhtuoi.com/uploads/2021/11/anh-bau-troi-46.jpg',
     '1993-02-20', 'thuytien93@gmail.com', 'FEMALE', 'GOOGLE', 'USER', 'Thùy Tiên');
