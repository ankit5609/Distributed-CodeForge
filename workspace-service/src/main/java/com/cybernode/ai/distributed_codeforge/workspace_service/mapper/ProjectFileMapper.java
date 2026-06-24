package com.cybernode.ai.distributed_codeforge.workspace_service.mapper;

import com.cybernode.ai.distributed_codeforge.common_lib.dto.FileNode;
import com.cybernode.ai.distributed_codeforge.workspace_service.entity.ProjectFile;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectFileMapper {

    List<FileNode> toListOfFileNode(List<ProjectFile> projectFileList);
}
