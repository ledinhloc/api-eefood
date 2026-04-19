package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.request.CookingSessionProgressRequest;
import com.eefood.recipeservice.dto.response.CookingSessionProgressResponse;
import com.eefood.recipeservice.dto.response.CookingSessionResponse;
import com.eefood.recipeservice.dto.response.CookingSessionStepResponse;
import com.eefood.recipeservice.enums.CookingSessionStatus;
import com.eefood.recipeservice.enums.CookingStepStatus;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.CookingSessionMapper;
import com.eefood.recipeservice.model.CookingSessionStep;
import com.eefood.recipeservice.model.CookingSessions;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeStep;
import com.eefood.recipeservice.repository.CookingSessionStepRepository;
import com.eefood.recipeservice.repository.CookingSessionsRepository;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CookingSessionService {
    private final CookingSessionsRepository sessionRepository;
    private final CookingSessionStepRepository sessionStepRepository;
    private final RecipeRepository recipeRepository;
    private final SecurityUtil securityUtil;
    private final CookingSessionMapper cookingSessionMapper;

    public CookingSessionResponse getOrCreateCookingSession(Long recipeId) {
        Long userId = securityUtil.getCurrentUserId();
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(()-> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        List<RecipeStep> recipeSteps = recipe.getSteps().stream().toList();
        if(recipeSteps.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_HAS_NO_STEPS);
        }

        CookingSessions session = sessionRepository.findByUserIdAndRecipeIdAndStatus(userId, recipeId, CookingSessionStatus.IN_PROGRESS)
                .orElseGet(()-> createNewSession(userId, recipe, recipeSteps));

        List<CookingSessionStep> sessionSteps = sessionStepRepository
                .findByCookingSessionIdOrderByRecipeStepStepNumberAsc(session.getId());

        List<CookingSessionStepResponse> stepResponses = cookingSessionMapper.toStepResponseList(sessionSteps);

        CookingSessionResponse response = cookingSessionMapper.toResponse(session);
        response.setTotalSteps(recipeSteps.size());
        response.setSteps(stepResponses);
        return response;
    }

    private CookingSessions createNewSession(Long userId, Recipe recipe, List<RecipeStep> recipeSteps) {
        LocalDateTime currentTime = LocalDateTime.now();
        CookingSessions newSession = CookingSessions.builder()
                .userId(userId)
                .recipe(recipe)
                .status(CookingSessionStatus.IN_PROGRESS)
                .currentStep(1)
                .startedAt(currentTime)
                .completedAt(currentTime.plusMinutes(recipe.getCookTime()))
                .build();

        CookingSessions savedSession = sessionRepository.save(newSession);

        List<CookingSessionStep> sessionSteps = new ArrayList<>();

        for (RecipeStep recipeStep : recipeSteps) {

            LocalDateTime stepStart = currentTime;
            LocalDateTime stepEnd = currentTime.plusMinutes(recipeStep.getStepTime()!=null ? recipeStep.getStepTime() : 0);

            boolean isFirstStep = recipeStep.getStepNumber() == 1;

            CookingSessionStep step = CookingSessionStep.builder()
                    .cookingSession(savedSession)
                    .recipeStep(recipeStep)
                    .status(isFirstStep
                            ? CookingStepStatus.IN_PROGRESS
                            : CookingStepStatus.PENDING)
                    .startedAt(isFirstStep ? stepStart : null)
                    .completedAt(stepEnd)
                    .build();

            sessionSteps.add(step);

            currentTime = stepEnd;
        }

        sessionStepRepository.saveAll(sessionSteps);

        savedSession.setCompletedAt(currentTime);
        sessionRepository.save(savedSession);

        return savedSession;
    }

    public CookingSessionProgressResponse savePartProgress(Long sessionId, CookingSessionProgressRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        CookingSessions session = getValidatedSession(userId, sessionId);

        List<CookingSessionStep> sessionSteps = sessionStepRepository
                .findByCookingSessionIdOrderByRecipeStepStepNumberAsc(sessionId);

        int targetStep = request.getCurrentStep();
        int totalSteps = sessionSteps.size();

        // Validate currentStep không vượt quá tổng số bước
        if (targetStep > totalSteps) {
            targetStep = totalSteps;
        }

        // Cập nhật trạng thái từng step
        for (CookingSessionStep sessionStep : sessionSteps) {
            int stepNumber = sessionStep.getRecipeStep().getStepNumber();
            if (stepNumber < targetStep) {
                sessionStep.setStatus(CookingStepStatus.DONE);
                if (sessionStep.getCompletedAt() == null) {
                    sessionStep.setCompletedAt(LocalDateTime.now());
                }
            } else if (stepNumber == targetStep) {
                sessionStep.setStatus(CookingStepStatus.IN_PROGRESS);
                if (sessionStep.getStartedAt() == null) {
                    sessionStep.setStartedAt(LocalDateTime.now());
                }
            } else {
                sessionStep.setStatus(CookingStepStatus.PENDING);
            }
        }

        sessionStepRepository.saveAll(sessionSteps);

        // Cập nhật session
        session.setCurrentStep(targetStep);
        sessionRepository.save(session);

        CookingSessionProgressResponse response = cookingSessionMapper.toProgressResponse(session);
        response.setTotalSteps(totalSteps);
        return response;
    }

    public CookingSessionProgressResponse completeSession(Long sessionId) {
        Long userId = securityUtil.getCurrentUserId();
        CookingSessions session = getValidatedSession(userId, sessionId);

        List<CookingSessionStep> sessionSteps = sessionStepRepository
                .findByCookingSessionIdOrderByRecipeStepStepNumberAsc(sessionId);

        // Đánh dấu tất cả step DONE
        LocalDateTime now = LocalDateTime.now();
        sessionSteps.forEach(step -> {
            step.setStatus(CookingStepStatus.DONE);
            if (step.getStartedAt() == null)  step.setStartedAt(now);
            if (step.getCompletedAt() == null) step.setCompletedAt(now);
        });
        sessionStepRepository.saveAll(sessionSteps);

        // Cập nhật session
        int totalSteps = sessionSteps.size();
        session.setStatus(CookingSessionStatus.COMPLETED);
        session.setCurrentStep(totalSteps);
        session.setCompletedAt(now);
        sessionRepository.save(session);

        CookingSessionProgressResponse response = cookingSessionMapper.toProgressResponse(session);
        response.setTotalSteps(totalSteps);
        return response;
    }

    private CookingSessions getValidatedSession(Long userId, Long sessionId) {
        CookingSessions session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.COOKING_SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw ExceptionUtil.forbidden(ErrorMessage.COOKING_SESSION_FORBIDDEN);
        }

        if (session.getStatus() == CookingSessionStatus.COMPLETED) {
            throw ExceptionUtil.badRequest(ErrorMessage.COOKING_SESSION_ALREADY_COMPLETED);
        }

        return session;
    }
}
