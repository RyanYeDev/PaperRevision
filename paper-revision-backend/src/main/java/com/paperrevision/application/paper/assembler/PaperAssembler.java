package com.paperrevision.application.paper.assembler;

import org.springframework.beans.BeanUtils;
import com.paperrevision.application.paper.dto.PaperDTO;
import com.paperrevision.domain.paper.model.PaperEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 论文装配器 */
public class PaperAssembler {

    public static PaperDTO toDTO(PaperEntity entity) {
        if (entity == null) return null;
        PaperDTO dto = new PaperDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public static List<PaperDTO> toDTOs(List<PaperEntity> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        return entities.stream().map(PaperAssembler::toDTO).collect(Collectors.toList());
    }
}
