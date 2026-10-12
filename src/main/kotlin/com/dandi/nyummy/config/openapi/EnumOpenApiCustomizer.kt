package com.dandi.nyummy.config.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

/**
 * API마다 요청·응답에서 쓰는 enum을 찾아 설명 맨 아래에 목록으로 붙인다.
 *
 * enum 값은 Schema 탭에만 있고, 기본으로 보이는 Example Value에는 첫 값 하나만 나온다.
 * API를 펼치자마자 허용 값이 보이도록 필드 경로와 값을 설명에 적는다. enum을 추가하거나 값을 바꿔도 따로 손댈 곳이 없다.
 *
 * 스키마가 모두 만들어진 뒤에야 `$ref`를 따라갈 수 있으므로 operation 단위가 아닌 문서 전체 customizer로 둔다.
 */
@Component
class EnumOpenApiCustomizer : OpenApiCustomizer {

    private data class EnumField(val path: String, val schemaName: String, val schema: Schema<*>)

    override fun customise(openApi: OpenAPI) {
        val schemas = openApi.components?.schemas.orEmpty()

        openApi.paths.orEmpty().values.flatMap { it.readOperations() }.forEach { operation ->
            val requestFields = buildList {
                operation.parameters.orEmpty().forEach {
                    collectEnumFields(it.schema, it.name, schemas, emptySet(), this)
                }
                collectEnumFields(operation.requestBody?.content, schemas, this)
            }
            val responseFields = buildList {
                operation.responses.orEmpty().filterKeys { it.startsWith("2") }.values
                    .forEach { collectEnumFields(it.content, schemas, this) }
            }

            val enumDescription = listOfNotNull(
                createEnumDescription("요청 enum", requestFields),
                createEnumDescription("응답 enum", responseFields),
            )
            if (enumDescription.isNotEmpty()) {
                operation.description = (listOfNotNull(operation.description) + enumDescription).joinToString("\n\n")
            }
        }
    }

    private fun collectEnumFields(content: Content?, schemas: Map<String, Schema<*>>, result: MutableList<EnumField>) {
        content.orEmpty().values.firstOrNull()?.let { collectEnumFields(it.schema, "", schemas, emptySet(), result) }
    }

    /**
     * 스키마를 따라 내려가며 enum 필드를 모은다. 경로는 `meals[].status`처럼 표기한다.
     * [visited]는 자기 자신을 참조하는 스키마에서 무한히 내려가지 않게 막는다.
     */
    private fun collectEnumFields(
        schema: Schema<*>?,
        path: String,
        schemas: Map<String, Schema<*>>,
        visited: Set<String>,
        result: MutableList<EnumField>,
    ) {
        if (schema == null) return

        val schemaName = schema.`$ref`?.substringAfterLast("/")
        if (schemaName != null) {
            if (schemaName in visited) return
            val target = schemas[schemaName] ?: return
            if (target.enum.isNullOrEmpty()) {
                collectEnumFields(target, path, schemas, visited + schemaName, result)
            } else {
                result.add(EnumField(path, schemaName, target))
            }
            return
        }

        if (!schema.enum.isNullOrEmpty()) {
            result.add(EnumField(path, schema.type ?: "enum", schema))
            return
        }

        schema.properties.orEmpty().forEach { (name, property) ->
            collectEnumFields(
                property,
                listOf(path, name).filter {
                    it.isNotEmpty()
                }.joinToString("."),
                schemas,
                visited,
                result,
            )
        }
        collectEnumFields(schema.items, "$path[]", schemas, visited, result)
        (schema.allOf.orEmpty() + schema.oneOf.orEmpty() + schema.anyOf.orEmpty())
            .forEach { collectEnumFields(it, path, schemas, visited, result) }
    }

    private fun createEnumDescription(title: String, fields: List<EnumField>): String? {
        if (fields.isEmpty()) return null

        // 목록(- )으로 쓰면 Swagger UI 다크 모드에서 설명 영역의 li 글자색이 배경과 겹쳐 보이지 않아 문단으로 쓴다.
        val paragraphs = fields.distinctBy { it.path }.map { field ->
            val values = field.schema.enum.joinToString(" · ") { "`$it`" }
            "`${field.path.ifEmpty { "(본문)" }}` (${field.schemaName}): $values"
        }
        return (listOf("**$title**") + paragraphs).joinToString("\n\n")
    }
}
