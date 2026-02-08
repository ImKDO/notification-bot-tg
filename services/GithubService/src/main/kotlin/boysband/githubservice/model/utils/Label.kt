package boysband.githubservice.model.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Label(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("color")
    val color: String = "",
    @JsonProperty("description")
    val description: String? = null,
)
