package com.cybernode.ai.distributed_codeforge.common_lib.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static com.cybernode.ai.distributed_codeforge.common_lib.enums.ProjectPermission.*;

@RequiredArgsConstructor
@Getter
public enum ProjectRole {
    EDITOR(Set.of(EDIT, VIEW, DELETE)),
    VIEWER(Set.of(VIEW,VIEW_MEMBERS)),
    OWNER(Set.of(VIEW,EDIT,DELETE,MANAGE_MEMBERS, VIEW_MEMBERS));

    private final Set<ProjectPermission> permissions;
}
