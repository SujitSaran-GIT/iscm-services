package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth URL response")
public class OAuthUrlResponse {

    @Schema(description = "OAuth authorization URL", example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...")
    private String url;
}