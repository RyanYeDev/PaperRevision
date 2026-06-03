package org.xhy.application.user.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 用户装配器 */
public class UserAssembler {

    public static UserDTO toDTO(UserEntity entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public static List<UserDTO> toDTOs(List<UserEntity> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        return entities.stream().map(UserAssembler::toDTO).collect(Collectors.toList());
    }
}
