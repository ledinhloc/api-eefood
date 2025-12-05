package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportPost;
import com.eefood.reactionservice.model.report.ReportStory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {
    @Mapping(target = "targetId", expression = "java(entity instanceof ReportPost rp ? rp.getPost().getId() : null)")
    ReportResponse toResponse(ReportPost entity);

    @Mapping(target = "targetId", expression = "java(entity instanceof ReportComment rc ? rc.getComment().getId() : null)")
    ReportResponse toResponse(ReportComment entity);

    @Mapping(target = "targetId", expression = "java(entity instanceof ReportStory rs ? rs.getStory().getId() : null)")
    ReportResponse toResponse(ReportStory entity);
}
