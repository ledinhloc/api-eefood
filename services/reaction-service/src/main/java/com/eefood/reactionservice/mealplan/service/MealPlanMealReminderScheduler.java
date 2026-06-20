package com.eefood.reactionservice.mealplan.service;

import com.eefood.common.avro.NotificationEvent;
import com.eefood.reactionservice.kafka.NotificationProducer;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanMealReminderScheduler {
    private static final String TIME_ZONE = "Asia/Bangkok";
    private static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE);
    private static final String NOTIFICATION_TYPE = "MEAL_PLAN";

    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final NotificationProducer notificationProducer;
    //second minute hour day month weekday
    @Scheduled(cron = "0 0 6 * * *", zone = TIME_ZONE)
    @Transactional(readOnly = true)
    public void remindBreakfast() {
        sendMealReminder(MealSlot.BREAKFAST);
    }

    @Scheduled(cron = "0 30 10 * * *", zone = TIME_ZONE)
    @Transactional(readOnly = true)
    public void remindLunch() {
        sendMealReminder(MealSlot.LUNCH);
    }

    @Scheduled(cron = "0 0 17 * * *", zone = TIME_ZONE)
    @Transactional(readOnly = true)
    public void remindDinner() {
        sendMealReminder(MealSlot.DINNER);
    }

    private void sendMealReminder(MealSlot mealSlot) {
        LocalDate today = LocalDate.now(ZONE_ID);
        // tìm các món
        List<MealPlanItem> items = mealPlanItemRepository
                .findAllByPlanDateAndMealSlotAndStatus(today, mealSlot, MealPlanItemStatus.PLANNED);

        log.info("Meal reminder job: date={}, slot={}, plannedItems={}", today, mealSlot, items.size());

        if (items.isEmpty()) {
            return;
        }

        Map<Long, MealPlan> mealPlansById = mealPlanRepository.findAllById(
                        items.stream()
                                .map(MealPlanItem::getMealPlanId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(MealPlan::getId, Function.identity()));

        items.stream()
                .filter(item -> mealPlansById.containsKey(item.getMealPlanId()))
                .collect(Collectors.groupingBy(MealPlanItem::getMealPlanId))
                .forEach((mealPlanId, mealItems) -> {
                    MealPlan mealPlan = mealPlansById.get(mealPlanId);
                    String dishNames = mealItems.stream()
                            .sorted(Comparator
                                    .comparing(MealPlanItem::getItemOrder, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(MealPlanItem::getId, Comparator.nullsLast(Long::compareTo)))
                            .map(this::getDishName)
                            .filter(name -> !name.isBlank())
                            .distinct()
                            .collect(Collectors.joining(", "));

                    if (dishNames.isBlank()) {
                        return;
                    }

                    NotificationEvent notification = NotificationEvent.newBuilder()
                            .setTitle(getTitle(mealSlot))
                            .setBody(dishNames)
                            .setPath("/meal-plan?date=" + today)
                            .setAvatarUrl(null)
                            .setPostImageUrl(null)
                            .setType(NOTIFICATION_TYPE)
                            .setUserId(mealPlan.getUserId())
                            .build();

                    notificationProducer.sendNotification(notification);
                    log.info("Sent meal reminder: userId={}, date={}, slot={}, dishes={}",
                            mealPlan.getUserId(),
                            today,
                            mealSlot,
                            dishNames);
                });
    }

    private String getDishName(MealPlanItem item) {
        if (item.getRecipeTitle() != null && !item.getRecipeTitle().isBlank()) {
            return item.getRecipeTitle().trim();
        }
        if (item.getCustomMealName() != null && !item.getCustomMealName().isBlank()) {
            return item.getCustomMealName().trim();
        }
        return "";
    }

    private String getTitle(MealSlot mealSlot) {
        return switch (mealSlot) {
            case BREAKFAST -> "Đến giờ ăn sáng";
            case LUNCH -> "Đến giờ ăn trưa";
            case DINNER -> "Đến giờ ăn tối";
            case SNACK -> "Đến giờ ăn nhé";
        };
    }
}
