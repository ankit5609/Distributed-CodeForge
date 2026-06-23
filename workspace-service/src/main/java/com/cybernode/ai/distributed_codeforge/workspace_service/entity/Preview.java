package com.cybernode.ai.distributed_codeforge.workspace_service.entity;//package com.example.cybernode.ai.CodeForge.entity;


import com.cybernode.ai.distributed_codeforge.common_lib.enums.PreviewStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

//@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Preview {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Project project;

    String namespace;

    String podName;

    String previewUrl;

    PreviewStatus status;

    Instant startedAt;

    Instant terminatedAt;

    Instant createdAt;
}
