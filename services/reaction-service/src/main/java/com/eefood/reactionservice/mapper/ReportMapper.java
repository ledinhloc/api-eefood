package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportPost;
import com.eefood.reactionservice.model.report.ReportStory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {
    @Mapping(target = "targetId", expression = "java((entity.getPost() != null) ? entity.getPost().getId() : null)")
    ReportResponse toResponse(ReportPost entity);

    @Mapping(target = "targetId", expression = "java((entity.getComment() != null) ? entity.getComment().getId() : null)")
    ReportResponse toResponse(ReportComment entity);

    @Mapping(target = "targetId", expression = "java((entity.getStory() != null) ? entity.getStory().getId() : null)")
    ReportResponse toResponse(ReportStory entity);
}